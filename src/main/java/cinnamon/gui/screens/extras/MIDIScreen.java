package cinnamon.gui.screens.extras;

import cinnamon.Client;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.Tickable;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.ComboBox;
import cinnamon.gui.widgets.types.Label;
import cinnamon.gui.widgets.types.Slider;
import cinnamon.input.InputManager;
import cinnamon.logger.Logger;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import org.joml.Math;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import javax.sound.midi.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;

public class MIDIScreen extends ParentedScreen {

    public static final Resource
            PLAY_ICON = new Resource("textures/gui/icons/play.png"),
            PAUSE_ICON = new Resource("textures/gui/icons/pause.png");

    public static Logger LOGGER = new Logger(Logger.ROOT_NAMESPACE + "/MIDI");

    private static final Map<Integer, Integer> keyMap = new HashMap<>();
    static {
        keyMap.put(90, 53); // Z -> F3
        keyMap.put(83, 54); // S -> F#3
        keyMap.put(88, 55); // X -> G3
        keyMap.put(68, 56); // D -> G#3
        keyMap.put(67, 57); // C -> A3
        keyMap.put(70, 58); // F -> A#3
        keyMap.put(86, 59); // V -> B3
        keyMap.put(66, 60); // B -> C4
        keyMap.put(72, 61); // H -> C#4
        keyMap.put(78, 62); // N -> D4
        keyMap.put(74, 63); // J -> D#4
        keyMap.put(77, 64); // M -> E4
        keyMap.put(44, 65); // , -> F4
        keyMap.put(76, 66); // L -> F#4
        keyMap.put(46, 67); // . -> G4
        keyMap.put(59, 68); // ; -> G#4
        keyMap.put(47, 69); // / -> A4
        keyMap.put(39, 70); // ' -> A#4
    }

    private CompletableFuture<Void> tasks;

    private final List<Pair<String, MidiDevice>>
            inDevices = new ArrayList<>(),
            outDevices = new ArrayList<>();

    private MidiDevice in, out = null;
    private Transmitter transmitter = null;
    private Receiver receiver = null;

    private Soundbank soundbank = null;
    private int instrumentID = 0, reverb = 0;

    private Sequencer sequencer = null;
    private Receiver sequencerReceiver = null;

    private final boolean log = false;

    private final ComboBox inBox, outBox, instrumentBox;
    private final Slider reverbSlider;
    private final Button playPauseButton;
    private final Slider playbackSlider;
    private final Label playbackLabel, endTimeLabel;

    private final List<Key> keys = new ArrayList<>();

