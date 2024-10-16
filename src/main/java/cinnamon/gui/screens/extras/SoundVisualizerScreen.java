package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Slider;
import cinnamon.model.GeometryHelper;
import cinnamon.parsers.LrcLoader;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.sound.Sound;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundInstance;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import org.jtransforms.fft.DoubleFFT_1D;

import java.nio.ShortBuffer;
import java.nio.file.Path;

public class SoundVisualizerScreen extends ParentedScreen {

    private static final int FFT_SIZE = 1024;
    private static final DoubleFFT_1D FFT = new DoubleFFT_1D(FFT_SIZE);

    private static final Resource
            PLAY = new Resource("textures/gui/icons/play.png"),
            PAUSE = new Resource("textures/gui/icons/pause.png"),
            STOP = new Resource("textures/gui/icons/stop.png"),
            REPEAT = new Resource("textures/gui/icons/repeat.png"),
            REPEAT_OFF = new Resource("textures/gui/icons/repeat_off.png");

    private Resource resource;
    private LrcLoader.Lyrics lyrics;
    private String title = "";

    private Sound sound;
    private SoundInstance soundData;
    private ShortBuffer pcmBuffer;
    private int frameLength;
    private double[][] doubleBuffer;

    private Slider slider;
    private Button playPauseButton;
    private boolean repeat;

    public SoundVisualizerScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void removed() {
        super.removed();
        if (soundData != null && !soundData.isRemoved())
            soundData.stop();
    }

    @Override
    public void init() {
        ContainerGrid grid = new ContainerGrid(0, 0, 4, 3);

        playPauseButton = new Button(0, 0, 16, 16, null, button -> {
            if (soundData != null && !soundData.isRemoved()) {
                if (soundData.isPlaying()) {
                    soundData.pause();
                    button.setImage(PLAY);
                } else {
                    soundData.play();
                    button.setImage(PAUSE);
                }
            } else if (resource != null) {
                playSound();
            }
        });
        playPauseButton.setImage(soundData != null && !soundData.isRemoved() && soundData.isPlaying() ? PAUSE : PLAY);
        playPauseButton.setSilent(true);
        grid.addWidget(playPauseButton);

        Button stopButton = new Button(0, 0, 16, 16, null, button -> {
            if (soundData != null && !soundData.isRemoved()) {
                soundData.stop();
                playPauseButton.setImage(PLAY);
                slider.updateValue(0);
            }
        });
        stopButton.setImage(STOP);
        stopButton.setSilent(true);
        grid.addWidget(stopButton);

        Button repeatButton = new Button(0, 0, 16, 16, null, button -> {
            repeat = !repeat;
            if (soundData != null && !soundData.isRemoved())
                soundData.loop(repeat);
            button.setImage(repeat ? REPEAT : REPEAT_OFF);
        });
        repeatButton.setImage(repeat ? REPEAT : REPEAT_OFF);
        repeatButton.setSilent(true);
        grid.addWidget(repeatButton);

        grid.setAlignment(Alignment.CENTER);
        grid.setPos(width / 2, height - 8 - 4 - 16);
        addWidget(grid);

        slider = new Slider((width - 240) / 2, height - 8 - 4, 240);
        slider.setMax(sound != null ? sound.duration : 1);
        slider.setChangeListener((f, i) -> {
            if (soundData != null && !soundData.isRemoved())
                soundData.setPlaybackTime(i);
        });
        slider.setTooltipFunction((f, i) -> Text.of("%d:%02d".formatted(i / 1000 / 60, (i / 1000) % 60)));
        addWidget(slider);

        Slider volume = new Slider(4, height - 8 - 4, 50);
        volume.updatePercentage(SoundCategory.MUSIC.getVolume());
        volume.setUpdateListener((f, i) -> SoundCategory.MUSIC.setVolume(client.soundManager, f));
        volume.setTooltipFunction((f, i) -> Text.of("Volume: " + i));
        addWidget(volume);

        super.init();
    }

    private void playSound() {
        if (soundData != null && !soundData.isRemoved())
            soundData.stop();

        sound = Sound.of(resource);
        soundData = client.soundManager.playSound(resource, SoundCategory.MUSIC);

        pcmBuffer = sound.getBuffer();
        frameLength = FFT_SIZE * sound.channels;
        doubleBuffer = new double[sound.channels][FFT_SIZE];

        slider.setMax(sound.duration);
        playPauseButton.setImage(PAUSE);
        soundData.loop(repeat);
    }

