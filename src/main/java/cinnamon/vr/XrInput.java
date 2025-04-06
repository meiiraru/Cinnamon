package cinnamon.vr;

import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static cinnamon.Client.LOGGER;
import static cinnamon.vr.XrManager.*;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class XrInput {

    private static final Map<String, XrKeybind> actions = new HashMap<>();

    static boolean init(MemoryStack stack) {
        LongBuffer profilePath = stringToPath(stack, "/interaction_profiles/oculus/touch_controller");
        LongBuffer leftHand =    stringToPath(stack, "/user/hand/left");
        LongBuffer rightHand =   stringToPath(stack, "/user/hand/right");

        actions.put("left_pose",     new XrKeybind("left_pose",     "/user/hand/left/input/grip/pose",      leftHand,  XR_ACTION_TYPE_POSE_INPUT,       stack));
        actions.put("right_pose",    new XrKeybind("right_pose",    "/user/hand/right/input/grip/pose",     rightHand, XR_ACTION_TYPE_POSE_INPUT,       stack));
        actions.put("left_click",    new XrKeybind("left_click",    "/user/hand/left/input/x/click",        leftHand,  XR_ACTION_TYPE_BOOLEAN_INPUT,    stack));
        actions.put("right_click",   new XrKeybind("right_click",   "/user/hand/right/input/a/click",       rightHand, XR_ACTION_TYPE_BOOLEAN_INPUT,    stack));
        actions.put("left_vibrate",  new XrKeybind("left_vibrate",  "/user/hand/left/output/haptic",        leftHand,  XR_ACTION_TYPE_VIBRATION_OUTPUT, stack));
        actions.put("right_vibrate", new XrKeybind("right_vibrate", "/user/hand/right/output/haptic",       rightHand, XR_ACTION_TYPE_VIBRATION_OUTPUT, stack));
        actions.put("left_squeeze",  new XrKeybind("left_squeeze",  "/user/hand/left/input/squeeze/value",  leftHand,  XR_ACTION_TYPE_FLOAT_INPUT,      stack));
        actions.put("right_squeeze", new XrKeybind("right_squeeze", "/user/hand/right/input/squeeze/value", rightHand, XR_ACTION_TYPE_FLOAT_INPUT,      stack));

        return suggestBindings(stack, profilePath);
    }

    static void free() {
        for (XrKeybind action : actions.values())
            action.free();
        actions.clear();
    }

    // -- init -- //

    static LongBuffer stringToPath(MemoryStack stack, String path) {
        LongBuffer buffer = stack.mallocLong(1);
        check(xrStringToPath(instance, path, buffer), "Failed to create path: error code %s");
        return buffer;
    }

    private static boolean suggestBindings(MemoryStack stack, LongBuffer profilePath) {
        XrActionSuggestedBinding.Buffer suggestedBindingsBuffer = XrActionSuggestedBinding.calloc(actions.size(), stack);
        int i = 0;
        for (XrKeybind action : actions.values()) {
            suggestedBindingsBuffer.get(i)
                    .action(action.action)
                    .binding(action.xrPath.get(0));
            i++;
        }

        XrInteractionProfileSuggestedBinding suggestedBindings = XrInteractionProfileSuggestedBinding.calloc(stack)
                .type$Default()
                .next(NULL)
                .interactionProfile(profilePath.get(0))
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
        XrRenderer.leftHandPose = actions.get("left_pose").getAsPose(stack);
        XrRenderer.rightHandPose = actions.get("right_pose").getAsPose(stack);

        boolean leftClick = actions.get("left_click").getAsBoolean(stack);
        if (leftClick) {
            actions.get("left_vibrate").vibrate(stack);
            System.out.println("left click " + displayTime);
        }

        boolean rightClick = actions.get("right_click").getAsBoolean(stack);
        if (rightClick) {
            actions.get("right_vibrate").vibrate(stack);
            System.out.println("right click " + displayTime);
        }

        float lSqueeze = actions.get("left_squeeze").getAsFloat(stack);
        if (lSqueeze >= 0.9f) {
            actions.get("left_vibrate").vibrate(stack);
            System.out.println("left SQUEEZE: " + lSqueeze);
        }
        else if (lSqueeze > 0f)
            System.out.println("left squeeze: " + lSqueeze);

        float rSqueeze = actions.get("right_squeeze").getAsFloat(stack);
        if (rSqueeze >= 0.9f) {
            actions.get("right_vibrate").vibrate(stack);
            System.out.println("right SQUEEZE: " + rSqueeze);
        }
        else if (rSqueeze > 0f)
            System.out.println("right squeeze: " + rSqueeze);
    }

    private static class XrKeybind {

        private final LongBuffer xrPath;
        private final long hand;
        private final XrAction action;
        private final XrSpace actionSpace;

        public XrKeybind(String name, String xrPath, LongBuffer hand, int actionType, MemoryStack stack) {
            this.xrPath = stringToPath(stack, xrPath);
            this.hand = hand.get(0);

            ByteBuffer nameBfr = stack.UTF8(name);
            XrActionCreateInfo rightSqueezeInfo = XrActionCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .actionName(nameBfr)
                    .localizedActionName(nameBfr)
                    .actionType(actionType)
                    .countSubactionPaths(1)
                    .subactionPaths(hand);

            PointerBuffer ptr = stack.mallocPointer(1);
            check(xrCreateAction(actionSet, rightSqueezeInfo, ptr), "Failed to create action: error code %s");
            this.action = new XrAction(ptr.get(0), actionSet);

            this.actionSpace = genActionSpace(stack, actionType);
        }

        void free() {
            xrDestroyAction(action);
            if (actionSpace != null)
                xrDestroySpace(actionSpace);
        }

        private XrSpace genActionSpace(MemoryStack stack, int actionType) {
            if (actionType != XR_ACTION_TYPE_POSE_INPUT)
                return null;

            PointerBuffer actionSpacePtr = stack.mallocPointer(1);
            XrActionSpaceCreateInfo actionSpaceInfo = XrActionSpaceCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .action(action)
                    .subactionPath(hand)
                    .poseInActionSpace(XrPosef.malloc(stack)
                            .position$(XrVector3f.calloc(stack)
                                    .set(0f, 0f, 0f))
                            .orientation(XrQuaternionf.calloc(stack)
                                    .set(0f, 0f, 0f, 1f)));

            check(xrCreateActionSpace(session, actionSpaceInfo, actionSpacePtr), "Failed to create action space: error code %s");
            return new XrSpace(actionSpacePtr.get(0), session);
        }

        public boolean getAsBoolean(MemoryStack stack) {
            XrActionStateBoolean actionState = XrActionStateBoolean.malloc(stack).type$Default().next(NULL);
            XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                    .type$Default()
                    .action(action)
                    .subactionPath(hand);

            if (check(xrGetActionStateBoolean(session, actionStateInfo, actionState), "Failed to get boolean action state: error code %s"))
                return false;

            return actionState.currentState();
        }

        public float getAsFloat(MemoryStack stack) {
            XrActionStateFloat actionState = XrActionStateFloat.malloc(stack).type$Default().next(NULL);
            XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                    .type$Default()
                    .action(action)
                    .subactionPath(hand);

            if (check(xrGetActionStateFloat(session, actionStateInfo, actionState), "Failed to get float action state: error code %s"))
                return 0f;

            return actionState.currentState();
        }

        public XrPosef getAsPose(MemoryStack stack) {
            XrActionStatePose actionState = XrActionStatePose.malloc(stack).type$Default().next(NULL);
            XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                    .type$Default()
                    .action(action)
                    .subactionPath(hand);

            if (check(xrGetActionStatePose(session, actionStateInfo, actionState), "Failed to get action state: error code %s"))
                return null;

            XrSpaceLocation actionSpaceLocation = XrSpaceLocation.malloc(stack).type$Default().next(NULL);
            if (check(xrLocateSpace(actionSpace, headspace, displayTime, actionSpaceLocation), "Failed to locate space: error code %s"))
                return null;

            if ((actionSpaceLocation.locationFlags() & XR_SPACE_LOCATION_POSITION_VALID_BIT) != 0 &&
                    (actionSpaceLocation.locationFlags() & XR_SPACE_LOCATION_ORIENTATION_VALID_BIT) != 0) {
                return actionSpaceLocation.pose();
            }

            return null;
        }

        public void vibrate(MemoryStack stack) {
            vibrate(stack, 0.5f, XR_MIN_HAPTIC_DURATION / 1_000_000);
        }

        public void vibrate(MemoryStack stack, float amplitude, long duration) {
            XrHapticVibration hapticVibration = XrHapticVibration.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .amplitude(amplitude)
                    .duration(duration * 1_000_000)
                    .frequency(XR_FREQUENCY_UNSPECIFIED);

            XrHapticActionInfo hapticActionInfo = XrHapticActionInfo.malloc(stack)
                    .type$Default()
                    .action(action)
                    .subactionPath(hand);

            check(xrApplyHapticFeedback(session, hapticActionInfo, XrHapticBaseHeader.create(hapticVibration)), "Failed to apply haptic feedback: error code %s");
        }
    }
}
