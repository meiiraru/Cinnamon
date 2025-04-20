package cinnamon.vr;

import cinnamon.utils.Pair;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static cinnamon.vr.XrInput.stringToPath;
import static cinnamon.vr.XrManager.*;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

abstract class XrKeybind<T> {

    protected final LongBuffer xrPath;
    protected final long user;
    protected final XrAction action;

    protected T value, lastVal;
    protected boolean hasChanges;

    public XrKeybind(MemoryStack stack, XrInput.UserProfile user, String path, String name, int actionType) {
        this.xrPath = stringToPath(stack, user.path + path);
        this.user = user.pathBuffer.get(0);

        ByteBuffer nameBfr = stack.UTF8(user.name + "_" + name);
        XrActionCreateInfo rightSqueezeInfo = XrActionCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .actionName(nameBfr)
                .localizedActionName(nameBfr)
                .actionType(actionType)
                .countSubactionPaths(1)
                .subactionPaths(user.pathBuffer);

        PointerBuffer ptr = stack.mallocPointer(1);
        check(xrCreateAction(actionSet, rightSqueezeInfo, ptr), "Failed to create action: error code %s");
        this.action = new XrAction(ptr.get(0), actionSet);
    }

    public void free() {
        xrDestroyAction(action);
    }

    public abstract void poll(MemoryStack stack);

    public T getValue() {
        return value;
    }

    public T getLastVal() {
        return lastVal;
    }

    public boolean hasChanges() {
        return hasChanges;
    }

    // -- types -- //

    static class XrBooleanKeybind extends XrKeybind<Boolean> {

        public XrBooleanKeybind(MemoryStack stack, XrInput.UserProfile user, String path, String name) {
            super(stack, user, path, name, XR_ACTION_TYPE_BOOLEAN_INPUT);
            lastVal = false;
        }

        @Override
        public void poll(MemoryStack stack) {
            XrActionStateBoolean actionState = XrActionStateBoolean.malloc(stack).type$Default().next(NULL);
            XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                    .type$Default()
                    .action(action)
                    .subactionPath(user);

            if (check(xrGetActionStateBoolean(session, actionStateInfo, actionState), "Failed to get boolean action state: error code %s")) {
                hasChanges = false;
                return;
            }

            hasChanges = actionState.isActive() && actionState.changedSinceLastSync();
            lastVal = value;
            value = actionState.currentState();
        }
    }

    static class XrFloatKeybind extends XrKeybind<Float> {

        public XrFloatKeybind(MemoryStack stack, XrInput.UserProfile user, String path, String name) {
            super(stack, user, path, name, XR_ACTION_TYPE_FLOAT_INPUT);
            lastVal = 0f;
        }

        @Override
        public void poll(MemoryStack stack) {
            XrActionStateFloat actionState = XrActionStateFloat.malloc(stack).type$Default().next(NULL);
            XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                    .type$Default()
                    .action(action)
                    .subactionPath(user);

            if (check(xrGetActionStateFloat(session, actionStateInfo, actionState), "Failed to get float action state: error code %s")) {
                hasChanges = false;
                return;
            }

            hasChanges = actionState.isActive() && actionState.changedSinceLastSync();
            lastVal = value;
            value = actionState.currentState();
        }
    }

    static class XrVec2fKeybind extends XrKeybind<Vector2f> {

        public XrVec2fKeybind(MemoryStack stack, XrInput.UserProfile user, String path, String name) {
            super(stack, user, path, name, XR_ACTION_TYPE_VECTOR2F_INPUT);
            value = new Vector2f();
            lastVal = new Vector2f();
        }

        @Override
        public void poll(MemoryStack stack) {
            XrActionStateVector2f actionState = XrActionStateVector2f.malloc(stack).type$Default().next(NULL);
            XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                    .type$Default()
                    .action(action)
                    .subactionPath(user);

            if (check(xrGetActionStateVector2f(session, actionStateInfo, actionState), "Failed to get vector2f action state: error code %s")) {
                hasChanges = false;
                return;
            }

            hasChanges = actionState.isActive() && actionState.changedSinceLastSync();
            lastVal.set(value);
            XrVector2f vec = actionState.currentState();
            value.set(vec.x(), vec.y());
        }
    }

