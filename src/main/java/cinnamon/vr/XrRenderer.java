package cinnamon.vr;

import cinnamon.Client;
import cinnamon.model.SimpleGeometry;
import cinnamon.render.Camera;
import cinnamon.render.Window;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.openxr.*;

public class XrRenderer {

    private static XrFramebuffer framebuffer;

    private static XrVector3f pos;
    private static XrQuaternionf orientation;

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

        //save the view matrix data to apply it on the fly
        XrPosef pose = layerView.pose();
        pos = pose.position$();
        orientation = pose.orientation();

        //change the projection matrix
        //the projection is given as is, so we can directly change it
        Matrix4f proj = Client.getInstance().camera.getProjectionMatrix();

        XrFovf fov = layerView.fov();
        float distToLeftPlane = Math.tan(fov.angleLeft());
        float distToRightPlane = Math.tan(fov.angleRight());
        float distToBottomPlane = Math.tan(fov.angleDown());
        float distToTopPlane = Math.tan(fov.angleUp());

        float nearZ = Camera.NEAR_PLANE;
        float farZ = Camera.FAR_PLANE;

        proj.identity().frustum(distToLeftPlane * nearZ, distToRightPlane * nearZ, distToBottomPlane * nearZ, distToTopPlane * nearZ, nearZ, farZ, false);

        toRender.run();

        if (isLastView) {
            Framebuffer.DEFAULT_FRAMEBUFFER.useClear();
            Framebuffer.DEFAULT_FRAMEBUFFER.adjustViewPort();

            Shader s = PostProcess.BLIT.getShader().use();
            s.setTexture("colorTex", swapchainImage.image(), 0);

            SimpleGeometry.QUAD.render();
        }
    }

    public static void applyViewMatrix(Matrix4f viewMatrix) {
        if (pos == null || orientation == null)
            return;

        viewMatrix.translationRotateScaleInvert(
                pos.x(), pos.y(), pos.z(),
                orientation.x(), orientation.y(), orientation.z(), orientation.w(),
                1, 1, 1
        );

        viewMatrix.translate(0, 0, -0.5f);

        float s = 1f / 1024f;
        viewMatrix.scale(s, -s, s);

        Window w = Client.getInstance().window;
        viewMatrix.translate(-w.scaledWidth / 2f, -w.scaledHeight / 2f, 0);
    }

    public static void bindFramebuffer() {
        if (framebuffer != null)
            framebuffer.use();
    }
}
