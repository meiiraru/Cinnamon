package cinnamon.vr;

import cinnamon.Client;
import cinnamon.settings.Settings;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Vector2f;
import org.lwjgl.openxr.XrActionSuggestedBinding;
import org.lwjgl.openxr.XrActionsSyncInfo;
import org.lwjgl.openxr.XrActiveActionSet;
import org.lwjgl.openxr.XrInteractionProfileSuggestedBinding;
import org.lwjgl.system.MemoryStack;

import java.io.InputStreamReader;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static cinnamon.vr.XrManager.*;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class XrInput {

    public static final Resource DEFAULT_PROFILE = new Resource("data/xr_input/khronos_simple_controller.json");

    private static final List<XrKeybind<?>> allButtons = new ArrayList<>();
    private static final List<UserProfile> profiles = new ArrayList<>();
    private static String profilePath;
    private static int lastActiveHand = 0;


    // -- init -- //


    static boolean init(MemoryStack stack) {
        return loadProfile(stack) || suggestBindings(stack);
    }

    static void free() {
        for (XrKeybind<?> action : allButtons)
            action.free();
        allButtons.clear();
        for (UserProfile profile : profiles)
            profile.clear();
        profilePath = null;
    }

    static LongBuffer stringToPath(MemoryStack stack, String path) {
        LongBuffer buffer = stack.mallocLong(1);
        check(xrStringToPath(instance, path, buffer), "Failed to create path: error code %s");
        return buffer;
    }

    private static boolean loadProfile(MemoryStack stack) {
        Resource profile = new Resource(Settings.xrInteractionProfile.get());
        if (!IOUtils.hasResource(profile))
            profile = DEFAULT_PROFILE;

        try {
            free();
            JsonObject json = JsonParser.parseReader(new InputStreamReader(IOUtils.getResource(profile))).getAsJsonObject();

            JsonObject userPaths = json.getAsJsonObject("user_paths");
            if (userPaths.isEmpty())
                throw new RuntimeException("no user_paths found");

            profilePath = json.get("path").getAsString();

            for (Map.Entry<String, JsonElement> userPathsEntry : userPaths.entrySet()) {
                JsonObject value = userPathsEntry.getValue().getAsJsonObject();
                UserProfile user = new UserProfile(userPathsEntry.getKey(), value.get("path").getAsString());
                profiles.add(user);
                loadUserPath(stack, value, List.of(user));
            }

            loadUserPath(stack, json.getAsJsonObject("all_user_paths"), profiles);
            XrRenderer.setHands(profiles.size());
            lastActiveHand = Math.min(1, profiles.size() - 1); //default to right hand

            LOGGER.info("Loaded xr input profile: %s", profile);
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to load profile: %s", profile, e);
            return true;
        }
    }

    private static void loadUserPath(MemoryStack stack, JsonObject src, List<UserProfile> profiles) {
        loadUserElement(src, profiles, "haptics", (user, path) -> {
            XrKeybind.XrHapticsKeybind key = new XrKeybind.XrHapticsKeybind(stack, user, path, "haptics_" + user.haptics.size());
            user.haptics.add(key);
            return key;
        });
        loadUserElement(src, profiles, "analogs", (user, path) -> {
            XrKeybind.XrVec2fKeybind key = new XrKeybind.XrVec2fKeybind(stack, user, path, "analog_" + user.analogs.size());
            user.analogs.add(key);
            return key;
        });
        loadUserElement(src, profiles, "poses", (user, path) -> {
            XrKeybind.XrPoseKeybind key = new XrKeybind.XrPoseKeybind(stack, user, path, "pose_" + user.poses.size());
            user.poses.add(key);
            return key;
        });
        loadUserElement(src, profiles, "buttons", (user, path) -> {
            XrKeybind.XrBooleanKeybind key = new XrKeybind.XrBooleanKeybind(stack, user, path, "button_" + user.buttons.size());
            user.buttons.add(key);
            return key;
        });
        loadUserElement(src, profiles, "triggers", (user, path) -> {
            XrKeybind.XrFloatKeybind key = new XrKeybind.XrFloatKeybind(stack, user, path, "trigger_" + user.triggers.size());
            user.triggers.add(key);
            return key;
        });
    }

    private static void loadUserElement(JsonObject src, List<UserProfile> profiles, String name, BiFunction<UserProfile, String, XrKeybind<?>> keybindFactory) {
        if (src.has(name)) {
            for (JsonElement e : src.get(name).getAsJsonArray()) {
                String path = e.getAsString();
                for (UserProfile user : profiles)
                    allButtons.add(keybindFactory.apply(user, path));
            }
        }
    }

    private static boolean suggestBindings(MemoryStack stack) {
        XrActionSuggestedBinding.Buffer suggestedBindingsBuffer = XrActionSuggestedBinding.calloc(allButtons.size(), stack);
        int i = 0;
        for (XrKeybind<?> action : allButtons) {
            suggestedBindingsBuffer.get(i)
                    .action(action.action)
                    .binding(action.xrPath.get(0));
            i++;
        }

        XrInteractionProfileSuggestedBinding suggestedBindings = XrInteractionProfileSuggestedBinding.calloc(stack)
                .type$Default()
                .next(NULL)
                .interactionProfile(stringToPath(stack, profilePath).get(0))
                .suggestedBindings(suggestedBindingsBuffer);

        if (check(xrSuggestInteractionProfileBindings(instance, suggestedBindings), "Failed to suggest interaction profile bindings: error code %s"))
            return true;

        LOGGER.debug("Set suggested xr bindings");
        return false;
    }


    // -- poll -- //


    static boolean poll(MemoryStack stack) {
        //sync actions
        XrActiveActionSet.Buffer actionSetBuffer = XrActiveActionSet.calloc(1, stack)
                .actionSet(actionSet)
                .subactionPath(XR_NULL_PATH);

        XrActionsSyncInfo syncInfo = XrActionsSyncInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .countActiveActionSets(1)
                .activeActionSets(actionSetBuffer);

        int result = xrSyncActions(session, syncInfo);
        if (result == XR_SESSION_NOT_FOCUSED)
            return false;

        if (check(result, "Failed to sync actions: error code %s"))
            return true;

        //process input
        processInput(stack);

        return false;
    }

    private static void processInput(MemoryStack stack) {
        Client c = Client.getInstance();
        for (int hand = 0; hand < profiles.size(); hand++) {
            UserProfile profile = profiles.get(hand);

            //poses
            if (!profile.poses.isEmpty()) {
                XrKeybind.XrPoseKeybind pose = profile.poses.getFirst();
                pose.poll(stack);
                if (pose.hasChanges()) XrRenderer.updateHand(hand, pose.getValue());
            }

            //analogs
            if (!profile.analogs.isEmpty()) {
                XrKeybind.XrVec2fKeybind keybind = profile.analogs.getFirst();
                keybind.poll(stack);
                if (keybind.hasChanges()) {
                    Vector2f vec = keybind.getValue();
                    Vector2f old = keybind.getLastVal();
                    c.xrJoystickMove(vec.x(), vec.y(), hand, old.x(), old.y());
                }
            }

            //buttons
            for (int button = 0; button < profile.buttons.size(); button++) {
                XrKeybind.XrBooleanKeybind keybind = profile.buttons.get(button);
                keybind.poll(stack);
                if (keybind.hasChanges()) {
                    boolean pressed = keybind.getValue();
                    if (pressed) lastActiveHand = hand;
                    c.xrButtonPress(button, pressed, hand);
                }
            }

            //triggers
            for (int button = 0; button < profile.triggers.size(); button++) {
                XrKeybind.XrFloatKeybind keybind = profile.triggers.get(button);
                keybind.poll(stack);
                if (keybind.hasChanges()) {
                    float lastVal = keybind.getLastVal();
                    float val = keybind.getValue();
                    if (val >= 1f && val > lastVal) lastActiveHand = hand;
                    c.xrTriggerPress(button, val, hand, lastVal);
                }
            }
        }

        XrRenderer.updateScreenCollision();
    }


    // -- functions -- //


    public static void vibrate(int hand) {
        vibrate(hand, 0.1f, XR_MIN_HAPTIC_DURATION / 1_000_000);
    }

    public static void vibrate(int hand, float amplitude, long duration) {
        if (!Settings.xrHapticFeedback.get())
            return;

        List<XrKeybind.XrHapticsKeybind> haptics = profiles.get(hand).haptics;
        if (!haptics.isEmpty()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                for (XrKeybind.XrHapticsKeybind haptic : haptics)
                    //expected nanoseconds as duration, the argument uses milliseconds
                    haptic.vibrate(stack, amplitude, duration * 1_000_000);
            }
        }
    }

    public static int getActiveHand() {
        return lastActiveHand;
    }

    public static int getHandCount() {
        return profiles.size();
    }


    // -- profile class -- //


    static class UserProfile {
        private final List<XrKeybind.XrHapticsKeybind> haptics = new ArrayList<>();
        private final List<XrKeybind.XrVec2fKeybind> analogs = new ArrayList<>();
        private final List<XrKeybind.XrPoseKeybind> poses = new ArrayList<>();
        private final List<XrKeybind.XrBooleanKeybind> buttons = new ArrayList<>();
        private final List<XrKeybind.XrFloatKeybind> triggers = new ArrayList<>();

        public final String name;
        public final String path;
        public final LongBuffer pathBuffer;

        public UserProfile(String name, String path) {
            this.name = name;
            this.path = path;
            this.pathBuffer = stringToPath(MemoryStack.stackGet(), path);
        }

        public void clear() {
            haptics.clear();
            analogs.clear();
            poses.clear();
            buttons.clear();
            triggers.clear();
        }
    }
}