    private double[][] getCurrentData() {
        if (soundData == null || soundData.isRemoved())
            return null;

        //calculate offset based on time and sample rate
        int offset = (int) (soundData.getPlaybackTime() * sound.sampleRate / 1000 * sound.channels);

        //return null if out of bounds
        if (offset + frameLength >= pcmBuffer.capacity())
            return null;

        //get the current audio data from the buffer
        for (int i = 0; i < sound.channels; i++) {
            pcmBuffer.position(offset + FFT_SIZE * i);
            for (int j = 0; j < FFT_SIZE; j++)
                doubleBuffer[i][j] = pcmBuffer.get();
        }
        return doubleBuffer;
    }

    @Override
    public void tick() {
        if (soundData != null) {
            if (!soundData.isRemoved()) {
                if (!slider.isDragged())
                    slider.updateValue((int) soundData.getPlaybackTime());
            } else {
                soundData = null;
                playPauseButton.setImage(PLAY);
            }
        }
        super.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render audio spectrum
        double[][] audioData = getCurrentData();
        if (audioData != null)
            drawSpectrum(matrices, audioData);

        //draw top text
        font.render(VertexConsumer.FONT, matrices, (int) (width / 2f), 4, Text.of("Drop an Ogg Vorbis file to play!").withStyle(Style.EMPTY.color(Colors.LIGHT_GRAY)), Alignment.CENTER);
        if (resource != null)
            font.render(VertexConsumer.FONT, matrices, 4, 4, Text.of("Current Playing:\n" + resource.getPath()));

        //draw timers
        int x = slider.getX();
        int y = (int) (slider.getCenterY() - font.lineHeight / 2);
        int now = slider.getValue() / 1000;
        int max = slider.getMax() / 1000;
        font.render(VertexConsumer.FONT, matrices, x - 4, y, Text.of("%d:%02d".formatted(now / 60, now % 60)), Alignment.RIGHT);
        font.render(VertexConsumer.FONT, matrices, x + slider.getWidth() + 4, y, Text.of("%d:%02d".formatted(max / 60, max % 60)));

        if (!title.isBlank())
            font.render(VertexConsumer.FONT, matrices, (int) (width / 2f), playPauseButton.getY() - font.lineHeight - 4, Text.of(title), Alignment.CENTER);

        if (lyrics != null && soundData != null && !soundData.isRemoved()) {
            int time = (int) soundData.getPlaybackTime();
            String text = lyrics.getLyric(time);
            if (!text.isBlank())
                font.render(VertexConsumer.FONT, matrices, (int) (width / 2f), (int) (height / 4f) - font.lineHeight / 2, Text.of(text), Alignment.CENTER);
        }

        //render widgets
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawSpectrum(MatrixStack matrices, double[][] audioData) {
        //apply FFT
        FFT.realForward(audioData[0]);

        //draw audio data
        for (int i = 0; i < audioData[0].length; i++) {
            float amplitude = (float) Math.abs(audioData[0][i]) / 10000f;
            drawBar(matrices, i, amplitude);
        }
    }

    private void drawBar(MatrixStack matrices, int i, float amplitude) {
        int w = 1;
        if (i * w > width)
            return;

        VertexConsumer.GUI.consume(GeometryHelper.rectangle(matrices, i * w, (height - amplitude) / 2, i * w + w, (height + amplitude) / 2, 0xFF72FFAD));
    }

    @Override
    public boolean filesDropped(String[] files) {
        if (files.length > 0 && files[0].toLowerCase().endsWith(".ogg")) {
            this.resource = new Resource("", files[0]);

            Path path = Path.of(files[0]);
            String filename = path.getFileName().toString();
            filename = filename.substring(0, filename.length() - 4);

            String title = "";

            try {
                this.lyrics = LrcLoader.loadLyrics(new Resource("", path.getParent().resolve(filename + ".lrc").toString()));
                title = lyrics.title + " - " + lyrics.artist;
            } catch (Exception ignored) {
                this.lyrics = null;
            }

            this.title = title.isBlank() ? filename : title;

            playSound();
            return true;
        }

        return super.filesDropped(files);
    }
}