    public MIDIScreen(Screen parentScreen) {
        super(parentScreen);

        //midi-in
        inBox = new ComboBox(0, 0, 100, 16);
        inBox.setTooltip(Text.translated("gui.midi_screen.input_device"));
        inBox.setChangeListener(i -> runTask(() -> {
            closeIn();
            try {
                MidiDevice device = inDevices.get(i).second();
                device.open();
                in = device;
                transmitter = in.getTransmitter();
                LOGGER.info("Opened MIDI input device: %s", inDevices.get(i).first());
                update();
            } catch (Exception e) {
                in = null;
                transmitter = null;
                Toast.addToast(Text.translated("gui.midi_screen.input_device_open_failed", Text.of(inDevices.get(i).first())));
                LOGGER.error("Failed to open MIDI input device: %s", inDevices.get(i).first(), e);
            }
        }));

        //midi-out
        outBox = new ComboBox(0, 0, 100, 16);
        outBox.setTooltip(Text.translated("gui.midi_screen.output_device"));
        outBox.setChangeListener(i -> runTask(() -> {
            closeOut();
            try {
                MidiDevice device = outDevices.get(i).second();
                device.open();
                out = device;
                receiver = createReceiver(out);
                LOGGER.info("Opened MIDI output device: %s", outDevices.get(i).first());
                update();
            } catch (Exception e) {
                out = null;
                receiver = null;
                Toast.addToast(Text.translated("gui.midi_screen.output_device_open_failed", Text.of(outDevices.get(i).first())));
                LOGGER.error("Failed to open MIDI output device: %s", outDevices.get(i).first(), e);
            }
        }));

        //default out to synthesizer (Gervill)
        runTask(() -> {
            try {
                MidiDevice synth = MidiSystem.getSynthesizer();
                outDevices.addFirst(new Pair<>("Synthesizer", synth));
                outBox.addEntry(Text.of("Synthesizer"));
                outBox.select(0);
            } catch (Exception e) {
                LOGGER.error("Failed to get Synthesizer", e);
            }
        }).join();

        //instruments
        instrumentBox = new ComboBox(0, 0, 100, 16);
        instrumentBox.setTooltip(Text.translated("gui.midi_screen.instrument"));
        for (int i = 0; i < 128; i++) {
            if (i > 0 && i % 8 == 0)
                instrumentBox.addDivider();
            instrumentBox.addEntry(Text.of(i + 1).append(". ").append(Text.translated("midi.instrument." + i)));
        }
        instrumentBox.setChangeListener(i -> runTask(() -> swapInstrument(i)));
        instrumentBox.setSelected(instrumentID);

        //reverb slider
        reverbSlider = new Slider(0, 0, 100);
        reverbSlider.setMin(0);
        reverbSlider.setMax(127);
        reverbSlider.setTooltipFunction((f, i) -> Text.translated("gui.midi_screen.reverb", Math.round(f * 100f) + "%"));
        reverbSlider.setChangeListener((f, i) -> runTask(() -> setReverb(i)));

        //play/pause button
        playPauseButton = new Button(0, 0, 16, 16, null, button -> runTask(() -> {
            if (sequencer != null && sequencer.isOpen()) {
                if (sequencer.isRunning()) {
                    sequencer.stop();
                    button.setIcon(PLAY_ICON);
                    button.setTooltip(Text.translated("gui.midi_screen.play"));
                } else {
                    sequencer.start();
                    button.setIcon(PAUSE_ICON);
                    button.setTooltip(Text.translated("gui.midi_screen.pause"));
                }
            }
        }));
        playPauseButton.setIcon(PLAY_ICON);
        playPauseButton.setTooltip(Text.translated("gui.midi_screen.play"));

        //playback labels
        playbackLabel = new Label(0, 0, Text.of("0:00"));
        endTimeLabel = new Label(0, 0, Text.of("0:00"));

        //playback slider
        playbackSlider = new Slider(0, 0, 100);
        playbackSlider.setTooltipFunction((f, i) -> Text.of("%d:%02d".formatted(i / 1000 / 60, (i / 1000) % 60)));
        playbackSlider.setChangeListener((f, i) -> runTask(() -> {
            if (sequencer != null && sequencer.isOpen())
                sequencer.setMicrosecondPosition(i * 1_000L);
            playbackLabel.setText(Text.of("%d:%02d".formatted(i / 1000 / 60, (i / 1000) % 60)));
        }));
        playbackSlider.setPreviewHoverValueTooltip(true);
    }

