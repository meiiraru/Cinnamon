package cinnamon.vr;

import cinnamon.Cinnamon;
import cinnamon.Client;
import cinnamon.logger.Logger;
import cinnamon.utils.Pair;
import org.lwjgl.PointerBuffer;
import org.lwjgl.egl.EGL;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.system.linux.XVisualInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;
import static org.lwjgl.glfw.GLFWNativeEGL.*;
import static org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXContext;
import static org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXFBConfig;
import static org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext;
import static org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window;
import static org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display;
import static org.lwjgl.opengl.GL21.GL_SRGB8;
import static org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GLX.glXGetCurrentDrawable;
import static org.lwjgl.opengl.GLX13.glXGetVisualFromFBConfig;
import static org.lwjgl.openxr.EXTDebugUtils.*;
import static org.lwjgl.openxr.KHROpenGLEnable.*;
import static org.lwjgl.openxr.MNDXEGLEnable.XR_MNDX_EGL_ENABLE_EXTENSION_NAME;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.windows.User32.GetDC;

public class XrManager {

    static Logger LOGGER = new Logger("xr");

    //instance
    static XrInstance instance;
    private static long systemId;
    static XrSession session;
    static XrSpace headspace;

    //session
    private static boolean missingXrDebug, useEgl;
    private static XrDebugUtilsMessengerEXT debugMessenger;
    private static XrEventDataBuffer eventDataBuffer;
    private static int sessionState;

    //input
    static XrActionSet actionSet;

    //render
    private static XrView.Buffer views;
    private static final int viewConfigType = XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO;
    private static XrViewConfigurationView.Buffer viewConfigs;
    static Swapchain[] swapchains;
    static long displayTime;

    private static boolean initialized, sessionRunning;

    public static void init() {
        if (initialized)
            return;

        try (MemoryStack stack = stackPush()) {
            if (initializeInstance(stack) || initializeSystem(stack) || initializeSession(stack) || initializeSwapChains(stack) || initializeReferenceSpace(stack) || initInput(stack)) {
                internalClose();
                return;
            }
            eventDataBuffer = XrEventDataBuffer.calloc().type$Default();
            initialized = true;
        }
    }

    public static void close() {
        if (initialized)
            internalClose();
    }

    private static void internalClose() {
        XrInput.free();
        if (actionSet != null) xrDestroyActionSet(actionSet);
        if (eventDataBuffer != null) eventDataBuffer.free();
        if (views != null) views.free();
        if (viewConfigs != null) viewConfigs.free();
        if (swapchains != null) for (Swapchain swapchain : swapchains) swapchain.destroy();
        if (headspace != null) xrDestroySpace(headspace);
        if (debugMessenger != null) xrDestroyDebugUtilsMessengerEXT(debugMessenger);
        if (session != null) xrDestroySession(session);
        if (instance != null) xrDestroyInstance(instance);
        XrRenderer.free();
        initialized = false;
        setSessionRunning(false);
    }

    public static boolean isInXR() {
        return sessionRunning;
    }

    private static void setSessionRunning(boolean bool) {
        if (sessionRunning == bool)
            return;

        sessionRunning = bool;
        Client c = Client.getInstance();
        c.windowResize(c.window.width, c.window.height);
    }

    public static boolean render(Runnable toRender) {
        if (!initialized || pollEvents() || !sessionRunning)
            return false;

        try (MemoryStack stack = stackPush()) {
            if (XrInput.poll(stack))
                return false;

            return render(stack, toRender);
        }
    }

    static boolean check(int error, String msg) {
        if (error == XR_SUCCESS)
            return false;

        LOGGER.error(msg, error);
        return true;
    }

    static <S extends Struct<S>, T extends StructBuffer<S, T>> T fill(T buffer, int offset, int value) {
        long ptr = buffer.address() + offset;
        int stride = buffer.sizeof();
        for (int i = 0; i < buffer.limit(); i++)
            memPutInt(ptr + (long) i * stride, value);
        return buffer;
    }

    // -- init functions -- //

