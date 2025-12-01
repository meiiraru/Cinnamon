package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.ComboBox;
import cinnamon.text.Text;
import cinnamon.utils.FileDialog;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;

import javax.sound.midi.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static cinnamon.Client.LOGGER;

public class MIDIScreen extends ParentedScreen {

    private CompletableFuture<Void> tasks;

    private final List<Pair<String, MidiDevice>>
            inDevices = new ArrayList<>(),
            outDevices = new ArrayList<>();

    private MidiDevice in, out = null;
    private Transmitter transmitter = null;
    private Receiver receiver = null;

    private Soundbank soundbank = null;
    private int instrumentID = 0;

    public MIDIScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        super.init();

        //get available MIDI devices
        runTask(this::fetchMIDIDevices).join();

        ContainerGrid grid = new ContainerGrid(4, 4, 4);

        //open sound font
        Button openButton = new Button(0, 0, 16, 16, null, button -> {
            String file = FileDialog.openFile(new FileDialog.Filter("sound fonts", "sf2,sfz"));
            if (file != null) {
                runTask(() -> {
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
        openButton.setIcon(new Resource("textures/gui/icons/open.png"));
        openButton.setTooltip(Text.translated("gui.midi_screen.load_sound_font"));
        grid.addWidget(openButton);

        //select input device
        ComboBox inBox = new ComboBox(0, 0, 100, 16);
        inBox.setTooltip(Text.translated("gui.midi_screen.input_device"));
        for (Pair<String, MidiDevice> device : inDevices)
            inBox.addEntry(Text.of(device.first()));
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
        grid.addWidget(inBox);

        //select output device
        ComboBox outBox = new ComboBox(0, 0, 100, 16);
        outBox.setTooltip(Text.translated("gui.midi_screen.output_device"));
        for (Pair<String, MidiDevice> device : outDevices)
            outBox.addEntry(Text.of(device.first()));
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
        grid.addWidget(outBox);

        //instrument
        ComboBox instrumentBox = new ComboBox(0, 0, 100, 16);
        instrumentBox.setTooltip(Text.translated("gui.midi_screen.instrument"));
        for (int i = 0; i < 128; i++)
            instrumentBox.addEntry(Text.translated("gui.midi_screen.instrument." + i));
        instrumentBox.setChangeListener(i -> runTask(() -> swapInstrument(i)));
        grid.addWidget(instrumentBox);

        //add grid
        addWidget(grid);
    }

    @Override
    public void removed() {
        super.removed();
        runTask(() -> {
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

        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            outDevices.addFirst(new Pair<>("Synthesizer", synth));
        } catch (Exception e) {
            LOGGER.error("Failed to get Synthesizer", e);
        }
    }

    protected Receiver createReceiver(MidiDevice device) throws Exception {
        Receiver receiver = device.getReceiver();
        return new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                timeStamp = -1;
                log(message, timeStamp);
                receiver.send(message, timeStamp);
            }

            @Override
            public void close() {
                receiver.close();
            }
        };
    }

    protected void closeIn() {
        if (transmitter != null) {
            transmitter.close();
            transmitter = null;
        }
        if (in != null && in.isOpen()) {
            in.close();
            in = null;
        }
    }

    protected void closeOut() {
        if (soundbank != null && out instanceof Synthesizer synth) {
            synth.unloadAllInstruments(soundbank);
            soundbank = null;
        }
        if (receiver != null) {
            receiver.close();
            receiver = null;
        }
        if (out != null && out.isOpen()) {
            out.close();
            out = null;
        }
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
            LOGGER.info("Changed instrument to " + id);

            //load new instrument
            if (soundbank != null && out instanceof Synthesizer synth) {
                Instrument instrument = soundbank.getInstrument(new Patch(0, id));
                if (instrument != null) {
                    synth.loadInstrument(instrument);
                    LOGGER.info("Loaded instrument " + id + " from sound bank");
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

    protected void log(MidiMessage message, long timeStamp) {
        byte[] data = message.getMessage();
        StringBuilder sb = new StringBuilder();
        sb.append("MIDI message (").append(data.length).append(" bytes): ");
        for (byte datum : data)
            sb.append("0x%02X".formatted(datum)).append(" ");
        sb.append("time: ").append(timeStamp);
        LOGGER.info(sb.toString());
    }
}