    @Override
    public void init() {
        super.init();

        //piano keys
        int octaves = 4;

        int notes = octaves * 14 + 1;
        int startingNote = 60 - 12 * (int) Math.ceil((octaves - 1) / 2f); //60 = C4

        int keyWidth = Math.min(width / (notes / 2 + 1), 20);
        int keyHeight = 60;
        int sharpKeyWidth = (int) (keyWidth * 0.6f);
        int sharpKeyHeight = keyHeight / 2;

        int startX = (width - ((notes / 2 + 1) * keyWidth)) / 2;
        int y = height - keyHeight - 4;

        keys.clear();
        List<Key> sharpKeys = new ArrayList<>();

        for (int i = 0, j = 0; i < notes; i++) {
            int key = i % 14;
            if (key == 5 || key == 13) //theres no E# nor B#
                continue;

            boolean sharp = key % 2 == 1;
            int x = startX + i / 2 * keyWidth;
            int note = startingNote + j;
            j++;

            Key k;
            if (sharp)
                sharpKeys.add(k = new Key(x + keyWidth - (sharpKeyWidth / 2), y, sharpKeyWidth, sharpKeyHeight, note, true, this));
            else
                addWidget(k = new Key(x, y, keyWidth, keyHeight, note, false, this));

            keys.add(k);
        }

        //add sharp keys on top
        for (Key sharpKey : sharpKeys)
            addWidget(sharpKey);

        //get available MIDI devices
        runTask(this::fetchMIDIDevices).join();

        ContainerGrid grid = new ContainerGrid(4, 4, 4);

        //open sound font
        ContainerGrid topButtons = new ContainerGrid(0, 0, 4, 999);
        grid.addWidget(topButtons);

        Button openButton = new Button(0, 0, 16, 16, null, _ -> {
            String file = FileDialog.openFile(new FileDialog.Filter("sound fonts", "sf2,sfz"));
            if (file == null)
                return;

            runTask(() -> {
                closeInstruments();
                try {
                    soundbank = MidiSystem.getSoundbank(new File(file));
                    LOGGER.info("Loaded sound font: %s", file);
                    update();
                } catch (Exception e) {
                    Toast.addToast(Text.translated("gui.midi_screen.sound_font_load_failed"));
                    LOGGER.error("Failed to load sound font: %s", file, e);
                }
            });
        });
        openButton.setIcon(new Resource("textures/gui/icons/settings.png"));
        openButton.setTooltip(Text.translated("gui.midi_screen.sound_font"));
        topButtons.addWidget(openButton);

        Button openFileButton = new Button(0, 0, 16, 16, null, _ -> {
            String file = FileDialog.openFile(new FileDialog.Filter("MIDI files", "mid, midi"));
            if (file == null)
                return;

            runTask(() -> {
                closeSequencer();
                try {
                    Sequence sequence = MidiSystem.getSequence(new File(file));
                    Sequencer sequencer = MidiSystem.getSequencer();
                    sequencer.setSequence(sequence);
                    if (out != null && out.isOpen()) {
                        this.sequencerReceiver = createReceiver(out);
                        sequencer.getTransmitter().setReceiver(sequencerReceiver);
                    }
                    sequencer.open();
                    this.sequencer = sequencer;

                    int len = (int) (sequencer.getMicrosecondLength() / 1_000);
                    playbackSlider.setMax(len);
                    endTimeLabel.setText(Text.of("%d:%02d".formatted(len / 1000 / 60, (len / 1000) % 60)));

                    playPauseButton.executeAction();
                    LOGGER.info("Playing MIDI file: %s", file);
                } catch (Exception e) {
                    Toast.addToast(Text.translated("gui.midi_screen.midi_file_play_failed"));
                    LOGGER.error("Failed to play MIDI file: %s", file, e);
                }
            });
        });
        openFileButton.setIcon(new Resource("textures/gui/icons/open.png"));
        openFileButton.setTooltip(Text.translated("gui.midi_screen.play_midi_file"));
        topButtons.addWidget(openFileButton);

        //reset all state
        Button resetButton = new Button(0, 0, 16, 16, null, _ -> runTask(this::resetAll));
        resetButton.setIcon(new Resource("textures/gui/icons/reload.png"));
        resetButton.setTooltip(Text.translated("gui.midi_screen.reset"));
        topButtons.addWidget(resetButton);

        //input devices
        inBox.clearEntries();
        for (Pair<String, MidiDevice> device : inDevices) {
            inBox.addEntry(Text.of(device.first()));
            if (in != null && in.getDeviceInfo().equals(device.second().getDeviceInfo()))
                inBox.setSelected(inBox.getEntryCount() - 1);
        }
        grid.addWidget(inBox);

        //output devices
        outBox.clearEntries();
        for (Pair<String, MidiDevice> device : outDevices) {
            outBox.addEntry(Text.of(device.first()));
            if (out != null && out.getDeviceInfo().equals(device.second().getDeviceInfo()))
                outBox.setSelected(outBox.getEntryCount() - 1);
        }
        grid.addWidget(outBox);

        //instruments
        grid.addWidget(instrumentBox);

        //reverb
        grid.addWidget(reverbSlider);

        //add grid
        addWidget(grid);

        //player controls
        ContainerGrid playerControls = new ContainerGrid(width / 2, 4, 4, 999);
        playerControls.setAlignment(Alignment.TOP_CENTER);
        addWidget(playerControls);

        ContainerGrid playback = new ContainerGrid(0, 0, 4, 999);
        playback.setAlignment(Alignment.CENTER);
        playerControls.addWidget(playback);

        playback.addWidget(playPauseButton);
        playback.addWidget(playbackLabel);
        playback.addWidget(playbackSlider);
        playback.addWidget(endTimeLabel);
    }

