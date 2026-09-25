package cinnamon.events;

import cinnamon.render.MatrixStack;

public class CoreEvents {

    //lifecycle events
    public static final Event<Empty> CLIENT_INIT         = Empty.create();
    public static final Event<Empty> CLIENT_EXIT         = Empty.create();
    public static final Event<Empty> TICK_BEFORE_WORLD   = Empty.create();
    public static final Event<Empty> TICK_BEFORE_GUI     = Empty.create();
    public static final Event<Empty> TICK_END            = Empty.create();
    public static final Event<Render> RENDER_BEFORE_WORLD = Render.create();
    public static final Event<Render> RENDER_BEFORE_GUI   = Render.create();
    public static final Event<Render> RENDER_END          = Render.create();
    public static final Event<Empty> RESOURCE_INIT       = Empty.create();
    public static final Event<Empty> RESOURCE_FREE       = Empty.create();

    //window events
    public static final Event<WindowMove>    WINDOW_MOVE    = WindowMove.create();
    public static final Event<WindowResize>  WINDOW_RESIZE  = WindowResize.create();
    public static final Event<WindowFocused> WINDOW_FOCUSED = WindowFocused.create();
    public static final Event<FilesDropped>  FILES_DROPPED  = FilesDropped.create();

    //input events
    public static final Event<KeyPress>   KEY_PRESS    = KeyPress.create();
    public static final Event<CharTyped>  CHAR_TYPED   = CharTyped.create();
    public static final Event<MousePress> MOUSE_PRESS  = MousePress.create();
    public static final Event<Mouse2D>    MOUSE_MOVE   = Mouse2D.create();
    public static final Event<Mouse2D>    MOUSE_SCROLL = Mouse2D.create();

    //xr events
    public static final Event<DeviceButton>   XR_BUTTON_PRESS  = DeviceButton.create();
    public static final Event<DeviceAxis>     XR_TRIGGER_PRESS = DeviceAxis.create();
    public static final Event<XrJoystickMove> XR_JOYSTICK_MOVE = XrJoystickMove.create();

    //joystick and gamepad events
    public static final Event<DeviceButton>    JOYSTICK_BUTTON_PRESS = DeviceButton.create();
    public static final Event<DeviceAxis>      JOYSTICK_AXIS_MOVE    = DeviceAxis.create();
    public static final Event<JoystickHatMove> JOYSTICK_HAT_MOVE     = JoystickHatMove.create();
    public static final Event<DeviceButton>    GAMEPAD_BUTTON_PRESS  = DeviceButton.create();
    public static final Event<DeviceAxis>      GAMEPAD_AXIS_MOVE     = DeviceAxis.create();


    // -- event signatures --


    public interface Empty {
        void run();
        static Event<Empty> create() { return new Event<>(l -> () -> { for (Empty listener : l) listener.run(); }); }
    }

    public interface Render {
        void run(MatrixStack matrices, float tickDelta);
        static Event<Render> create() { return new Event<>(l -> (m, d) -> { for (Render listener : l) listener.run(m, d); }); }
    }

    public interface WindowMove {
        void run(int x, int y);
        static Event<WindowMove> create() { return new Event<>(l -> (x, y) -> { for (WindowMove listener : l) listener.run(x, y); }); }
    }

    public interface WindowResize {
        void run(int width, int height);
        static Event<WindowResize> create() { return new Event<>(l -> (w, h) -> { for (WindowResize listener : l) listener.run(w, h); }); }
    }

    public interface WindowFocused {
        void run(boolean focused);
        static Event<WindowFocused> create() { return new Event<>(l -> f -> { for (WindowFocused listener : l) listener.run(f); }); }
    }

    public interface FilesDropped {
        void run(String[] files);
        static Event<FilesDropped> create() { return new Event<>(l -> f -> { for (FilesDropped listener : l) listener.run(f); }); }
    }

    public interface KeyPress {
        void run(int key, int scancode, int action, int mods);
        static Event<KeyPress> create() { return new Event<>(l -> (k, s, a, m) -> { for (KeyPress listener : l) listener.run(k, s, a, m); }); }
    }

    public interface CharTyped {
        void run(char c, int mods);
        static Event<CharTyped> create() { return new Event<>(l -> (c, m) -> { for (CharTyped listener : l) listener.run(c, m); }); }
    }

    public interface MousePress {
        void run(int button, int action, int mods);
        static Event<MousePress> create() { return new Event<>(l -> (b, a, m) -> { for (MousePress listener : l) listener.run(b, a, m); }); }
    }

    //mouse move, scroll
    public interface Mouse2D {
        void run(double x, double y);
        static Event<Mouse2D> create() { return new Event<>(l -> (x, y) -> { for (Mouse2D listener : l) listener.run(x, y); }); }
    }

    //xr, joystick, gamepad Buttons
    public interface DeviceButton {
        void run(int button, boolean pressed, int deviceId);
        static Event<DeviceButton> create() { return new Event<>(l -> (b, p, d) -> { for (DeviceButton listener : l) listener.run(b, p, d); }); }
    }

    //xr triggers, joystick axes, and gamepad axes
    public interface DeviceAxis {
        void run(int index, float value, int deviceId, float lastValue);
        static Event<DeviceAxis> create() { return new Event<>(l -> (idx, v, d, lv) -> { for (DeviceAxis listener : l) listener.run(idx, v, d, lv); }); }
    }

    public interface XrJoystickMove {
        void run(float x, float y, int hand, float lastX, float lastY);
        static Event<XrJoystickMove> create() { return new Event<>(l -> (x, y, h, lx, ly) -> { for (XrJoystickMove listener : l) listener.run(x, y, h, lx, ly); }); }
    }

    public interface JoystickHatMove {
        void run(int hat, byte hatState, int joystick, byte lastValue);
        static Event<JoystickHatMove> create() { return new Event<>(l -> (h, hs, j, lv) -> { for (JoystickHatMove listener : l) listener.run(h, hs, j, lv); }); }
    }
}