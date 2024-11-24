package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.ComboBox;
import cinnamon.gui.widgets.types.Slider;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.parsers.LrcLoader;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.settings.Settings;
import cinnamon.sound.*;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SoundVisualizerScreen extends ParentedScreen {

    private static final float
            BOOST = 3f;
    private static final Function<Float, Float>
            WEIGHTING_FUNCTION = f -> f < 1000 ? 1f : f < 4000 ? 2f : f < 12000 ? 4f : 8f;

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
    private final SoundSpectrum spectrum = new SoundSpectrum();

    private Slider slider;
    private Button playPauseButton, nextButton, previousButton;
    private int repeat = 0; //0 = off, 1 = one, 2 = all

    private final float initialVolume = SoundCategory.MUSIC.getVolume();

    public SoundVisualizerScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void removed() {
        super.removed();
        //stop the sound when the screen is closed
        if (soundData != null)
            soundData.stop();
        //save settings if we changed the volume
        if (initialVolume != SoundCategory.MUSIC.getVolume())
            Settings.save();
    }

    @Override
    public void init() {
        //widgets init

        //buttons grid
        ContainerGrid buttons = new ContainerGrid(0, 0, 16, 3);
        buttons.setAlignment(Alignment.CENTER);

        //previous track button
        previousButton = new Button(0, 0, 16, 16, null, button -> {
            if (playlistIndex >= 0)
                playSound((playlistIndex - 1 + playlist.size()) % playlist.size());
        });
        previousButton.setImage(PREVIOUS);
        previousButton.setTooltip(Text.of("Previous Song"));
        previousButton.setSilent(true);
        previousButton.setActive(playlist.size() > 1);
        buttons.addWidget(previousButton);

        //center buttons grid
        ContainerGrid centerButtons = new ContainerGrid(0, 0, 4, 3);
        centerButtons.setAlignment(Alignment.CENTER);

        //play/pause button
        playPauseButton = new Button(0, 0, 16, 16, null, button -> {
            if (soundData != null) {
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
        boolean pause = soundData != null && soundData.isPlaying();
        playPauseButton.setImage(pause ? PAUSE : PLAY);
        playPauseButton.setTooltip(Text.of(pause ? "Pause" : "Play"));
        playPauseButton.setSilent(true);
        centerButtons.addWidget(playPauseButton);

        //stop button
        Button stopButton = new Button(0, 0, 16, 16, null, button -> {
            if (soundData != null) {
                soundData.pause();
                playPauseButton.setImage(PLAY);
                slider.updateValue(0);
            }
        });
        stopButton.setImage(STOP);
        stopButton.setTooltip(Text.of("Stop"));
        stopButton.setSilent(true);
        centerButtons.addWidget(stopButton);

        //repeat-mode button
        Button repeatButton = new Button(0, 0, 16, 16, null, button -> {
            repeat = (repeat + 1) % 3;
            if (soundData != null)
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

        //add center buttons to the main grid
        buttons.addWidget(centerButtons);

        //next track button
        nextButton = new Button(0, 0, 16, 16, null, button -> {
            if (playlistIndex >= 0)
                playSound((playlistIndex + 1) % playlist.size());
        });
        nextButton.setImage(NEXT);
        nextButton.setTooltip(Text.of("Next Song"));
        nextButton.setSilent(true);
        nextButton.setActive(playlist.size() > 1);
        buttons.addWidget(nextButton);

        //update buttons grid to the center of screen at the bottom - and add it to the screen
        buttons.setPos(width / 2, height - 8 - 4 - 16);
        addWidget(buttons);

        //slider for the playback control
        slider = new Slider((width - 240) / 2, height - 8 - 4, 240);
        slider.setMax(sound != null ? sound.duration : 1);
        slider.setChangeListener((f, i) -> {
            if (soundData != null)
                soundData.setPlaybackTime(Math.max(i - 1, 0));
        });
        slider.setUpdateListener((f, i) -> {
            if (soundData != null && !soundData.isPlaying())
                soundData.setPlaybackTime(Math.max(i - 1, 0));
        });
        slider.setTooltipFunction((f, i) -> Text.of("%d:%02d".formatted(i / 1000 / 60, (i / 1000) % 60)));
        slider.setValue(soundData == null ? 0 : (int) soundData.getPlaybackTime());
        slider.setPreviewHoverValueTooltip(true);
        addWidget(slider);

        //volume slider
        Slider volume = new Slider(4, height - 8 - 4, 50);
        volume.updatePercentage(SoundCategory.MUSIC.getVolume());
        volume.setUpdateListener((f, i) -> SoundCategory.MUSIC.setVolume(f));
        volume.setTooltipFunction((f, i) -> Text.of("Volume: " + i));
        addWidget(volume);

        //output device
        ComboBox device = new ComboBox(4, volume.getY() - 4 - 16, 50, 16);
        for (String string : SoundManager.getDevices())
            device.addEntry(Text.of(string));
        device.setChangeListener(i -> {
            boolean playing = soundData != null && soundData.isPlaying();
            float f = slider.getPercentage();

            SoundManager.swapDevice(i);
            playSound(playlistIndex);

            slider.setPercentage(f);
            if (!playing) playPauseButton.onRun();
        });
        addWidget(device);

        super.init();
    }

    @Override
    public void tick() {
        if (soundData != null) {
            if (!soundData.isRemoved()) {
                //update slider based on the sound time, but only if not dragged by the user
                if (!slider.isDragged() && soundData.isPlaying())
                    slider.updateValue((int) soundData.getPlaybackTime());
            } else if (playlistIndex >= 0 && repeat != 1) {
                //no sound here, so play the next song
                if (repeat == 2 || playlistIndex < playlist.size() - 1)
                    playSound((playlistIndex + 1) % playlist.size());
                //or stop completely based on the repeat flag
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


    // -- render functions -- //


    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //grab the audio spectrum and calculate the amplitudes
        spectrum.updateAmplitudes(sound, soundData, true);
        float[] amplitudes = spectrum.getAmplitudes();

        //draw bars
        int bars = amplitudes.length;
        for (int i = 0; i < bars; i++)
            drawBar(matrices, i, bars, amplitudes[i] * BOOST * WEIGHTING_FUNCTION.apply((float) i / bars * spectrum.getMaxFrequency()));

        //draw texts
        drawTexts(matrices);

        //render widgets
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawTexts(MatrixStack matrices) {
        int songCount = playlist.size();
        int playTime = soundData != null ? (int) soundData.getPlaybackTime() : slider.getValue();

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
        Text text = track.getLyrics(playTime);
        if (text != null)
            font.render(VertexConsumer.FONT, matrices, (int) (width / 2f), (int) (height / 4f - TextUtils.getHeight(text, font) / 2f), text, Alignment.CENTER);
    }

    private void drawBar(MatrixStack matrices, int i, int bars, float amplitude) {
        float w = Math.max((width - 20 - bars + 1f) / bars, 1f);
        float x = i * (w + 1) + 10;

        if (x > width - 10)
            return;

        float y = height - height / 3f;
        float height = (int) Math.max(amplitude, 1f);
        int color = ColorUtils.rgbToInt(ColorUtils.hsvToRGB(new Vector3f((x - 10f) / (width - 20f), 0.5f, 1f))) + (0xFF << 24);

        //front
        VertexConsumer.GUI.consume(GeometryHelper.rectangle(matrices, x, y - height, x + w, y, color));

        float top = w * 0.5f;
        float side = w * 0.4f;

        //top
        Vertex[] vertices = GeometryHelper.rectangle(matrices, x, y - height - top, x + w, y - height, color - 0x222222);
        vertices[2].getPosition().add(-side, 0, -side);
        vertices[3].getPosition().add(-side, 0, -side);
        VertexConsumer.GUI.consume(vertices);

        //side
        vertices = GeometryHelper.rectangle(matrices, x - side, y - height - top, x, y, color - 0x111111);
        vertices[0].getPosition().add(0, -top, -side);
        vertices[3].getPosition().add(0, 0, -side);
        vertices[2].getPosition().add(0, top, 0);
        VertexConsumer.GUI.consume(vertices);

        //mirror
        vertices = GeometryHelper.rectangle(matrices, x, y, x + w, y + height, color - (0x88 << 24));
        vertices[0].getPosition().add(0.75f * height, 0, 0);
        vertices[1].getPosition().add(0.75f * height, 0, 0);
        VertexConsumer.GUI.consume(vertices);
    }


    // -- sound logic -- //


    private void playSound(int index) {
        //stop the current sound
        if (soundData != null)
            soundData.stop();

        //index out of bounds (shouldn't happen ever)
        if (index < 0 || index >= playlist.size())
            return;

        //grab sound data
        playlistIndex = index;
        Track track = playlist.get(index);
        Resource resource = track.resource;

        //save properties and play the sound instance
        sound = Sound.of(resource);
        soundData = SoundManager.playSound(resource, SoundCategory.MUSIC);
        soundData.loop(repeat == 1);

        //update widgets
        slider.setMax(sound.duration);
        playPauseButton.setImage(PAUSE);

        //notify the user
        Toast.addToast(Text.of("Now playing: %s".formatted(track.title)), font);
    }

    @Override
    public boolean filesDropped(String[] files) {
        List<Track> newPlaylist = new ArrayList<>();

        //go through all .ogg files
        for (String file : files) {
            if (!file.toLowerCase().endsWith(".ogg"))
                continue;

            //load songs as Resources
            Resource song = new Resource("", file);

            Path path = Path.of(file);
            String filename = path.getFileName().toString();
            filename = filename.substring(0, filename.length() - 4);

            Lyrics lyrics;

            //also .lrc lyrics with the same filename as the song
            try {
                lyrics = LrcLoader.loadLyrics(new Resource("", path.getParent().resolve(filename + ".lrc").toString()));
            } catch (Exception ignored) {
                lyrics = null;
            }

            //grab title from the lyrics data
            String title = "";
            if (lyrics != null) {
                if (!lyrics.getTitle().isBlank()) {
                    title = lyrics.getTitle();
                    if (!lyrics.getArtist().isBlank())
                        title += " - " + lyrics.getArtist();
                }
            }

            //if no title, use the filename
            if (title.isBlank())
                title = filename;

            //add the song to the playlist
            newPlaylist.add(new Track(song, lyrics, title));
        }

        //sort the playlist alphabetically by title
        newPlaylist.sort((a, b) -> a.title.compareToIgnoreCase(b.title));

        //update only if we got something!
        int size = newPlaylist.size();
        if (size > 0) {
            //reset variables
            playlist.clear();
            playlistIndex = 0;

            //add playlist
            playlist.addAll(newPlaylist);

            //set playlist buttons to work only if there are more than 1 song
            nextButton.setActive(size > 1);
            previousButton.setActive(size > 1);

            //feebdback to the user
            Toast.addToast(Text.of("Loaded %d song%s!".formatted(size, size > 1 ? "s" : "")), font);

            //automatically play the first song
            playSound(0);
            return true;
        }

        return super.filesDropped(files);
    }

    private record Track(Resource resource, Lyrics lyrics, String title) {
        public Text getLyrics(int time) {
            //no lyrics :(
            if (lyrics == null)
                return null;

            //get the current lyrics index based on its timeframe
            final int offsetedTime = time + lyrics.getOffset();
            int i = Maths.binarySearch(0, lyrics.getLyrics().size() - 1, index -> offsetedTime <= lyrics.getLyrics().get(index).first()) - 1;

            //prev2
            String previous2 = i > 1 ? lyrics.getLyrics().get(i - 2).second() : "";
            //prev
            String previous = i > 0 ? lyrics.getLyrics().get(i - 1).second() : "";
            //current
            String current = i >= 0 ? lyrics.getLyrics().get(i).second() : "";
            //next
            String next = i < lyrics.getLyrics().size() - 1 ? lyrics.getLyrics().get(i + 1).second() : "";
            //next2
            String next2 = i < lyrics.getLyrics().size() - 2 ? lyrics.getLyrics().get(i + 2).second() : "";

            return Text.of(previous2 + "\n").withStyle(Style.EMPTY.color(Colors.LIGHT_GRAY))
                    .append(previous + "\n")
                    .append(Text.of(current).withStyle(Style.EMPTY.color(Colors.WHITE)))
                    .append("\n" + next)
                    .append("\n" + next2);
        }
    }
}
