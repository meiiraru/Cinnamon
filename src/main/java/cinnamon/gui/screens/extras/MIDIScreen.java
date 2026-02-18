package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.Tickable;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.ComboBox;
import cinnamon.input.InputManager;
import cinnamon.logger.Logger;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.FileDialog;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;
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

    public static Logger LOGGER = new Logger(Logger.ROOT_NAMESPACE + "/MIDI");

    private static final Map<Integer, Integer> keyMap = new HashMap<>();
    static {
        keyMap.put(90, 53); //Z -> F3
        keyMap.put(83, 54); //S -> F#3
        keyMap.put(88, 55); //X -> G3
        keyMap.put(68, 56); //D -> G#3
        keyMap.put(67, 57); //C -> A3
        keyMap.put(70, 58); //F -> A#3
        keyMap.put(86, 59); //V -> B3
        keyMap.put(66, 60); //B -> C4
        keyMap.put(72, 61); //H -> C#4
        keyMap.put(78, 62); //N -> D4
        keyMap.put(74, 63); //J -> D#4
        keyMap.put(77, 64); //M -> E4
        keyMap.put(44, 65); //, -> F4
        keyMap.put(76, 66); //L -> F#4
        keyMap.put(46, 67); //. -> G4
        keyMap.put(59, 68); //; -> G#4
        keyMap.put(47, 69); /// -> A4
        keyMap.put(39, 70); //' -> A#4
    }

    private CompletableFuture<Void> tasks;

    private final List<Pair<String, MidiDevice>>
            inDevices = new ArrayList<>(),
            outDevices = new ArrayList<>();

    private MidiDevice in, out = null;
    private Transmitter transmitter = null;
    private Receiver receiver = null;

    private Soundbank soundbank = null;
    private int instrumentID = 0;

    private Sequencer sequencer = null;
    private Receiver sequencerReceiver = null;

    private boolean log = false;

    private final ComboBox inBox, outBox, instrumentBox;
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
                LOGGER.info("Opened MIDI input device: " + inDevices.get(i).first());
                update();
            } catch (Exception e) {
                in = null;
                transmitter = null;
                Toast.addToast(Text.translated("gui.midi_screen.input_device_open_failed", Text.of(inDevices.get(i).first())));
                LOGGER.error("Failed to open MIDI input device: " + inDevices.get(i).first(), e);
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
                LOGGER.info("Opened MIDI output device: " + outDevices.get(i).first());
                update();
            } catch (Exception e) {
                out = null;
                receiver = null;
                Toast.addToast(Text.translated("gui.midi_screen.output_device_open_failed", Text.of(outDevices.get(i).first())));
                LOGGER.error("Failed to open MIDI output device: " + outDevices.get(i).first(), e);
            }
        }));

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

        /*
        new Await(40, () -> {
           //play sound file
            String file = FileDialog.openFile(new FileDialog.Filter("MIDI files", "mid, midi"));
            if (file != null) {
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
                        sequencer.start();
                        this.sequencer = sequencer;
                        LOGGER.info("Playing MIDI file: " + file);
                    } catch (Exception e) {
                        Toast.addToast(Text.translated("gui.midi_screen.midi_file_play_failed"));
                        LOGGER.error("Failed to play MIDI file: " + file, e);
                    }
                });
            }
        });
         */
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
        Button openButton = new Button(0, 0, 16, 16, null, button -> {
            String file = FileDialog.openFile(new FileDialog.Filter("sound fonts", "sf2,sfz"));
            if (file != null) {
                runTask(() -> {
                    closeInstruments();
                    try {
                        soundbank = MidiSystem.getSoundbank(new File(file));
                        LOGGER.info("Loaded sound font: " + file);
                        update();
                    } catch (Exception e) {
                        Toast.addToast(Text.translated("gui.midi_screen.sound_font_load_failed"));
                        LOGGER.error("Failed to load sound font: " + file, e);
                    }
                });
            }
        });
        openButton.setIcon(new Resource("textures/gui/icons/settings.png"));
        openButton.setTooltip(Text.translated("gui.midi_screen.sound_font"));
        grid.addWidget(openButton);

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

        //add grid
        addWidget(grid);
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
        if (sequencerReceiver != null)
            sequencerReceiver.close();
        sequencerReceiver = null;

        if (sequencer != null && sequencer.isOpen())
            sequencer.close();
        sequencer = null;
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
                    LOGGER.info("Loaded instrument \"" + instrument.getName() + "\" from sound bank");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to change instrument to " + id, e);
        }
    }

    protected void update() {
        //reroute in -> out
        if (transmitter != null && receiver != null)
            transmitter.setReceiver(receiver);

        //set instrument
        swapInstrument(instrumentID);
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
            LOGGER.error("Failed to play note: " + note, e);
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
            LOGGER.error("Failed to stop note: " + note, e);
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
            for (Vector2f press : new ArrayList<>(presses))
                VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, x + b, press.x + d, x + w - b, press.x + press.y + d, sharp ? 0xFFB55E5B : 0xFFD3AB7A));

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
            presses.add(new Vector2f(getY() + 3, 1));
        }
    }
}
