package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
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
import java.util.ArrayList;
import java.util.List;

public class SoundVisualizerScreen extends ParentedScreen {

    private static final int FFT_SIZE = 1024;
    private static final DoubleFFT_1D FFT = new DoubleFFT_1D(FFT_SIZE);

    private static final Resource
            PLAY = new Resource("textures/gui/icons/play.png"),
            PAUSE = new Resource("textures/gui/icons/pause.png"),
            STOP = new Resource("textures/gui/icons/stop.png"),
            REPEAT = new Resource("textures/gui/icons/repeat.png"),
            REPEAT_ONE = new Resource("textures/gui/icons/repeat_one.png"),
            REPEAT_OFF = new Resource("textures/gui/icons/repeat_off.png"),
            PREVIOUS = new Resource("textures/gui/icons/previous.png"),
            NEXT = new Resource("textures/gui/icons/next.png");

    private final List<Track> playlist = new ArrayList<>();
    private int playlistIndex = -1;

    private Sound sound;
    private SoundInstance soundData;
    private ShortBuffer pcmBuffer;
    private int frameLength;
    private double[][] doubleBuffer;

    private Slider slider;
    private Button playPauseButton, nextButton, previousButton;
    private int repeat = 0; //0 = off, 1 = one, 2 = all

    public SoundVisualizerScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void removed() {
        super.removed();
        if (hasSound())
            soundData.stop();
    }

    @Override
    public void init() {
        ContainerGrid buttons = new ContainerGrid(0, 0, 16, 3);
        buttons.setAlignment(Alignment.CENTER);

        previousButton = new Button(0, 0, 16, 16, null, button -> {
            if (playlistIndex >= 0)
                playSound((playlistIndex - 1 + playlist.size()) % playlist.size());
        });
        previousButton.setImage(PREVIOUS);
        previousButton.setTooltip(Text.of("Previous Song"));
        previousButton.setSilent(true);
        previousButton.setActive(playlist.size() > 1);
        buttons.addWidget(previousButton);

        ContainerGrid centerButtons = new ContainerGrid(0, 0, 4, 3);
        centerButtons.setAlignment(Alignment.CENTER);

        playPauseButton = new Button(0, 0, 16, 16, null, button -> {
            if (hasSound()) {
                if (soundData.isPlaying()) {
                    soundData.pause();
                    button.setImage(PLAY);
                    button.setTooltip(Text.of("Play"));
                } else {
                    soundData.play();
                    button.setImage(PAUSE);
                    button.setTooltip(Text.of("Pause"));
                }
            } else {
                playSound(playlistIndex);
            }
        });
        boolean pause = hasSound() && soundData.isPlaying();
        playPauseButton.setImage(pause ? PAUSE : PLAY);
        playPauseButton.setTooltip(Text.of(pause ? "Pause" : "Play"));
        playPauseButton.setSilent(true);
        centerButtons.addWidget(playPauseButton);

        Button stopButton = new Button(0, 0, 16, 16, null, button -> {
            if (hasSound()) {
                soundData.pause();
                playPauseButton.setImage(PLAY);
                slider.updateValue(0);
            }
        });
        stopButton.setImage(STOP);
        stopButton.setTooltip(Text.of("Stop"));
        stopButton.setSilent(true);
        centerButtons.addWidget(stopButton);

        Button repeatButton = new Button(0, 0, 16, 16, null, button -> {
            repeat = (repeat + 1) % 3;
            if (hasSound())
                soundData.loop(repeat == 1);
            switch (repeat) {
                case 1 -> {
                    button.setImage(REPEAT_ONE);
                    button.setTooltip(Text.of("Repeat One"));
                }
                case 2 -> {
                    button.setImage(REPEAT);
                    button.setTooltip(Text.of("Repeat All"));
                }
                default -> {
                    button.setImage(REPEAT_OFF);
                    button.setTooltip(Text.of("Repeat Off"));
                }
            }
        });
        repeatButton.setSilent(true);
        repeat--;
        repeatButton.onRun();
        centerButtons.addWidget(repeatButton);

        buttons.addWidget(centerButtons);

        nextButton = new Button(0, 0, 16, 16, null, button -> {
            if (playlistIndex >= 0)
                playSound((playlistIndex + 1) % playlist.size());
        });
        nextButton.setImage(NEXT);
        nextButton.setTooltip(Text.of("Next Song"));
        nextButton.setSilent(true);
        nextButton.setActive(playlist.size() > 1);
        buttons.addWidget(nextButton);

        buttons.setPos(width / 2, height - 8 - 4 - 16);
        addWidget(buttons);

        slider = new Slider((width - 240) / 2, height - 8 - 4, 240);
        slider.setMax(sound != null ? sound.duration : 1);
        slider.setChangeListener((f, i) -> {
            if (hasSound())
                soundData.setPlaybackTime(i);
        });
        slider.setUpdateListener((f, i) -> {
            if (hasSound() && !soundData.isPlaying())
                soundData.setPlaybackTime(i);
        });
        slider.setTooltipFunction((f, i) -> Text.of("%d:%02d".formatted(i / 1000 / 60, (i / 1000) % 60)));
        slider.setValue(hasSound() ? (int) soundData.getPlaybackTime() : 0);
        addWidget(slider);

        Slider volume = new Slider(4, height - 8 - 4, 50);
        volume.updatePercentage(SoundCategory.MUSIC.getVolume());
        volume.setUpdateListener((f, i) -> SoundCategory.MUSIC.setVolume(client.soundManager, f));
        volume.setTooltipFunction((f, i) -> Text.of("Volume: " + i));
        addWidget(volume);

        super.init();
    }

    private boolean hasSound() {
        return soundData != null && !soundData.isRemoved();
    }

