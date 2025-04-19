package cinnamon.vr;

import cinnamon.Cinnamon;
import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.utils.Pair;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.openxr.*;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.vr.XrManager.swapchains;
import static org.lwjgl.opengl.GL11.glViewport;

public class XrRenderer {

    public static final float XR_DEPTH_OFFSET = 0.01f;
    public static final int XR_WIDTH = Cinnamon.WIDTH / 2;
    public static final int XR_HEIGHT = Cinnamon.HEIGHT / 2;
    public static final float XR_NEAR = 0.1f;
    public static final float XR_FAR = 100f;

    private static final XrFramebuffer framebuffer = new XrFramebuffer();
    private static final List<Pair<Vector3f, Quaternionf>> userPoses = new ArrayList<>();

    static float screenCollision = -1f;

    static void free() {
        framebuffer.free();
    }

    static void render(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, int index, Runnable toRender) {
        //prepare framebuffer
        Framebuffer fb = Framebuffer.DEFAULT_FRAMEBUFFER;
        fb.useClear();
        XrRect2Di imageRect = layerView.subImage().imageRect();
        fb.setPos(imageRect.offset().x(), imageRect.offset().y());
        fb.resize(imageRect.extent().width(), imageRect.extent().height());
        fb.adjustViewPort();

        //update camera matrices
        XrPosef pose = layerView.pose();
        XrFovf fov = layerView.fov();
        float distToLeftPlane = Math.tan(fov.angleLeft());
        float distToRightPlane = Math.tan(fov.angleRight());
        float distToBottomPlane = Math.tan(fov.angleDown());
        float distToTopPlane = Math.tan(fov.angleUp());

        XrVector3f pos = pose.position$();
        XrQuaternionf orientation = pose.orientation();

        Camera camera = Client.getInstance().camera;
        camera.setProjFrustum(distToLeftPlane, distToRightPlane, distToBottomPlane, distToTopPlane, XR_NEAR, XR_FAR);
        camera.setXrTransform(pos.x(), pos.y(), pos.z(), orientation.x(), orientation.y(), orientation.z(), orientation.w());

        //render whatever it is going to render
        toRender.run();

        //blit framebuffer back
        framebuffer.use();
        framebuffer.bindColorTexture(swapchainImage);
        Framebuffer.clear();
        Blit.copy(Framebuffer.DEFAULT_FRAMEBUFFER, framebuffer.id(), PostProcess.BLIT);

        if (index == swapchains.length - 1)
            renderBuffer(swapchains[index].width, swapchains[index].height);
    }

    private static void renderBuffer(int width, int height) {
        //grab aspect ratio
        Window window = Client.getInstance().window;
        float aspect = (float) width / height;
        float windowAspect = (float) window.width / window.height;

        //adjust view port to match the aspect ratio
        float x = 0, y = 0;
        float w = window.width, h = window.height;
        if (windowAspect > aspect) {
            w = window.height * aspect;
            x = (window.width - w) / 2f;
        } else {
            h = window.width / aspect;
            y = (window.height - h) / 2f;
        }
        glViewport((int) x, (int) y, (int) w, (int) h);

        //render the buffer
        Framebuffer.DEFAULT_FRAMEBUFFER.useClear();
        Blit.copy(framebuffer, Framebuffer.DEFAULT_FRAMEBUFFER.id(), PostProcess.BLIT_GAMMA);
    }

    public static void applyGUITransform(MatrixStack matrices) {
        matrices.translate(0, 0, -0.5f);
        float s = 1f / 1024f;
        matrices.scale(s, -s, s);
        matrices.translate(-XR_WIDTH / 2f, -XR_HEIGHT / 2f, 0);
    }

    static void setHands(int size) {
        userPoses.clear();
        for (int i = 0; i < size; i++)
            userPoses.add(new Pair<>(new Vector3f(), new Quaternionf()));
    }

    static void updateHand(int hand, XrPosef pose) {
        if (pose == null)
            return;

        Pair<Vector3f, Quaternionf> pair = userPoses.get(hand);

        XrVector3f pos = pose.position$();
        pair.first().set(pos.x(), pos.y(), pos.z());

        XrQuaternionf rot = pose.orientation();
        pair.second().set(rot.x(), rot.y(), rot.z(), rot.w()).rotateX(Math.toRadians(-90f));
    }

    public static Vector3f getHandPos(int hand) {
        return userPoses.get(hand).first();
    }

    public static Quaternionf getHandRot(int hand) {
        return userPoses.get(hand).second();
    }

    public static void renderHands(MatrixStack matrices) {
        for (int i = 0; i < userPoses.size(); i++) {
            boolean activeHand = i == XrInput.getActiveHand();
            Pair<Vector3f, Quaternionf> pair = userPoses.get(i);
            Vector3f pos = pair.first();
            Quaternionf rot = pair.second();

            matrices.pushMatrix();

            matrices.translate(pos);
            matrices.scale(0.02f);

            matrices.pushMatrix();

            matrices.rotate(rot);
            VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, activeHand ? 0xFF72ADFF : 0xFFFF72AD));

            matrices.popMatrix();

            if (activeHand && screenCollision > 0f) {
                Vector3f dir = new Vector3f(0, 0, -1).rotate(rot).mul(screenCollision * 512f).add(pos);
                VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, 0.1f, 0xFF72ADFF));
            }

            matrices.popMatrix();
        }

        VertexConsumer.MAIN.finishBatch(Client.getInstance().camera);
    }
}
