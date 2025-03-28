package cinnamon.vr;

import cinnamon.Cinnamon;
import cinnamon.Client;
import cinnamon.model.SimpleGeometry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import org.joml.Math;
import org.lwjgl.openxr.*;

public class XrRenderer {

    public static final float XR_DEPTH_OFFSET = 0.01f;
    public static final int XR_WIDTH = Cinnamon.WIDTH / 2;
    public static final int XR_HEIGHT = Cinnamon.HEIGHT / 2;
    public static final float XR_NEAR = 0.1f;
    public static final float XR_FAR = 100f;

    private static XrFramebuffer framebuffer;

    public static void prepare(XrManager.Swapchain[] swapChains) {
        framebuffer = new XrFramebuffer(swapChains, swapChains[0].width, swapChains[0].height);
    }

    public static void free() {
        if (framebuffer != null)
            framebuffer.free();
    }

    public static void render(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, boolean isLastView, Runnable toRender) {
        framebuffer.use();
        framebuffer.bindTextures(swapchainImage);
        Framebuffer.clear();
        framebuffer.adjustViewPort(layerView.subImage().imageRect());

        XrPosef pose = layerView.pose();
        XrFovf fov = layerView.fov();
        float distToLeftPlane = Math.tan(fov.angleLeft());
        float distToRightPlane = Math.tan(fov.angleRight());
        float distToBottomPlane = Math.tan(fov.angleDown());
        float distToTopPlane = Math.tan(fov.angleUp());

        XrVector3f pos = pose.position$();
        XrQuaternionf orientation = pose.orientation();

        //update camera matrices
        Camera camera = Client.getInstance().camera;
        camera.setProjFrustum(distToLeftPlane, distToRightPlane, distToBottomPlane, distToTopPlane, XR_NEAR, XR_FAR);
        camera.setXrTransform(pos.x(), pos.y(), pos.z(), orientation.x(), orientation.y(), orientation.z(), orientation.w());

        toRender.run();

        if (isLastView) {
            Framebuffer.DEFAULT_FRAMEBUFFER.useClear();
            Framebuffer.DEFAULT_FRAMEBUFFER.adjustViewPort();

            Shader old = Shader.activeShader;
            Shader s = PostProcess.BLIT_GAMMA.getShader().use();
            s.setTexture("colorTex", swapchainImage.image(), 0);

            SimpleGeometry.QUAD.render();

            old.use();
        }
    }

    public static void applyGUITransform(MatrixStack matrices) {
        matrices.translate(0, 0, -0.5f);
        float s = 1f / 1024f;
        matrices.scale(s, -s, s);
        matrices.translate(-XR_WIDTH / 2f, -XR_HEIGHT / 2f, 0);
    }

    public static void bindFramebuffer() {
        if (framebuffer != null)
            framebuffer.use();
    }
}
