package cinnamon.vr;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.utils.AABB;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResult;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.openxr.*;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.vr.XrManager.swapchains;
import static org.lwjgl.opengl.GL11.glViewport;

public class XrRenderer {

    //rendering
    public static final float DEPTH_OFFSET = 0.01f;
    public static final float
            NEAR_PLANE = 0.1f,
            FAR_PLANE = 100f;

    //gui
    public static final int
            GUI_WIDTH = 427,
            GUI_HEIGHT = 240;
    public static final float
            GUI_DISTANCE = 0.8f,
            GUI_SCALE = 1f / 512f,
            RAYCAST_DISTANCE = 10f;

    private static final XrFramebuffer framebuffer = new XrFramebuffer();
    private static final List<XrHandTransform> userPoses = new ArrayList<>();

    private static int swapchainIndex = 0;

    private static boolean screenCollided = false;
    private static float screenCollision = -1f;

    static void free() {
        framebuffer.free();
    }

    static void render(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, int index, Runnable toRender) {
        swapchainIndex = index;

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
        camera.setProjFrustum(distToLeftPlane, distToRightPlane, distToBottomPlane, distToTopPlane, NEAR_PLANE, FAR_PLANE);
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

    public static int getRenderIndex() {
        return swapchainIndex;
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
        matrices.translate(0, 0, -GUI_DISTANCE);
        matrices.scale(GUI_SCALE, -GUI_SCALE, GUI_SCALE);
        matrices.translate(-GUI_WIDTH / 2f, -GUI_HEIGHT / 2f, 0);
    }

    static void setHands(int size) {
        userPoses.clear();
        for (int i = 0; i < size; i++)
            userPoses.add(new XrHandTransform());
    }

    static void updateHand(int hand, XrHandTransform transform) {
        XrHandTransform t = userPoses.get(hand);
        t.setFrom(transform);
        t.rot().rotateX(Math.toRadians(-90f));
    }

    public static XrHandTransform getHandTransform(int hand) {
        return userPoses.get(hand);
    }

    private static void applyHandMatrix(XrHandTransform hand, MatrixStack matrices) {
        matrices.translate(hand.pos());
        matrices.scale(0.02f);
        matrices.rotate(hand.rot());
    }

    public static void renderHands(MatrixStack matrices) {
        for (XrHandTransform hand : userPoses) {
            matrices.pushMatrix();
            applyHandMatrix(hand, matrices);
            VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, 0.75f, 0.75f, 0.75f, -0.75f, -0.75f, -0.75f, 0xAAFF72AD));
            VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, -0.4f, -0.4f, -0.4f, 0.4f, 0.4f, 0.4f, 0xFFFF72AD));
            matrices.popMatrix();
        }
    }

    public static void renderHandLaser(MatrixStack matrices) {
        int activeHand = XrInput.getActiveHand();
        XrHandTransform hand = userPoses.get(activeHand);

        matrices.pushMatrix();
        applyHandMatrix(hand, matrices);
        VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, 0.8f, 0.8f, 0.8f, -0.8f, -0.8f, -0.8f, 0xAAFFFFFF));
        matrices.popMatrix();

        if (isScreenCollided()) {
            Vector3f pos = hand.pos();
            Vector3f dir = new Vector3f(0, 0, -1).mul(screenCollision).rotate(hand.rot()).add(pos);
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, 0.002f, 0xFFFFFFFF));
        }
    }

    static void updateScreenCollision() {
        Client c = Client.getInstance();
        //no screen - no collision
        if (c.screen == null) {
            screenCollision = -1f;
            return;
        }

        //calculate mouse position from the current hand pose
        XrHandTransform transform = userPoses.get(XrInput.getActiveHand());
        Vector3f pos = transform.pos();
        Quaternionf rot = transform.rot();
        Vector3f dir = new Vector3f(0, 0, -1).rotate(rot).mul(RAYCAST_DISTANCE);

        //grab screen AABB in world space to raycast collision
        AABB screenAABB = new AABB(0, 0, -GUI_DISTANCE, 0, 0, -GUI_DISTANCE).inflate(GUI_WIDTH * 2f, GUI_HEIGHT * 2f, 0);
        CollisionResult result = CollisionDetector.collisionRay(screenAABB, pos, dir);

        //we got a collision! so undo the collided position back to screen space
        if (result != null) {
            screenCollision = result.near() * RAYCAST_DISTANCE;
            screenCollided = true;

            Vector3f screen = dir
                    .mul(result.near())
                    .add(pos.x, pos.y, pos.z + GUI_DISTANCE)
                    .div(GUI_SCALE, -GUI_SCALE, GUI_SCALE)
                    .add(GUI_WIDTH * 0.5f, GUI_HEIGHT * 0.5f, 0);
            c.mouseMove(screen.x, screen.y);
        } else {
            screenCollided = false;
            screenCollision = 0.5f;
        }
    }

    public static boolean isScreenCollided() {
        return screenCollided;
    }
}
