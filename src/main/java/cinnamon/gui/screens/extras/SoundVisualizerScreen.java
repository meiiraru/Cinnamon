package cinnamon.gui.screens.extras;

import cinnamon.gui.GUIStyle;
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
import cinnamon.vr.XrManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static cinnamon.Client.LOGGER;

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
            NEXT = new Resource("textures/gui/icons/next.png"),
            OPEN = new Resource("textures/gui/icons/open.png"),
            CLOSE = new Resource("textures/gui/icons/close.png");

    private final List<Track> playlist = new ArrayList<>();
    private int playlistIndex = -1;

    private Sound sound;
    private SoundInstance soundData;
    private final SoundSpectrum spectrum = new SoundSpectrum();

    private Slider slider;
    private Button playPauseButton, nextButton, previousButton;
    private int repeat = 0; //0 = off, 1 = one, 2 = all

    private boolean settingsDirty = false;

    public SoundVisualizerScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void removed() {
        super.removed();
        //stop the sound when the screen is closed
        if (soundData != null)
            soundData.stop();
        //save settings if we changed the volume or changed the default device
        if (settingsDirty)
            Settings.save();
    }

    @Override
    public void init() {
        //widgets init

        //open files
        Button openButton = new Button(4, 4, 16, 16, null, button -> {
            List<String> files = FileDialog.openFiles(FileDialog.Filter.AUDIO_FILES);
            if (!files.isEmpty())
                loadTracks(files.toArray(new String[0]));
        });
        openButton.setIcon(OPEN);
        openButton.setTooltip(Text.translated("gui.music_screen.load"));
        addWidget(openButton);

        //buttons grid
        ContainerGrid buttons = new ContainerGrid(0, 0, 16, 3);
        buttons.setAlignment(Alignment.TOP_CENTER);

        //previous track button
        previousButton = new Button(0, 0, 16, 16, null, button -> {
            if (playlistIndex >= 0)
                playSound((playlistIndex - 1 + playlist.size()) % playlist.size());
        });
        previousButton.setIcon(PREVIOUS);
        previousButton.setTooltip(Text.translated("gui.music_screen.previous"));
        previousButton.setSilent(true);
        previousButton.setActive(playlist.size() > 1);
        buttons.addWidget(previousButton);

        //center buttons grid
        ContainerGrid centerButtons = new ContainerGrid(0, 0, 4, 3);
        centerButtons.setAlignment(Alignment.TOP_CENTER);

        //play/pause button
        playPauseButton = new Button(0, 0, 16, 16, null, button -> {
            if (soundData != null) {
                if (soundData.isPlaying()) {
                    soundData.pause();
                    button.setIcon(PLAY);
                    button.setTooltip(Text.translated("gui.music_screen.play"));
                } else {
                    soundData.play();
                    button.setIcon(PAUSE);
                    button.setTooltip(Text.translated("gui.music_screen.pause"));
                }
            } else {
                playSound(playlistIndex);
            }
        });
        boolean pause = soundData != null && soundData.isPlaying();
        playPauseButton.setIcon(pause ? PAUSE : PLAY);
        playPauseButton.setTooltip(Text.translated(pause ? "gui.music_screen.pause" : "gui.music_screen.play"));
        playPauseButton.setSilent(true);
        centerButtons.addWidget(playPauseButton);

        //stop button
        Button stopButton = new Button(0, 0, 16, 16, null, button -> {
            if (soundData != null) {
                soundData.pause();
                playPauseButton.setIcon(PLAY);
                slider.updateValue(0);
            }
        });
        stopButton.setIcon(STOP);
        stopButton.setTooltip(Text.translated("gui.music_screen.stop"));
        stopButton.setSilent(true);
        centerButtons.addWidget(stopButton);

        //repeat-mode button
        Button repeatButton = new Button(0, 0, 16, 16, null, button -> {
            repeat = (repeat + 1) % 3;
            if (soundData != null)
                soundData.loop(repeat == 1);
            switch (repeat) {
                case 1 -> {
                    button.setIcon(REPEAT_ONE);
                    button.setTooltip(Text.translated("gui.music_screen.repeat_one"));
                }
                case 2 -> {
                    button.setIcon(REPEAT);
                    button.setTooltip(Text.translated("gui.music_screen.repeat_all"));
                }
                default -> {
                    button.setIcon(REPEAT_OFF);
                    button.setTooltip(Text.translated("gui.music_screen.repeat_off"));
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
        nextButton.setIcon(NEXT);
        nextButton.setTooltip(Text.translated("gui.music_screen.next"));
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
        volume.setUpdateListener((f, i) -> {
            SoundCategory.MUSIC.setVolume(f);
            settingsDirty = true;
        });
        volume.setTooltipFunction((f, i) -> Text.translated("gui.music_screen.volume", i));
        addWidget(volume);

        //output device
        ComboBox device = new ComboBox(4, volume.getY() - 4 - 16, 50, 16);

        device.addEntry(Text.translated("gui.default"));
        List<String> devices = SoundManager.getDevices();
        for (String string : devices)
            device.addEntry(Text.of(string.replaceFirst("^OpenAL Soft on ", "")));

        device.setSelected(devices.indexOf(SoundManager.getCurrentDevice()) + 1);

        device.setChangeListener(i -> {
            boolean playing = soundData != null && soundData.isPlaying();
            float f = slider.getPercentage();

            SoundManager.swapDevice(i == 0 ? "" : devices.get(i - 1));
            Settings.soundDevice.set(SoundManager.getCurrentDevice());
            settingsDirty = true;

            playSound(playlistIndex);
            slider.setPercentage(f);
            if (!playing) playPauseButton.onRun();
        });

        addWidget(device);

        super.init();
    }

    @Override
    protected void addBackButton() {
        //super.addBackButton();

        //close button
        Button closeButton = new Button(width - 4 - 16, 4, 16, 16, null, button -> close());
        closeButton.setIcon(CLOSE);
        closeButton.setTooltip(Text.translated("gui.close"));
        addWidget(closeButton);
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
                    playPauseButton.setIcon(PLAY);
                    slider.updatePercentage(0f);
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

        float lineHeight = GUIStyle.getDefault().getFont().lineHeight;

        //draw top text
        if (songCount == 0)
            Text.translated("gui.music_screen.help").withStyle(Style.EMPTY.color(Colors.WHITE)).render(VertexConsumer.MAIN, matrices, (int) (width / 2f), 4, Alignment.TOP_CENTER);

        //draw timers
        int x = slider.getX();
        int y = (int) (slider.getCenterY() - lineHeight / 2);
        int now = playTime / 1000;
        int max = slider.getMax() / 1000;
        Text.of("%d:%02d".formatted(now / 60, now % 60)).render(VertexConsumer.MAIN, matrices, x - 4, y, Alignment.TOP_RIGHT);
        Text.of("%d:%02d".formatted(max / 60, max % 60)).render(VertexConsumer.MAIN, matrices, x + slider.getWidth() + 4, y);

        if (songCount == 0)
            return;

        //song count
        if (songCount > 1)
            Text.of("%d / %d".formatted(playlistIndex + 1, songCount)).render(VertexConsumer.MAIN, matrices, (int) (width / 2f), playPauseButton.getY() - lineHeight - 4, Alignment.TOP_CENTER);

        Track track = playlist.get(playlistIndex);

        //title
        Text.of(track.title).render(VertexConsumer.MAIN, matrices, (int) (width / 2f), playPauseButton.getY() - (lineHeight + 4) * (songCount > 1 ? 2 : 1), Alignment.TOP_CENTER);

        //lyrics
        Text text = track.getLyrics(playTime);
        if (text != null)
            text.render(VertexConsumer.MAIN, matrices, (int) (width / 2f), (int) (height * 0.1f), Alignment.TOP_CENTER);
    }

    private void drawBar(MatrixStack matrices, int i, int bars, float amplitude) {
        float w = Math.max((width - 20 - bars + 1f) / bars, 1f);
        float x = i * (w + 1) + 10;

        if (x > width - 10)
            return;

        float y = height - height / 3f;
        float height = (int) Math.max(amplitude, 1f);
        int color = ColorUtils.rgbToInt(ColorUtils.hsvToRGB(new Vector3f((x - 10f) / (width - 20f), 0.5f, 1f))) + (0xFF << 24);
        float d = UIHelper.getDepthOffset();

        if (XrManager.isInXR()) {
            //main
            Vertex[][] box = GeometryHelper.box(matrices, x, y - height, -d, x + w, y, -10f, color);
            int dark = color - 0x222222;
            box[0][3].color(dark);
            box[2][0].color(dark);
            box[2][1].color(dark);
            box[2][2].color(dark);
            box[2][3].color(dark);
            VertexConsumer.MAIN.consume(box);

            //mirror
            box = GeometryHelper.box(matrices, x, y, -d, x + w, y + height, -10f, color - (0x88 << 24));
            int alpha = (int) Math.min(Maths.lerp(0x00, 0xFF, height / 30f), 0xFF);
            int transparent = color - (alpha << 24);
            box[0][0].color(transparent);
            box[0][1].color(transparent);
            box[2][0].color(transparent);
            box[2][1].color(transparent);
            VertexConsumer.MAIN.consume(box);
        } else {
            float top = w * 0.5f;
            float side = w * 0.4f;
            Matrix4f pos = matrices.peek().pos();

            //front
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, x, y - height, x + w, y, -d, color));

            //top
            Vertex[] vertices = GeometryHelper.rectangle(matrices, x, y - height - top, x + w, y - height, -d, color - 0x222222);
            Vector3f add = new Vector3f(-side, 0, -side).mulDirection(pos);
            vertices[2].getPosition().add(add);
            vertices[3].getPosition().add(add);
            VertexConsumer.MAIN.consume(vertices);

            //side
            vertices = GeometryHelper.rectangle(matrices, x - side, y - height - top, x, y, -d, color - 0x111111);
            vertices[0].getPosition().add(add.set(0, -top, -side).mulDirection(pos));
            vertices[3].getPosition().add(add.set(0, 0, -side).mulDirection(pos));
            vertices[2].getPosition().add(add.set(0, top, 0).mulDirection(pos));
            VertexConsumer.MAIN.consume(vertices);

            //mirror
            vertices = GeometryHelper.rectangle(matrices, x, y, x + w, y + height, -d, color - (0x88 << 24));
            add.set(0.75f * height, 0, 0).mulDirection(pos);
            vertices[0].getPosition().add(add);
            vertices[1].getPosition().add(add);
            VertexConsumer.MAIN.consume(vertices);
        }
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
        slider.setMax(sound != null ? sound.duration : 1);
        playPauseButton.setIcon(PAUSE);

        //notify the user
        Toast.addToast(Text.translated("gui.music_screen.now_playing", track.title));
    }

    @Override
    public boolean filesDropped(String[] files) {
        return loadTracks(files) || super.filesDropped(files);
    }

    private boolean loadTracks(String[] files) {
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

            Lyrics lyrics = null;

            //also .lrc lyrics with the same filename as the song
            Resource res = new Resource("", path.getParent().resolve(filename + ".lrc").toString());
            if (IOUtils.hasResource(res)) {
                try {
                    lyrics = LrcLoader.loadLyrics(res);
                } catch (Exception e) {
                    LOGGER.error("Failed to load lyrics file \"%s\"", res, e);
                }
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
            Toast.addToast(Text.translated(size == 1 ? "gui.music_screen.load_end" : "gui.music_screen.load_end_plural", size));

            //automatically play the first song
            playSound(0);
            return true;
        }

        return false;
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
