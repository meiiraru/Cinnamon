package cinnamon.vr;

import cinnamon.Client;
import cinnamon.model.SimpleGeometry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import org.joml.Math;
import org.lwjgl.openxr.*;

public class XrRenderer {

    private static XrFramebuffer framebuffer;

    public static void prepare(XrManager.Swapchain[] swapChains) {
        framebuffer = new XrFramebuffer(swapChains);
    }

    public static void free() {
        if (framebuffer != null)
            framebuffer.free();
    }

    public static void render(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, boolean isLastView, Runnable toRender) {
        framebuffer.use();
        framebuffer.bindTextures(swapchainImage);
        framebuffer.clear();
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
        camera.setProjFrustum(distToLeftPlane, distToRightPlane, distToBottomPlane, distToTopPlane);
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

        Window w = Client.getInstance().window;
        matrices.translate(-w.scaledWidth / 2f, -w.scaledHeight / 2f, 0);
    }

    public static void bindFramebuffer() {
        if (framebuffer != null)
            framebuffer.use();
    }
}
