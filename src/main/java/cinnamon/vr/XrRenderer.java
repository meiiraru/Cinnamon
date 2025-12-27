package cinnamon.vr;

import cinnamon.Client;
import cinnamon.gui.GUIStyle;
import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.model.SimpleGeometry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResult;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.openxr.*;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.vr.XrManager.swapchains;
import static org.lwjgl.opengl.GL11.*;

public class XrRenderer {

    public static final Resource HAND_PATH = new Resource("models/xr/hands/paw.obj");

    //rendering
    public static final float DEPTH_OFFSET = 0.01f;

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

    private static ModelRenderer handModel;

    static void free() {
        framebuffer.free();
    }

    static void render(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, int index, Runnable toRender) {
        swapchainIndex = index;

        //prepare framebuffer
        Framebuffer fb = Framebuffer.DEFAULT_FRAMEBUFFER;
        XrRect2Di imageRect = layerView.subImage().imageRect();
        fb.setPos(imageRect.offset().x(), imageRect.offset().y());
        fb.resize(imageRect.extent().width(), imageRect.extent().height());
        fb.useClear();
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
        camera.setProjFrustum(distToLeftPlane, distToRightPlane, distToBottomPlane, distToTopPlane, Camera.NEAR_PLANE, Camera.FAR_PLANE);
        camera.setXrTransform(pos.x(), pos.y(), pos.z(), orientation.x(), orientation.y(), orientation.z(), orientation.w());

        //render whatever it is going to render
        toRender.run();

        //blit framebuffer back
        framebuffer.use();
        framebuffer.bindColorTexture(swapchainImage);
        Framebuffer.DEFAULT_FRAMEBUFFER.blit(framebuffer);
        Framebuffer.DEFAULT_FRAMEBUFFER.useClear();

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
        Shader blit = PostProcess.BLIT_GAMMA.getShader().use();
        blit.setTexture("colorTex", framebuffer.getColorBuffer(), 0);
        blit.setFloat("gamma", 1f / 2.2f);
        SimpleGeometry.QUAD.render();
    }

    public static void applyGUITransform(MatrixStack matrices) {
        matrices.translate(0, 0, -GUI_DISTANCE);
        matrices.scale(GUI_SCALE, -GUI_SCALE, GUI_SCALE);
        matrices.translate(-GUI_WIDTH / 2f, -GUI_HEIGHT / 2f, 0);
    }

    public static void removeGUITransform(MatrixStack matrices) {
        matrices.translate(GUI_WIDTH / 2f, GUI_HEIGHT / 2f, 0);
        matrices.scale(1f / GUI_SCALE, 1f / -GUI_SCALE, 1f / GUI_SCALE);
        matrices.translate(0, 0, GUI_DISTANCE);
    }

    static void setHands(int size) {
        userPoses.clear();
        for (int i = 0; i < size; i++)
            userPoses.add(new XrHandTransform());
        handModel = ModelManager.load(HAND_PATH);
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
        matrices.scale(0.1f);
        matrices.rotate(hand.rot());
    }

    public static void renderHands(MatrixStack matrices) {
        for (int i = 0; i < userPoses.size(); i++) {
            //skip unmoved hands
            XrHandTransform hand = userPoses.get(i);
            if (hand.pos().lengthSquared() == 0)
                continue;

            //apply the hand matrix
            matrices.pushMatrix();
            applyHandMatrix(hand, matrices);

            //flip the model for the left hand
            boolean lefty = i % 2 == 0;
            if (lefty) {
                matrices.peek().pos().scale(-1, 1, 1);
                matrices.peek().normal().scale(-1, 1, 1);
                glFrontFace(GL_CW);
            }

            //render
            handModel.render(matrices);

            //clear the rendering
            matrices.popMatrix();
            if (lefty) glFrontFace(GL_CCW);
        }
    }

    public static void renderHandLaser(MatrixStack matrices) {
        //grab the active hand
        int activeHand = XrInput.getActiveHand();
        XrHandTransform hand = userPoses.get(activeHand);

        //skip unmoved hand
        if (hand.pos().lengthSquared() == 0)
            return;

        //prepare the renderer
        boolean lefty = activeHand % 2 == 0;
        WorldRenderer.setupFramebuffer();
        WorldRenderer.initOutlineBatch(Client.getInstance().camera);

        //apply the hand matrices
        matrices.pushMatrix();
        applyHandMatrix(hand, matrices);
        if (lefty) {
            matrices.peek().pos().scale(-1, 1, 1);
            matrices.peek().normal().scale(-1, 1, 1);
            glFrontFace(GL_CW);
        }

        int color = GUIStyle.getDefault().getInt("xr_interact_color");
        Shader.activeShader.applyColorRGBA(color);

        //render
        handModel.render(matrices);

        //cleanup
        matrices.popMatrix();
        if (lefty) glFrontFace(GL_CCW);
        WorldRenderer.bakeOutlines(s -> s.setFloat("radius", 8f));

        //render the laser
        if (isScreenCollided()) {
            Vector3f pos = hand.pos();
            Vector3f dir = new Vector3f(0, 0, -1).mul(screenCollision).rotate(hand.rot()).add(pos);
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, 0.002f, color));
        }
    }

    static void updateScreenCollision() {
        Client c = Client.getInstance();
        //no screen - no collision
        if (c.screen == null) {
            screenCollided = false;
            return;
        }

        //calculate mouse position from the current hand pose
        XrHandTransform transform = userPoses.get(XrInput.getActiveHand());
        Vector3f pos = transform.pos();

        //do not collide if the hand is not moved
        if (pos.lengthSquared() == 0) {
            screenCollided = false;
            return;
        }

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
        }
    }

    public static boolean isScreenCollided() {
        return screenCollided;
    }
}