    @Override
    public void removed() {
        super.removed();
        runTask(() -> {
            closeSequencer();
            closeIn();
            closeOut();
        });
    }

    @Override
    protected void addBackButton() {
        //close button
        Button closeButton = new Button(width - 4 - 16, 4, 16, 16, null, button -> close());
        closeButton.setIcon(new Resource("textures/gui/icons/close.png"));
        closeButton.setTooltip(Text.translated("gui.close"));
        addWidget(closeButton);
    }

    @Override
    public void tick() {
        super.tick();
        if (in != null && !in.isOpen())
            runTask(this::closeIn);
        if (out != null && !out.isOpen())
            runTask(this::closeOut);

        //update playback slider and label
        runTask(() -> {
            if (sequencer != null && sequencer.isOpen() && sequencer.isRunning()) {
                int pos = (int) (sequencer.getMicrosecondPosition() / 1_000);
                if (!playbackSlider.isDragged())
                    playbackSlider.updateValue(pos);
                playbackLabel.setText(Text.of("%d:%02d".formatted(pos / 1000 / 60, (pos / 1000) % 60)));
            } else if (!playPauseButton.getIcon().equals(PLAY_ICON)) {
                playPauseButton.setIcon(PLAY_ICON);
                playPauseButton.setTooltip(Text.translated("gui.midi_screen.play"));
                playbackSlider.setPercentage(0f);
            }
        });
    }

    protected CompletableFuture<Void> runTask(Runnable toRun) {
        if (tasks == null || tasks.isDone())
            tasks = CompletableFuture.runAsync(toRun);
        else
            tasks = tasks.thenRunAsync(toRun);
        return tasks;
    }