    private static boolean initializeInstance(MemoryStack stack) {
        //extensions
        Pair<PointerBuffer, PointerBuffer> extensions = grabExtensionsAndLayers(stack);
        if (extensions == null)
            return true;

        //setup xr instance
        XrInstanceCreateInfo instanceInfo = XrInstanceCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .createFlags(0)
                .applicationInfo(XrApplicationInfo.calloc(stack)
                        .applicationName(stack.UTF8(Cinnamon.TITLE))
                        .apiVersion(XR_API_VERSION_1_0))
                .enabledApiLayerNames(extensions.second())
                .enabledExtensionNames(extensions.first());

        //create xr instance
        PointerBuffer instancePtr = stack.mallocPointer(1);
        if (check(xrCreateInstance(instanceInfo, instancePtr), "Failed to create OpenXR instance: error code %s"))
            return true;

        LOGGER.debug("Created OpenXR instance");
        LOGGER.info("OpenXR version: %s", XR_VERSION_MAJOR(XR_API_VERSION_1_0) + "." + XR_VERSION_MINOR(XR_API_VERSION_1_0) + "." + XR_VERSION_PATCH(XR_API_VERSION_1_0));
        instance = new XrInstance(instancePtr.get(0), instanceInfo);
        return false;
    }

    private static Pair<PointerBuffer, PointerBuffer> grabExtensionsAndLayers(MemoryStack stack) {
        //extensions
        IntBuffer pi = stack.mallocInt(1);
        if (check(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, pi, null), "Failed to get OpenXR instance extension count: error code %s"))
            return null;