    static class XrPoseKeybind extends XrKeybind<Pair<Vector3f, Quaternionf>> {

        private final XrSpace actionSpace;

        public XrPoseKeybind(MemoryStack stack, XrInput.UserProfile user, String path, String name) {
            super(stack, user, path, name, XR_ACTION_TYPE_POSE_INPUT);
            value = new Pair<>(new Vector3f(), new Quaternionf());

            PointerBuffer actionSpacePtr = stack.mallocPointer(1);
            XrActionSpaceCreateInfo actionSpaceInfo = XrActionSpaceCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .action(action)
                    .subactionPath(this.user)
                    .poseInActionSpace(XrPosef.malloc(stack)
                            .position$(XrVector3f.calloc(stack)
                                    .set(0f, 0f, 0f))
                            .orientation(XrQuaternionf.calloc(stack)
                                    .set(0f, 0f, 0f, 1f)));

            check(xrCreateActionSpace(session, actionSpaceInfo, actionSpacePtr), "Failed to create action space: error code %s");
            actionSpace = new XrSpace(actionSpacePtr.get(0), session);
        }

        @Override
        public void free() {
            super.free();
            if (actionSpace != null)
                xrDestroySpace(actionSpace);
        }

        @Override
        public void poll(MemoryStack stack) {
            XrActionStatePose actionState = XrActionStatePose.malloc(stack).type$Default().next(NULL);
            XrActionStateGetInfo actionStateInfo = XrActionStateGetInfo.malloc(stack)
                    .type$Default()
                    .action(action)
                    .subactionPath(user);

            XrSpaceLocation actionSpaceLocation = XrSpaceLocation.malloc(stack).type$Default().next(NULL);

            if (check(xrGetActionStatePose(session, actionStateInfo, actionState), "Failed to get pose action state: error code %s") ||
                    check(xrLocateSpace(actionSpace, headspace, displayTime, actionSpaceLocation), "Failed to locate space: error code %s") ||
                    (actionSpaceLocation.locationFlags() & XR_SPACE_LOCATION_POSITION_VALID_BIT) == 0 ||
                    (actionSpaceLocation.locationFlags() & XR_SPACE_LOCATION_ORIENTATION_VALID_BIT) == 0
            ) {
                hasChanges = false;
                return;
            }

            hasChanges = actionState.isActive();

            XrPosef pose = actionSpaceLocation.pose();
            XrVector3f pos = pose.position$();
            XrQuaternionf rot = pose.orientation();

            value.first().set(pos.x(), pos.y(), pos.z());
            value.second().set(rot.x(), rot.y(), rot.z(), rot.w());
        }
    }

    static class XrHapticsKeybind extends XrKeybind<Void> {

        public XrHapticsKeybind(MemoryStack stack, XrInput.UserProfile user, String path, String name) {
            super(stack, user, path, name, XR_ACTION_TYPE_VIBRATION_OUTPUT);
        }

        @Override
        public void poll(MemoryStack stack) {}

        public void vibrate(MemoryStack stack, float amplitude, long duration) {
            XrHapticVibration hapticVibration = XrHapticVibration.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .amplitude(amplitude)
                    .duration(duration)
                    .frequency(XR_FREQUENCY_UNSPECIFIED);

            XrHapticActionInfo hapticActionInfo = XrHapticActionInfo.malloc(stack)
                    .type$Default()
                    .action(action)
                    .subactionPath(user);

            check(xrApplyHapticFeedback(session, hapticActionInfo, XrHapticBaseHeader.create(hapticVibration)), "Failed to apply haptic feedback: error code %s");
        }
    }
}
