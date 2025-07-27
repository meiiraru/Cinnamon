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

import java.io.InputStream;
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

    public static final Resource PROFILE_ROOT = new Resource("data/xr_input/");
    private static final String DEFAULT_PROFILE = "khronos_simple_controller.json";

    private static final List<InteractionProfile> allProfiles = new ArrayList<>();

    private static InteractionProfile activeProfile;


    // -- init -- //


    static boolean init(MemoryStack stack) {
        //grab profiles
        List<String> profiles = IOUtils.listResources(PROFILE_ROOT, false);

        //load all profiles
        for (int i = 0, j = 1; i < profiles.size(); i++) {
            String profile = profiles.get(i);
            if (!profile.equals(DEFAULT_PROFILE))
                loadProfile(j++, PROFILE_ROOT.resolve(profile), stack);
        }
        loadProfile(0, PROFILE_ROOT.resolve(DEFAULT_PROFILE), stack);

        //try suggesting per profile
        for (InteractionProfile profile : allProfiles) {
            if (!suggestBindings(stack, profile)) {
                activeProfile = profile;

                //update the hand set
                XrRenderer.setHands(activeProfile.profiles.size());
                activeProfile.lastActiveHand = Math.min(1, activeProfile.profiles.size() - 1); //default to right hand

                LOGGER.info("Using xr input profile: %s", profile.name);
                return false; //success
            }
        }

        //failed to set a valid profile
        LOGGER.error("Failed to set a valid xr input profile, no profiles were loaded or all failed to suggest bindings");
        return true;
    }

    static void free() {
        for (InteractionProfile profile : allProfiles)
            profile.free();
    }

    static LongBuffer stringToPath(MemoryStack stack, String path) {
        LongBuffer buffer = stack.mallocLong(1);
        check(xrStringToPath(instance, path, buffer), "Failed to create path: error code %s");
        return buffer;
    }

    private static void loadProfile(int id, Resource profile, MemoryStack stack) {
        LOGGER.debug("Loading xr input profile \"%s\"", profile);

        InputStream stream = IOUtils.getResource(profile);
        if (stream == null) {
            LOGGER.error("Resource not found: %s", profile);
            return;
        }

        try (stream; InputStreamReader reader = new InputStreamReader(stream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            JsonObject userPaths = json.getAsJsonObject("user_paths");
            if (userPaths.isEmpty())
                throw new RuntimeException("no user_paths found");

            InteractionProfile interactionProfile = new InteractionProfile(json.get("path").getAsString(), json.get("name").getAsString());

            for (Map.Entry<String, JsonElement> userPathsEntry : userPaths.entrySet()) {
                JsonObject value = userPathsEntry.getValue().getAsJsonObject();
                UserProfile user = new UserProfile(id, userPathsEntry.getKey(), value.get("path").getAsString());
                interactionProfile.profiles.add(user);
                loadUserPath(stack, value, List.of(user), interactionProfile);
            }

            loadUserPath(stack, json.getAsJsonObject("all_user_paths"), interactionProfile.profiles, interactionProfile);
            allProfiles.add(interactionProfile);
        } catch (Exception e) {
            LOGGER.error("Failed to load profile \"%s\"", profile, e);
        }
    }

    private static void loadUserPath(MemoryStack stack, JsonObject src, List<UserProfile> profiles, InteractionProfile profile) {
        loadUserElement(src, profile, profiles, "haptics", (user, path) -> {
            XrKeybind.XrHapticsKeybind key = new XrKeybind.XrHapticsKeybind(stack, user, path, "haptics_" + user.haptics.size());
            user.haptics.add(key);
            return key;
        });
        loadUserElement(src, profile, profiles, "analogs", (user, path) -> {
            XrKeybind.XrVec2fKeybind key = new XrKeybind.XrVec2fKeybind(stack, user, path, "analog_" + user.analogs.size());
            user.analogs.add(key);
            return key;
        });
        loadUserElement(src, profile, profiles, "poses", (user, path) -> {
            XrKeybind.XrPoseKeybind key = new XrKeybind.XrPoseKeybind(stack, user, path, "pose_" + user.poses.size());
            user.poses.add(key);
            return key;
        });
        loadUserElement(src, profile, profiles, "buttons", (user, path) -> {
            XrKeybind.XrBooleanKeybind key = new XrKeybind.XrBooleanKeybind(stack, user, path, "button_" + user.buttons.size());
            user.buttons.add(key);
            return key;
        });
        loadUserElement(src, profile, profiles, "triggers", (user, path) -> {
            XrKeybind.XrFloatKeybind key = new XrKeybind.XrFloatKeybind(stack, user, path, "trigger_" + user.triggers.size());
            user.triggers.add(key);
            return key;
        });
    }

    private static void loadUserElement(JsonObject src, InteractionProfile profile, List<UserProfile> profiles, String name, BiFunction<UserProfile, String, XrKeybind<?>> keybindFactory) {
        if (src.has(name)) {
            for (JsonElement e : src.get(name).getAsJsonArray()) {
                String path = e.getAsString();
                for (UserProfile user : profiles)
                    profile.allButtons.add(keybindFactory.apply(user, path));
            }
        }
    }

    private static boolean suggestBindings(MemoryStack stack, InteractionProfile profile) {
        LOGGER.debug("Suggesting xr input bindings for profile \"%s\"", profile.name);
        XrActionSuggestedBinding.Buffer suggestedBindingsBuffer = XrActionSuggestedBinding.calloc(profile.allButtons.size(), stack);
        int i = 0;
        for (XrKeybind<?> action : profile.allButtons) {
            suggestedBindingsBuffer.get(i)
                    .action(action.action)
                    .binding(action.xrPath.get(0));
            i++;
        }

        XrInteractionProfileSuggestedBinding suggestedBindings = XrInteractionProfileSuggestedBinding.calloc(stack)
                .type$Default()
                .next(NULL)
                .interactionProfile(stringToPath(stack, profile.profilePath).get(0))
                .suggestedBindings(suggestedBindingsBuffer);

        int check = xrSuggestInteractionProfileBindings(instance, suggestedBindings);
        if (check != XR_SUCCESS) {
            LOGGER.debug("Failed to suggest interaction profile bindings for profile \"%s\", error code %s", profile.name, check);
            return true;
        }

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
        for (int hand = 0; hand < activeProfile.profiles.size(); hand++) {
            UserProfile profile = activeProfile.profiles.get(hand);

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
                    if (pressed) activeProfile.lastActiveHand = hand;
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
                    if (val >= 1f && val > lastVal) activeProfile.lastActiveHand = hand;
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

        List<XrKeybind.XrHapticsKeybind> haptics = activeProfile.profiles.get(hand).haptics;
        if (!haptics.isEmpty()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                for (XrKeybind.XrHapticsKeybind haptic : haptics)
                    //expected nanoseconds as duration, the argument uses milliseconds
                    haptic.vibrate(stack, amplitude, duration * 1_000_000);
            }
        }
    }

    public static int getActiveHand() {
        return activeProfile.lastActiveHand;
    }

    public static int getHandCount() {
        return activeProfile.profiles.size();
    }


    // -- profile classes -- //


    private static class InteractionProfile {
        private final List<XrKeybind<?>> allButtons = new ArrayList<>();
        private final List<UserProfile> profiles = new ArrayList<>();
        private final String profilePath, name;

        private int lastActiveHand = 0;

        public InteractionProfile(String profilePath, String name) {
            this.profilePath = profilePath;
            this.name = name;
        }

        public void free() {
            for (XrKeybind<?> action : allButtons)
                action.free();
        }
    }

    static class UserProfile {
        private final List<XrKeybind.XrHapticsKeybind> haptics = new ArrayList<>();
        private final List<XrKeybind.XrVec2fKeybind> analogs = new ArrayList<>();
        private final List<XrKeybind.XrPoseKeybind> poses = new ArrayList<>();
        private final List<XrKeybind.XrBooleanKeybind> buttons = new ArrayList<>();
        private final List<XrKeybind.XrFloatKeybind> triggers = new ArrayList<>();

        public final int id;
        public final String name;
        public final String path;
        public final LongBuffer pathBuffer;

        public UserProfile(int id, String name, String path) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.pathBuffer = stringToPath(MemoryStack.stackGet(), path);
        }
    }
}