    private void playSound(int index) {
        if (hasSound())
            soundData.stop();

        if (index < 0 || index >= playlist.size())
            return;

        playlistIndex = index;
        Track track = playlist.get(index);
        Resource resource = track.resource;

        sound = Sound.of(resource);
        soundData = client.soundManager.playSound(resource, SoundCategory.MUSIC);

        pcmBuffer = sound.getBuffer();
        frameLength = FFT_SIZE * sound.channels;
        doubleBuffer = new double[sound.channels][FFT_SIZE];

        slider.setMax(sound.duration);
        playPauseButton.setImage(PAUSE);
        soundData.loop(repeat == 1);

        Toast.addToast(Text.of("Now playing: %s".formatted(track.title)), font);
    }

    private double[][] getCurrentData() {
        if (!hasSound())
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
                if (!slider.isDragged() && soundData.isPlaying())
                    slider.updateValue((int) soundData.getPlaybackTime());
            } else if (playlistIndex >= 0 && repeat != 1) {
                if (repeat == 2 || playlistIndex < playlist.size() - 1)
                    playSound((playlistIndex + 1) % playlist.size());
                else {
                    soundData = null;
                    playlistIndex = 0;
                    playPauseButton.setImage(PLAY);
                    slider.updatePercentage(1f);
                }
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

        //draw texts
        drawTexts(matrices);

        //render widgets
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawTexts(MatrixStack matrices) {
        int songCount = playlist.size();
        int playTime = hasSound() ? (int) soundData.getPlaybackTime() : slider.getValue();

        //draw top text
        font.render(VertexConsumer.FONT, matrices, (int) (width / 2f), 4, Text.of("Drop Ogg Vorbis files to play!").withStyle(Style.EMPTY.color(songCount > 0 ? Colors.LIGHT_GRAY : Colors.WHITE)), Alignment.CENTER);

        //draw timers
        int x = slider.getX();
        int y = (int) (slider.getCenterY() - font.lineHeight / 2);
        int now = playTime / 1000;
        int max = slider.getMax() / 1000;
        font.render(VertexConsumer.FONT, matrices, x - 4, y, Text.of("%d:%02d".formatted(now / 60, now % 60)), Alignment.RIGHT);
        font.render(VertexConsumer.FONT, matrices, x + slider.getWidth() + 4, y, Text.of("%d:%02d".formatted(max / 60, max % 60)));

        if (songCount == 0)
            return;

        //song count
        if (songCount > 1)
            font.render(VertexConsumer.FONT, matrices, (int) (width / 2f), playPauseButton.getY() - font.lineHeight - 4, Text.of("%d / %d".formatted(playlistIndex + 1, songCount)), Alignment.CENTER);

        Track track = playlist.get(playlistIndex);

        //title
        font.render(VertexConsumer.FONT, matrices, (int) (width / 2f), playPauseButton.getY() - (font.lineHeight + 4) * (songCount > 1 ? 2 : 1), Text.of(track.title), Alignment.CENTER);

        //lyrics
        String text = track.getLyrics(playTime);
        if (!text.isBlank())
            font.render(VertexConsumer.FONT, matrices, (int) (width / 2f), (int) (height / 4f) - font.lineHeight / 2, Text.of(text), Alignment.CENTER);
    }

    private void drawSpectrum(MatrixStack matrices, double[][] audioData) {
        // Apply FFT to the left audio channel (audioData[0])
        FFT.realForward(audioData[0]);

        // Number of bars (can vary based on the visualization width)
        int numBars = width;

        // Draw audio data with log scale
        for (int i = 1; i < numBars; i++) {
            // Logarithmic index mapping
            int logI = (int) (Math.pow(FFT_SIZE, (double) i / numBars) - 1);

            // Ensure logI stays within valid bounds
            if (logI >= FFT_SIZE) {
                continue;
            }

            // Calculate amplitude from log-scaled frequency bin
            float amplitude = (float) Math.abs(audioData[0][logI]) / 10000f;

            // Draw the bar on the spectrum visualizer
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
        List<Track> newPlaylist = new ArrayList<>();

        for (String file : files) {
            if (!file.toLowerCase().endsWith(".ogg"))
                continue;

            Resource song = new Resource("", file);

            Path path = Path.of(file);
            String filename = path.getFileName().toString();
            filename = filename.substring(0, filename.length() - 4);

            LrcLoader.Lyrics lyrics;

            try {
                lyrics = LrcLoader.loadLyrics(new Resource("", path.getParent().resolve(filename + ".lrc").toString()));
            } catch (Exception ignored) {
                lyrics = null;
            }

            String title = "";
            if (lyrics != null) {
                if (!lyrics.title.isBlank()) {
                    title = lyrics.title;
                    if (!lyrics.artist.isBlank())
                        title += " - " + lyrics.artist;
                }
            }

            if (title.isBlank())
                title = filename;

            newPlaylist.add(new Track(song, lyrics, title));
        }

        newPlaylist.sort((a, b) -> a.title.compareToIgnoreCase(b.title));

        int size = newPlaylist.size();
        if (size > 0) {
            playlist.clear();
            playlistIndex = 0;
            playlist.addAll(newPlaylist);
            nextButton.setActive(size > 1);
            previousButton.setActive(size > 1);
            Toast.addToast(Text.of("Loaded %d song%s!".formatted(size, size > 1 ? "s" : "")), font);
            playSound(0);
            return true;
        }

        return super.filesDropped(files);
    }

    private record Track(Resource resource, LrcLoader.Lyrics lyrics, String title) {
        public String getLyrics(int time) {
            return lyrics != null ? lyrics.getLyric(time) : "";
        }
    }
}