    protected void fetchMIDIDevices() {
        inDevices.clear();
        outDevices.clear();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : infos) {
            String name = info.getName();
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxTransmitters() != 0)
                    inDevices.add(new Pair<>(name, device));
                if (device.getMaxReceivers() != 0)
                    outDevices.add(new Pair<>(name, device));
            } catch (Exception e) {
                LOGGER.error("Failed to get device %s", name, e);
            }
        }
    }

    protected Receiver createReceiver(MidiDevice device) throws Exception {
        Receiver receiver = device.getReceiver();
        return new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                timeStamp = -1;
                if (message instanceof ShortMessage sm) {
                    int command = sm.getCommand();
                    if (command == NOTE_ON || command == NOTE_OFF)
                        pressKey(sm.getData1(), command == NOTE_ON && sm.getData2() > 0);
                }

                if (log) {
                    byte[] data = message.getMessage();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unknown MIDI message (").append(data.length).append(" bytes): ");
                    for (byte datum : data)
                        sb.append("%d".formatted(datum)).append(" ");
                    sb.append("time: ").append(timeStamp);
                    LOGGER.info(sb.toString());
                }

                if (receiver != null)
                    receiver.send(message, timeStamp);
            }

            @Override
            public void close() {
                if (receiver != null)
                    receiver.close();
            }
        };
    }

    protected void closeIn() {
        if (transmitter != null)
            transmitter.close();
        transmitter = null;

        if (in != null && in.isOpen())
            in.close();
        in = null;

        for (Key key : keys)
            key.setVirtuallyPressed(false);
    }

    protected void closeOut() {
        closeInstruments();

        if (receiver != null)
            receiver.close();
        receiver = null;

        if (out != null && out.isOpen())
            out.close();
        out = null;

        for (Key key : keys)
            key.stop();
    }

    protected void closeInstruments() {
        if (soundbank != null && out instanceof Synthesizer synth)
            synth.unloadAllInstruments(soundbank);
        soundbank = null;
    }

    protected void closeSequencer() {
        if (sequencer != null && sequencer.isOpen())
            sequencer.close();
        sequencer = null;

        if (sequencerReceiver != null)
            sequencerReceiver.close();
        sequencerReceiver = null;
    }

    protected void swapInstrument(int id) {
        if (receiver == null)
            return;

        try {
            //send swap instrument message
            ShortMessage programChange = new ShortMessage();
            programChange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, id, 0);
            receiver.send(programChange, -1);
            instrumentID = id;

            //load new instrument
            if (soundbank != null && out instanceof Synthesizer synth) {
                Instrument instrument = soundbank.getInstrument(new Patch(0, id));
                if (instrument != null) {
                    synth.loadInstrument(instrument);
                    LOGGER.info("Loaded instrument \"%s\" from sound bank", instrument.getName());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to change instrument to %s", id, e);
        }
    }

    protected void setReverb(int value) {
        if (receiver == null)
            return;

        try {
            ShortMessage reverbChange = new ShortMessage();
            reverbChange.setMessage(ShortMessage.CONTROL_CHANGE, 0, 91, value);
            receiver.send(reverbChange, -1);
            reverb = value;
        } catch (Exception e) {
            LOGGER.error("Failed to change reverb to %s", value, e);
        }
    }

    protected void resetAll() {
        //send reset message sequence to all 16 channels
        if (receiver != null) {
            try {
                for (int i = 0; i < 16; i++) {
                    ShortMessage reset = new ShortMessage();

                    reset.setMessage(ShortMessage.CONTROL_CHANGE, i, 120, 0); //all sound off
                    receiver.send(reset, -1);
                    reset.setMessage(ShortMessage.CONTROL_CHANGE, i, 123, 0); //all notes off
                    receiver.send(reset, -1);
                    reset.setMessage(ShortMessage.CONTROL_CHANGE, i, 121, 0); //reset all controllers
                    receiver.send(reset, -1);
                    reset.setMessage(ShortMessage.CONTROL_CHANGE, i, 7, 127); //reset volume to 127
                    receiver.send(reset, -1);
                    reset.setMessage(ShortMessage.CONTROL_CHANGE, i, 10, 64); //reset pan to center
                    receiver.send(reset, -1);
                    reset.setMessage(ShortMessage.PITCH_BEND, i, 0, 64); //reset pitch bend to center
                    receiver.send(reset, -1);
                    reverbSlider.setValue(0); //reset reverb to 0
                }

                //release all notes
                for (Key key : keys)
                    key.setVirtuallyPressed(false);
            } catch (Exception e) {
                LOGGER.error("Failed to reset all MIDI settings", e);
            }
        }
    }

    protected void update() {
        //reroute in -> out
        if (transmitter != null && receiver != null)
            transmitter.setReceiver(receiver);

        //set instrument
        swapInstrument(instrumentID);

        //reverb
        setReverb(reverb);
    }

    protected void playNote(int note, int velocity) {
        if (receiver == null)
            return;

        try {
            //note on
            ShortMessage noteOn = new ShortMessage();
            noteOn.setMessage(NOTE_ON, 0, note, velocity);
            receiver.send(noteOn, -1);
        } catch (Exception e) {
            LOGGER.error("Failed to play note: %s", note, e);
        }
    }

    protected void stopNote(int note) {
        if (receiver == null)
            return;

        try {
            //note off
            ShortMessage noteOff = new ShortMessage();
            noteOff.setMessage(NOTE_OFF, 0, note, 0);
            receiver.send(noteOff, -1);
        } catch (Exception e) {
            LOGGER.error("Failed to stop note: %s", note, e);
        }
    }

    protected void pressKey(int note, boolean press) {
        for (Key key : keys) {
            if (key.key == note) {
                key.setVirtuallyPressed(press);
                return;
            }
        }
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        Integer i = keyMap.get(key);
        if (i != null) {
            if (action == GLFW.GLFW_PRESS)
                playNote(i, 100);
            else if (action == GLFW.GLFW_RELEASE)
                stopNote(i);
            return true;
        }

        return super.keyPress(key, scancode, action, mods);
    }

    private static class Key extends Button implements Tickable {

        public static final String[] keys = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        private static final float NOTE_TICK_SPEED = 3f;

        private final MIDIScreen parent;
        private final int key;
        private final boolean sharp;

        private final List<Vector2f> presses = new ArrayList<>();

        private boolean pressed, isVirtuallyPressed;

        public Key(int x, int y, int width, int height, int key, boolean sharp, MIDIScreen parent) {
            super(x, y, width, height, Text.empty(), null);
            this.parent = parent;
            this.key = key;
            this.sharp = sharp;
            setSilent(true);
        }

        @Override
        public void tick() {
            if (presses.isEmpty())
                return;

            if (isPressed())
                presses.getLast().y += NOTE_TICK_SPEED;

            Iterator<Vector2f> iterator = presses.iterator();
            while (iterator.hasNext()) {
                Vector2f press = iterator.next();
                if (press.x + press.y <= 0)
                    iterator.remove();
                press.x -= NOTE_TICK_SPEED;
            }
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            float b = 0.5f;
            boolean pressed = isPressed();

            //render presses
            float d = Math.lerp(NOTE_TICK_SPEED, 0, delta);
            int color = getSkin().getInt("accent_color");
            int color2 = ColorUtils.rgbToInt(ColorUtils.intToRGB(color).mul(0.65f)) + 0xFF000000;

            for (Vector2f press : new ArrayList<>(presses))
                VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, x + b, press.x + d, x + w - b, press.x + press.y + d, sharp ? color2 : color));

            //render key
            matrices.pushMatrix();

            float depth = UIHelper.getDepthOffset();
            matrices.translate(0f, 0f, depth * (sharp ? 2 : 0));
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, x, y + (pressed && !sharp ? 4 : 0), x + w, y + h, sharp ? 0xFF000000 : 0xFFDDDDDD));

            matrices.translate(0f, 0f, depth);
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, x + b, y + (pressed && !sharp ? 4 : 0) + b, x + w - b, y + h - (pressed ? 0 : 4) - b, sharp ? 0xFF303030 : 0xFFFFFFFF));

            //render label
            String label = keys[key % 12];
            if (key % 12 == 0)
                label += (key / 12) - 1; //octave number

            matrices.translate(0f, 0f, depth);
            Text.of(label)
                    .withStyle(Style.EMPTY.outlined(true))
                    .render(VertexConsumer.MAIN, matrices, getCenterX(), y + h - 6 + (pressed ? 4 : 0), Alignment.BOTTOM_CENTER);

            matrices.popMatrix();
        }

        public boolean isPressed() {
            return pressed || isVirtuallyPressed;
        }

        public void play() {
            if (pressed)
                return;

            pressed = true;
            parent.runTask(() -> parent.playNote(key, 100));
            addPress();
        }

        public void stop() {
            if (!pressed)
                return;

            pressed = false;
            parent.runTask(() -> parent.stopNote(key));
        }

        @Override
        public GUIListener mousePress(int button, int action, int mods) {
            if (!isHovered())
                return null;

            if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                if (action == GLFW.GLFW_PRESS)
                    play();
                else if (action == GLFW.GLFW_RELEASE)
                    stop();
                return this;
            }

            return null;
        }

        @Override
        public GUIListener mouseMove(int x, int y) {
            GUIListener sup = super.mouseMove(x, y);
            if (sup != null)
                return sup;

            if (!isHovered())
                stop();
            else if (InputManager.isMousePressed(GLFW.GLFW_MOUSE_BUTTON_1)) {
                play();
            }

            return null;
        }

        @Override
        protected void updateHover(int x, int y) {
            if (parent.getWidgetAt(x, y) != this)
                setHovered(false);
            else
                super.updateHover(x, y);
        }

        public void setVirtuallyPressed(boolean bool) {
            if (!isVirtuallyPressed && bool)
                addPress();
            isVirtuallyPressed = bool;
        }

        protected void addPress() {
            Client.getInstance().queueTick(() -> presses.add(new Vector2f(getY() + 3, 1)));
        }
    }
}