        int numExtensions = pi.get(0);
        XrExtensionProperties.Buffer properties = fill(
                XrExtensionProperties.calloc(numExtensions, stack),
                XrExtensionProperties.TYPE,
                XR_TYPE_EXTENSION_PROPERTIES
        );
        if (check(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, pi, properties), "Failed to get OpenXR instance extensions: error code %s"))
            return null;

        LOGGER.debug("OpenXR loaded with %s extensions", numExtensions);

        boolean missingOpenGL = true;
        missingXrDebug = true;
        useEgl = false;

        for (int i = 0; i < numExtensions; i++) {
            String extensionName = properties.get(i).extensionNameString();
            LOGGER.debug(extensionName);
            switch (extensionName) {
                case XR_KHR_OPENGL_ENABLE_EXTENSION_NAME -> missingOpenGL = false;
                case XR_EXT_DEBUG_UTILS_EXTENSION_NAME -> missingXrDebug = false;
                case XR_MNDX_EGL_ENABLE_EXTENSION_NAME -> useEgl = true;
            }
        }

        if (missingOpenGL) {
            LOGGER.error("Missing required extension: %s", XR_KHR_OPENGL_ENABLE_EXTENSION_NAME);
            return null;
        }

        PointerBuffer extensions = stack.mallocPointer(3);
        extensions.put(stack.UTF8(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME));
        if (useEgl) extensions.put(stack.UTF8(XR_MNDX_EGL_ENABLE_EXTENSION_NAME));
        else if (!missingXrDebug) extensions.put(stack.UTF8(XR_EXT_DEBUG_UTILS_EXTENSION_NAME));
        extensions.flip();

        //layers
        boolean useValidationLayer = false;

        if (check(xrEnumerateApiLayerProperties(pi, null), "Failed to get OpenXR layer count: error code %s"))
            return null;

        int numLayers = pi.get(0);
        XrApiLayerProperties.Buffer pLayers = fill(
                XrApiLayerProperties.calloc(numLayers, stack),
                XrApiLayerProperties.TYPE,
                XR_TYPE_API_LAYER_PROPERTIES
        );
        if (check(xrEnumerateApiLayerProperties(pi, pLayers), "Failed to get OpenXR layers: error code %s"))
            return null;

        LOGGER.debug("OpenXR loaded with %s layers", numLayers);
        for (int i = 0; i < numLayers; i++) {
            String layerName = pLayers.get(i).layerNameString();
            LOGGER.debug(layerName);
            if (!useEgl && layerName.equals("XR_APILAYER_LUNARG_core_validation"))
                useValidationLayer = true;
        }

        PointerBuffer layers;
        if (useValidationLayer) {
            layers = stack.callocPointer(1);
            layers.put(0, stack.UTF8("XR_APILAYER_LUNARG_core_validation"));
            LOGGER.debug("Enabling XR core validation");
        } else {
            layers = null;
            LOGGER.debug("Running without validation layers");
        }

        return new Pair<>(extensions, layers);
    }

    private static boolean initializeSystem(MemoryStack stack) {
        LongBuffer systemIdBuffer = stack.longs(0);
        XrSystemGetInfo systemInfo = XrSystemGetInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .formFactor(XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY);

        if (check(xrGetSystem(instance, systemInfo, systemIdBuffer), "Failed to get OpenXR system: error code %s"))
            return true;

        systemId = systemIdBuffer.get(0);
        if (systemId == 0) {
            LOGGER.error("No compatible headset detected");
            return true;
        }

        LOGGER.debug("Found headset with system id %s", systemId);
        return false;
    }

    private static boolean initializeSession(MemoryStack stack) {
        //open gl compatibility
        XrGraphicsRequirementsOpenGLKHR graphicsRequirements = XrGraphicsRequirementsOpenGLKHR.malloc(stack)
                .type$Default()
                .next(NULL)
                .minApiVersionSupported(0)
                .maxApiVersionSupported(0);

        xrGetOpenGLGraphicsRequirementsKHR(instance, systemId, graphicsRequirements);

        //create session
        XrSessionCreateInfo sessionInfo = XrSessionCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .createFlags(0)
                .systemId(systemId);
        if (createGraphicsBindingOpenGL(stack, Client.getInstance().window.getHandle(), sessionInfo, useEgl)) {
            LOGGER.error("Failed to create graphics binding");
            return true;
        }

        PointerBuffer sessionPtr = stack.mallocPointer(1);
        if (check(xrCreateSession(instance, sessionInfo, sessionPtr), "Failed to create OpenXR session: error code %s"))
            return true;

        LOGGER.debug("Created OpenXR session");
        session = new XrSession(sessionPtr.get(0), instance);

        if (missingXrDebug || useEgl)
            return false;

        //use debug utils when available
        XrDebugUtilsMessengerCreateInfoEXT ciDebugUtils = XrDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .type$Default()
                .messageSeverities(XR_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                .messageTypes(XR_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_TYPE_CONFORMANCE_BIT_EXT)
                .userCallback((messageSeverity, messageTypes, pCallbackData, userData) -> {
                    XrDebugUtilsMessengerCallbackDataEXT callbackData = XrDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                    LOGGER.debug("XR Debug Utils: %s", callbackData.messageString());
                    return 0;
                });

        if (check(xrCreateDebugUtilsMessengerEXT(instance, ciDebugUtils, sessionPtr), "Failed to create debug utils messenger: error code %s"))
            return true;

        LOGGER.debug("Enabling OpenXR debug utils");
        debugMessenger = new XrDebugUtilsMessengerEXT(sessionPtr.get(0), instance);

        return false;
    }

    private static boolean initializeSwapChains(MemoryStack stack) {
        //headset info
        XrSystemProperties systemProperties = XrSystemProperties.calloc(stack).type$Default();
        if (check(xrGetSystemProperties(instance, systemId, systemProperties), "Failed to get system properties: error code %s"))
            return true;

        LOGGER.info("Headset %s", memUTF8(memAddress(systemProperties.systemName())));
        LOGGER.debug("Vendor %s", systemProperties.vendorId());

        XrSystemTrackingProperties trackingProperties = systemProperties.trackingProperties();
        LOGGER.debug("Orientation tracking: %s", trackingProperties.orientationTracking());
        LOGGER.debug("Position tracking: %s", trackingProperties.positionTracking());

        XrSystemGraphicsProperties graphicsProperties = systemProperties.graphicsProperties();
        LOGGER.debug("Max swapchain width: %s", graphicsProperties.maxSwapchainImageWidth());
        LOGGER.debug("Max swapchain height: %s", graphicsProperties.maxSwapchainImageHeight());
        LOGGER.debug("Max layers: %s", graphicsProperties.maxLayerCount());

        //view config
        IntBuffer pi = stack.mallocInt(1);
        if (check(xrEnumerateViewConfigurationViews(instance, systemId, viewConfigType, pi, null), "Failed to get view configuration view count: error code %s"))
            return true;

        viewConfigs = fill(XrViewConfigurationView.calloc(pi.get(0)), XrViewConfigurationView.TYPE, XR_TYPE_VIEW_CONFIGURATION_VIEW);
        if (check(xrEnumerateViewConfigurationViews(instance, systemId, viewConfigType, pi, viewConfigs), "Failed to get view configuration views: error code %s"))
            return true;

        int viewCountNumber = pi.get(0);
        if (viewCountNumber <= 0) {
            LOGGER.error("No views found");
            return true;
        }

        views = fill(XrView.calloc(viewCountNumber), XrView.TYPE, XR_TYPE_VIEW);

        if (check(xrEnumerateSwapchainFormats(session, pi, null), "Failed to get swapchain formats: error code %s"))
            return true;

        LongBuffer swapchainFormats = stack.mallocLong(pi.get(0));
        if (check(xrEnumerateSwapchainFormats(session, pi, swapchainFormats), "Failed to get swapchain formats: error code %s"))
            return true;

        long glColorFormat = 0;
        long[] desiredSwapchainFormats = {
                GL_SRGB8_ALPHA8,
                GL_SRGB8,
                GL_RGBA16F,
        };

        label:
        for (long glFormatIter : desiredSwapchainFormats) {
            for (int i = 0; i < swapchainFormats.limit(); i++) {
                if (glFormatIter == swapchainFormats.get(i)) {
                    glColorFormat = glFormatIter;
                    break label;
                }
            }
        }

        if (glColorFormat == 0) {
            LOGGER.error("No compatable swapchain / framebuffer format availible");
            return true;
        }

        swapchains = new Swapchain[viewCountNumber];
        for (int i = 0; i < viewCountNumber; i++) {
            XrViewConfigurationView viewConfig = viewConfigs.get(i);
            Swapchain swapchain = new Swapchain();
            XrSwapchainCreateInfo swapchainCreateInfo = XrSwapchainCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .createFlags(0)
                    .usageFlags(XR_SWAPCHAIN_USAGE_SAMPLED_BIT | XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT)
                    .format(glColorFormat)
                    .sampleCount(viewConfig.recommendedSwapchainSampleCount())
                    .width(viewConfig.recommendedImageRectWidth())
                    .height(viewConfig.recommendedImageRectHeight())
                    .faceCount(1)
                    .arraySize(1)
                    .mipCount(1);

            PointerBuffer pp = stack.mallocPointer(1);
            if (check(xrCreateSwapchain(session, swapchainCreateInfo, pp), "Failed to create swapchain: error code %s"))
                return true;

            swapchain.handle = new XrSwapchain(pp.get(0), session);
            swapchain.width = swapchainCreateInfo.width();
            swapchain.height = swapchainCreateInfo.height();

            if (check(xrEnumerateSwapchainImages(swapchain.handle, pi, null), "Failed to get swapchain image count: error code %s"))
                return true;

            int imageCount = pi.get(0);
            XrSwapchainImageOpenGLKHR.Buffer swapchainImageBuffer = fill(XrSwapchainImageOpenGLKHR.calloc(imageCount), XrSwapchainImageOpenGLKHR.TYPE, XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR);
            if (check(xrEnumerateSwapchainImages(swapchain.handle, pi, XrSwapchainImageBaseHeader.create(swapchainImageBuffer)), "Failed to get swapchain images: error code %s"))
                return true;

            swapchain.images = swapchainImageBuffer;
            swapchains[i] = swapchain;
        }

        return false;
    }

    private static boolean createGraphicsBindingOpenGL(MemoryStack stack, long window, XrSessionCreateInfo sessionCreateInfo, boolean useEGL) {
        if (useEGL) {
            LOGGER.debug("Using XrGraphicsBindingEGLMNDX to create the session...");
            sessionCreateInfo.next(XrGraphicsBindingEGLMNDX.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .getProcAddress(EGL.getCapabilities().eglGetProcAddress)
                    .display(glfwGetEGLDisplay())
                    .config(glfwGetEGLConfig(window))
                    .context(glfwGetEGLContext(window)));
            return false;
        }

        return switch (Platform.get()) {
            case FREEBSD, LINUX -> {
                int platform = glfwGetPlatform();
                if (platform == GLFW_PLATFORM_X11) {
                    long display = glfwGetX11Display();
                    long glxConfig = glfwGetGLXFBConfig(window);

                    XVisualInfo visualInfo = glXGetVisualFromFBConfig(display, glxConfig);
                    if (visualInfo == null) {
                        LOGGER.error("Failed to get visual info");
                        yield true;
                    }
                    long visualid = visualInfo.visualid();

                    LOGGER.debug("Using XrGraphicsBindingOpenGLXlibKHR to create the session");
                    sessionCreateInfo.next(XrGraphicsBindingOpenGLXlibKHR.malloc(stack)
                            .type$Default()
                            .xDisplay(display)
                            .visualid((int) visualid)
                            .glxFBConfig(glxConfig)
                            .glxDrawable(glXGetCurrentDrawable())
                            .glxContext(glfwGetGLXContext(window)));
                    yield false;
                } else {
                    LOGGER.error("X11 is the only Linux windowing system with explicit OpenXR support. All other Linux systems must use EGL");
                    yield true;
                }
            }
            case WINDOWS -> {
                LOGGER.debug("Using XrGraphicsBindingOpenGLWin32KHR to create the session");
                sessionCreateInfo.next(XrGraphicsBindingOpenGLWin32KHR.malloc(stack)
                        .type$Default()
                        .hDC(GetDC(glfwGetWin32Window(window)))
                        .hGLRC(glfwGetWGLContext(window)));
                yield false;
            }
            default -> {
                LOGGER.error("Windows and Linux are the only platforms with explicit OpenXR support. All other platforms must use EGL");
                yield true;
            }
        };
    }

    private static boolean initializeReferenceSpace(MemoryStack stack) {
        PointerBuffer spacePtr = stack.mallocPointer(1);

        XrReferenceSpaceCreateInfo referenceSpace = XrReferenceSpaceCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .referenceSpaceType(XR_REFERENCE_SPACE_TYPE_LOCAL)
                .poseInReferenceSpace(XrPosef.malloc(stack)
                        .position$(XrVector3f.calloc(stack)
                                .set(0f, 0f, 0f))
                        .orientation(XrQuaternionf.calloc(stack)
                                .set(0f, 0f, 0f, 1f)));

        if (check(xrCreateReferenceSpace(session, referenceSpace, spacePtr), "Failed to create reference space: error code %s"))
            return true;

        LOGGER.debug("Created local xr reference space");
        headspace = new XrSpace(spacePtr.get(0), session);
        return false;
    }

    private static boolean initInput(MemoryStack stack) {
        //create the action set
        XrActionSetCreateInfo actionInfo = XrActionSetCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .actionSetName(stack.UTF8(Cinnamon.NAMESPACE))
                .localizedActionSetName(stack.UTF8(Cinnamon.TITLE))
                .priority(0);

        PointerBuffer actionSetPtr = stack.mallocPointer(1);
        if (check(xrCreateActionSet(instance, actionInfo, actionSetPtr), "Failed to create action set: error code %s"))
            return true;

        actionSet = new XrActionSet(actionSetPtr.get(0), instance);
        LOGGER.debug("Created action set");

        //init input
        if (XrInput.init(stack))
            return true;

        //attach action set to the session
        XrSessionActionSetsAttachInfo attachInfo = XrSessionActionSetsAttachInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .actionSets(actionSetPtr);

        if (check(xrAttachSessionActionSets(session, attachInfo), "Failed to attach action set: error code %s"))
            return true;

        LOGGER.debug("Attached action set");
        return false;
    }

    // -- xr event -- //

    private static boolean pollEvents() {
        XrEventDataBaseHeader event;
        while ((event = getXrEvent()) != null) {
            switch (event.type()) {
                case XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING -> {
                    XrEventDataInstanceLossPending instanceLossPending = XrEventDataInstanceLossPending.create(event);
                    LOGGER.error("XrEventDataInstanceLossPending by %s", instanceLossPending.lossTime());
                    return true;
                }
                case XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED -> {
                    XrEventDataSessionStateChanged state = XrEventDataSessionStateChanged.create(event);

                    int oldState = sessionState;
                    sessionState = state.state();

                    LOGGER.debug("XrEventDataSessionStateChanged: state %s -> %s session: %s time: %s", oldState, sessionState, state.session(), state.time());

                    if (state.session() != NULL && state.session() != session.address()) {
                        LOGGER.debug("XrEventDataSessionStateChanged for unknown session");
                        return false;
                    }

                    return checkSessionState();
                }
                default -> LOGGER.debug("Ignoring xr event type %s", event.type());
            }
        }

        return false;
    }

    private static boolean checkSessionState() {
        return switch (sessionState) {
            case XR_SESSION_STATE_READY -> {
                try (MemoryStack stack = stackPush()) {
                    XrSessionBeginInfo sessionBeginInfo = XrSessionBeginInfo.malloc(stack)
                            .type$Default()
                            .next(NULL)
                            .primaryViewConfigurationType(viewConfigType);
                    if (check(xrBeginSession(session, sessionBeginInfo), "Failed to begin OpenXR session: error code %s"))
                        yield true;
                    setSessionRunning(true);
                    yield false;
                }
            }
            case XR_SESSION_STATE_IDLE -> {
                setSessionRunning(false);
                yield false;
            }
            case XR_SESSION_STATE_SYNCHRONIZED, XR_SESSION_STATE_VISIBLE, XR_SESSION_STATE_FOCUSED -> {
                setSessionRunning(true);
                yield false;
            }
            case XR_SESSION_STATE_STOPPING -> {
                setSessionRunning(false);
                yield check(xrEndSession(session), "Failed to end OpenXR session: error code %s");
            }
            case XR_SESSION_STATE_EXITING, XR_SESSION_STATE_LOSS_PENDING -> {
                setSessionRunning(false);
                close();
                yield true;
            }
            default -> false;
        };
    }

    private static XrEventDataBaseHeader getXrEvent() {
        eventDataBuffer.clear();
        eventDataBuffer.type$Default();
        int result = xrPollEvent(instance, eventDataBuffer);
        if (result == XR_SUCCESS) {
            XrEventDataBaseHeader header = XrEventDataBaseHeader.create(eventDataBuffer.address());
            if (header.type() == XR_TYPE_EVENT_DATA_EVENTS_LOST) {
                XrEventDataEventsLost dataEventsLost = XrEventDataEventsLost.create(header);
                LOGGER.warn("%s events lost", dataEventsLost.lostEventCount());
            }
            return header;
        }
        if (result == XR_EVENT_UNAVAILABLE) {
            return null;
        }
        LOGGER.error("XrEventDataBaseHeader returned unexpected result: %s", result);
        return null;
    }

    // -- render -- //

    private static boolean render(MemoryStack stack, Runnable toRender) {
        XrFrameState frameState = XrFrameState.calloc(stack).type$Default();

        int wait = xrWaitFrame(session, XrFrameWaitInfo.calloc(stack).type$Default(), frameState);
        if (wait == XR_SESSION_LOSS_PENDING) {
            return false;
        } else if (check(wait, "Failed to wait for frame: error code %s")) {
            close();
            return false;
        }

        int begin = xrBeginFrame(session, XrFrameBeginInfo.calloc(stack).type$Default());
        if (begin == XR_SESSION_LOSS_PENDING || begin == XR_FRAME_DISCARDED) {
            return false;
        } else if (check(begin, "Failed to begin frame: error code %s")) {
            close();
            return false;
        }

        XrCompositionLayerProjection layerProjection = XrCompositionLayerProjection.calloc(stack).type$Default();
        PointerBuffer layers = stack.callocPointer(1);
        displayTime = frameState.predictedDisplayTime();

        boolean didRender = false;
        if (frameState.shouldRender()) {
            if (renderLayer(stack, layerProjection, toRender)) {
                layers.put(0, layerProjection);
                didRender = true;
            } else if (!initialized) {
                return false;
            } else {
                LOGGER.debug("Not rendering xr frame - no valid tracking poses for the views");
            }
        }

        XrFrameEndInfo endInfo = XrFrameEndInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .displayTime(displayTime)
                .environmentBlendMode(XR_ENVIRONMENT_BLEND_MODE_OPAQUE)
                .layers(didRender ? layers : null)
                .layerCount(didRender ? layers.remaining() : 0);

        if (check(xrEndFrame(session, endInfo), "Failed to end frame: error code %s")) {
            close();
            return false;
        }

        return didRender;
    }

    private static boolean renderLayer(MemoryStack stack, XrCompositionLayerProjection layerProjection, Runnable toRender) {
        XrViewState viewState = XrViewState.calloc(stack).type$Default();
        XrViewLocateInfo viewLocateInfo = XrViewLocateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .viewConfigurationType(viewConfigType)
                .displayTime(displayTime)
                .space(headspace);

        IntBuffer pi = stack.mallocInt(1);
        if (check(xrLocateViews(session, viewLocateInfo, viewState, pi, views), "Failed to locate views: error code %s")) {
            close();
            return false;
        }

        long flags = viewState.viewStateFlags();
        if ((flags & XR_VIEW_STATE_POSITION_VALID_BIT) == 0 || (flags & XR_VIEW_STATE_ORIENTATION_VALID_BIT) == 0)
            return false;

        int viewCount = pi.get(0);
        if (viewCount != views.capacity() || viewCount != viewConfigs.capacity() || viewCount != swapchains.length) {
            LOGGER.error("Mismatched view count: %s", viewCount);
            close();
            return false;
        }

        XrCompositionLayerProjectionView.Buffer projectionLayerViews = fill(
                XrCompositionLayerProjectionView.calloc(viewCount, stack),
                XrCompositionLayerProjectionView.TYPE,
                XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW
        );

        for (int i = 0; i < viewCount; i++) {
            Swapchain viewSwapchain = swapchains[i];
            if (check(xrAcquireSwapchainImage(viewSwapchain.handle, XrSwapchainImageAcquireInfo.calloc(stack).type$Default(), pi), "Failed to acquire swapchain image: error code %s")) {
                close();
                return false;
            }

            int imageIndex = pi.get(0);
            if (check(xrWaitSwapchainImage(viewSwapchain.handle, XrSwapchainImageWaitInfo.malloc(stack).type$Default().next(NULL).timeout(XR_INFINITE_DURATION)), "Failed to wait for swapchain image: error code %s")) {
                close();
                return false;
            }

            XrCompositionLayerProjectionView projectionLayerView = projectionLayerViews.get(i)
                    .pose(views.get(i).pose())
                    .fov(views.get(i).fov())
                    .subImage(si -> si
                            .swapchain(viewSwapchain.handle)
                            .imageRect(rect -> rect
                                    .offset(offset -> offset.x(0).y(0))
                                    .extent(extent -> extent
                                            .width(viewSwapchain.width)
                                            .height(viewSwapchain.height)
                                    )));

            XrRenderer.render(projectionLayerView, viewSwapchain.images.get(imageIndex), i, toRender);

            if (check(xrReleaseSwapchainImage(viewSwapchain.handle, XrSwapchainImageReleaseInfo.calloc(stack).type$Default()), "Failed to release swapchain image: error code %s")) {
                close();
                return false;
            }
        }

        layerProjection.space(headspace);
        layerProjection.views(projectionLayerViews);
        return true;
    }

    // -- internal structures -- //

    static class Swapchain {
        public XrSwapchain handle;
        public int width, height;
        public XrSwapchainImageOpenGLKHR.Buffer images;

        private void destroy() {
            xrDestroySwapchain(handle);
            images.free();
        }
    }
}
