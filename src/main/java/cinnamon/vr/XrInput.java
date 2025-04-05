package cinnamon.vr;

import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;

import static cinnamon.vr.XrManager.*;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class XrInput {

    private static XrAction[] actions;
    private static XrSpace[] actionSpaces;
    private static LongBuffer leftHand, rightHand;

    static boolean init(MemoryStack stack) {
        LongBuffer oculusProfilePath = stack.mallocLong(1);
        if (check(xrStringToPath(instance, "/interaction_profiles/oculus/touch_controller", oculusProfilePath), "Failed to create path: error code %s"))
            return true;

        leftHand = stack.mallocLong(1);
        if (check(xrStringToPath(instance, "/user/hand/left", leftHand), "Failed to create path: error code %s"))
            return true;

        rightHand = stack.mallocLong(1);
        if (check(xrStringToPath(instance, "/user/hand/right", rightHand), "Failed to create path: error code %s"))
            return true;

        LongBuffer xClickPath = stack.mallocLong(1);
        if (check(xrStringToPath(instance, "/user/hand/left/input/x/click", xClickPath), "Failed to create path: error code %s"))
            return true;

        LongBuffer leftVibratePath = stack.mallocLong(1);
        if (check(xrStringToPath(instance, "/user/hand/left/output/haptic", leftVibratePath), "Failed to create path: error code %s"))
            return true;

        LongBuffer rightPosePath = stack.mallocLong(1);
        if (check(xrStringToPath(instance, "/user/hand/right/input/grip/pose", rightPosePath), "Failed to create path: error code %s"))
            return true;

        LongBuffer rightSqueezePath = stack.mallocLong(1);
        if (check(xrStringToPath(instance, "/user/hand/right/input/squeeze/value", rightSqueezePath), "Failed to create path: error code %s"))
            return true;


        XrActionCreateInfo createInfo = XrActionCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .actionName(stack.UTF8("test"))
                .localizedActionName(stack.UTF8("test"))
                .actionType(XR_ACTION_TYPE_BOOLEAN_INPUT)
                .countSubactionPaths(1)
                .subactionPaths(leftHand);

        PointerBuffer actionPtr = stack.mallocPointer(1);
        if (check(xrCreateAction(actionSet, createInfo, actionPtr), "Failed to create action: error code %s"))
            return true;

        XrAction action = new XrAction(actionPtr.get(), actionSet);


        XrActionCreateInfo vibrateInfo = XrActionCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .actionName(stack.UTF8("vibrate"))
                .localizedActionName(stack.UTF8("vibrate"))
                .actionType(XR_ACTION_TYPE_VIBRATION_OUTPUT)
                .countSubactionPaths(1)
                .subactionPaths(leftHand);

        PointerBuffer vibratePtr = stack.mallocPointer(1);
        if (check(xrCreateAction(actionSet, vibrateInfo, vibratePtr), "Failed to create action: error code %s"))
            return true;

        XrAction vibrateAction = new XrAction(vibratePtr.get(), actionSet);


        XrActionCreateInfo rightPoseInfo = XrActionCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .actionName(stack.UTF8("right_hand_pose"))
                .localizedActionName(stack.UTF8("right_hand_pose"))
                .actionType(XR_ACTION_TYPE_POSE_INPUT)
                .countSubactionPaths(1)
                .subactionPaths(rightHand);

        PointerBuffer rightPosePtr = stack.mallocPointer(1);
        if (check(xrCreateAction(actionSet, rightPoseInfo, rightPosePtr), "Failed to create action: error code %s"))
            return true;

        XrAction rightPoseAction = new XrAction(rightPosePtr.get(), actionSet);



        XrActionCreateInfo rightSqueezeInfo = XrActionCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .actionName(stack.UTF8("right_hand_squeeze"))
                .localizedActionName(stack.UTF8("right_hand_squeeze"))
                .actionType(XR_ACTION_TYPE_FLOAT_INPUT)
                .countSubactionPaths(1)
                .subactionPaths(rightHand);

        PointerBuffer rightSqueezePtr = stack.mallocPointer(1);
        if (check(xrCreateAction(actionSet, rightSqueezeInfo, rightSqueezePtr), "Failed to create action: error code %s"))
            return true;

        XrAction rightSqueezeAction = new XrAction(rightSqueezePtr.get(), actionSet);


        actions = new XrAction[]{action, vibrateAction, rightPoseAction, rightSqueezeAction};
        LongBuffer[] paths = new LongBuffer[]{xClickPath, leftVibratePath, rightPosePath, rightSqueezePath};

        XrActionSuggestedBinding.Buffer suggestedBindingsBuffer = XrActionSuggestedBinding.calloc(actions.length, stack);
        for (int i = 0; i < actions.length; i++) {
            suggestedBindingsBuffer.get(i)
                    .action(actions[i])
                    .binding(paths[i].get(0));
        }

        XrInteractionProfileSuggestedBinding suggestedBindings = XrInteractionProfileSuggestedBinding.calloc(stack)
                .type$Default()
                .next(NULL)
                .interactionProfile(oculusProfilePath.get())
                .suggestedBindings(suggestedBindingsBuffer);

        if (check(xrSuggestInteractionProfileBindings(instance, suggestedBindings), "Failed to suggest interaction profile bindings: error code %s"))
            return true;


        PointerBuffer actionSpacePtr = stack.mallocPointer(1);
        XrActionSpaceCreateInfo actionSpaceInfo = XrActionSpaceCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .action(rightPoseAction)
                .subactionPath(rightHand.get(0))
                .poseInActionSpace(XrPosef.calloc(stack)
                        .position$(XrVector3f.calloc(stack)
                                .set(0f, 0f, 0f))
                        .orientation(XrQuaternionf.calloc(stack)
                                .set(0f, 0f, 0f, 1f)));
        if (check(xrCreateActionSpace(session, actionSpaceInfo, actionSpacePtr), "Failed to create action space: error code %s"))
            return true;
        actionSpaces = new XrSpace[]{new XrSpace(actionSpacePtr.get(), session)};

        return false;
    }

    static void free() {
        if (actions != null) for (XrAction action : actions) xrDestroyAction(action);
        if (actionSpaces != null) for (XrSpace actionSpace : actionSpaces) xrDestroySpace(actionSpace);
    }

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

    // -- init -- //

    private static void processInput(MemoryStack stack) {
        XrRenderer.rightHandPose = getActionPose(stack, rightHand, actions[2], actionSpaces[0]);
        boolean leftButton = getActionBoolean(stack, leftHand, actions[0]);
        if (leftButton) {
            sendHapticsFeedback(stack, leftHand, actions[1]);
            System.out.println("left button " + displayTime);
        }

        float squeezeValue = getActionFloat(stack, rightHand, actions[3]);
        if (squeezeValue >= 0.9f)
            System.out.println("SQUEEZE: " + squeezeValue);
        else if (squeezeValue > 0f)
            System.out.println("squeeze: " + squeezeValue);
    }

    private static boolean getActionBoolean(MemoryStack stack, LongBuffer hand, XrAction action) {
        XrActionStateBoolean actionState = XrActionStateBoolean.malloc(stack).type$Default().next(NULL);
        XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                .type$Default()
                .action(action)
                .subactionPath(hand.get(0));

        if (check(xrGetActionStateBoolean(session, actionStateInfo, actionState), "Failed to get boolean action state: error code %s"))
            return false;

        return actionState.currentState();
    }

    private static float getActionFloat(MemoryStack stack, LongBuffer hand, XrAction action) {
        XrActionStateFloat actionState = XrActionStateFloat.malloc(stack).type$Default().next(NULL);
        XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                .type$Default()
                .action(action)
                .subactionPath(hand.get(0));

        if (check(xrGetActionStateFloat(session, actionStateInfo, actionState), "Failed to get action state: error code %s"))
            return 0f;

        return actionState.currentState();
    }

    private static XrPosef getActionPose(MemoryStack stack, LongBuffer hand, XrAction action, XrSpace space) {
        XrActionStatePose actionState = XrActionStatePose.malloc(stack).type$Default().next(NULL);
        XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                .type$Default()
                .action(action)
                .subactionPath(hand.get(0));

        if (check(xrGetActionStatePose(session, actionStateInfo, actionState), "Failed to get action state: error code %s"))
            return null;

        XrSpaceLocation actionSpaceLocation = XrSpaceLocation.malloc(stack).type$Default().next(NULL);
        if (check(xrLocateSpace(space, headspace, displayTime, actionSpaceLocation), "Failed to locate space: error code %s"))
            return null;

        if ((actionSpaceLocation.locationFlags() & XR_SPACE_LOCATION_POSITION_VALID_BIT) != 0 &&
                (actionSpaceLocation.locationFlags() & XR_SPACE_LOCATION_ORIENTATION_VALID_BIT) != 0) {
            return actionSpaceLocation.pose();
        }

        return null;
    }

    private static void sendHapticsFeedback(MemoryStack stack, LongBuffer hand, XrAction action) {
        XrHapticVibration hapticVibration = XrHapticVibration.malloc(stack)
                .type$Default()
                .next(NULL)
                .amplitude(0.5f)
                .duration(1_000_000_000L) //1s
                .frequency(XR_FREQUENCY_UNSPECIFIED);

        XrHapticActionInfo hapticActionInfo = XrHapticActionInfo.malloc(stack)
                .type$Default()
                .action(action)
                .subactionPath(hand.get(0));

        check(xrApplyHapticFeedback(session, hapticActionInfo, XrHapticBaseHeader.create(hapticVibration)), "Failed to apply haptic feedback: error code %s");
    }
}
