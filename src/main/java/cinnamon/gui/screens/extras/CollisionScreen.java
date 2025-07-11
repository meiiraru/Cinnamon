package cinnamon.gui.screens.extras;

import cinnamon.Client;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.AABB;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class CollisionScreen extends ParentedScreen {

    private final AABB aabb, player;
    private final Vector3f pos, motion;
    private final float size = 10;
    private boolean l, r, u, d;
    private CollisionResult collision;

    public CollisionScreen(Screen parentScreen) {
        super(parentScreen);
        this.aabb = new AABB(100 - size, 100 - size, -size, 100 + size, 100 + size, size);
        this.player = new AABB(150 - size, 150 - size, -size, 150 + size, 150 + size, size);
        this.pos = new Vector3f();
        this.motion = new Vector3f();
    }

    @Override
    public void tick() {
        super.tick();

        float sp = 3;
        if (l) motion.x -= sp;
        if (r) motion.x += sp;
        if (u) motion.y -= sp;
        if (d) motion.y += sp;

        Vector3f pos = player.getCenter();
        AABB temp = new AABB(aabb).inflate(player.getDimensions().mul(0.5f));
        Vector3f move = new Vector3f();

        collision = CollisionDetector.collisionRay(temp, pos, motion);
        if (collision != null)
            CollisionResolver.push(collision, motion, move);

        player.translate(motion);
        aabb.translate(move);
        motion.set(0);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        aabbVsAABB(matrices);
        aabbVsRay(matrices, mouseX, mouseY);
    }

    private void aabbVsRay(MatrixStack matrices, int mouseX, int mouseY) {
        //length
        Vector3f len = new Vector3f(mouseX - pos.x, mouseY - pos.y, 0);

        //collision
        CollisionResult collision = CollisionDetector.collisionRay(aabb, pos, len);
        boolean collided = collision != null;

        //draw box
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, aabb.minX(), aabb.minY(), aabb.maxX(), aabb.maxY(), collided ? 0xFFFF72AD : 0xFFAD72FF));

        if (collided) {
            float n = collision.near();
            Vector3f near = new Vector3f(pos).add(len.x * n, len.y * n, len.z * n);
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, near.x - 3, near.y - 3, near.x + 3, near.y + 3, 0xFF72ADFF));

            float f = collision.far();
            Vector3f far = new Vector3f(pos).add(len.x * f, len.y * f, len.z * f);
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, far.x - 3, far.y - 3, far.x + 3, far.y + 3, 0xFFFFAD72));

            Vector3f normal = collision.normal();
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, near.x, near.y, near.x + normal.x * 10, near.y + normal.y * 10, 1, 0xFF72FF72));
        }

        //draw line
        VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, pos.x, pos.y, mouseX, mouseY, 1, collided ? 0xFFFFFF00 : 0xFFFFFFFF));
    }

    private void aabbVsAABB(MatrixStack matrices) {
        //draw box
        if (collision != null)
            VertexConsumer.LINES.consume(GeometryHelper.rectangle(matrices, aabb.minX(), aabb.minY(), aabb.maxX(), aabb.maxY(), 0xFFFFFF00));

        //draw player
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, player.minX(), player.minY(), player.maxX(), player.maxY(), 0xFFFF72AD));
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean child = super.mousePress(button, action, mods);
        if (child) return true;

        if (action == GLFW_PRESS) {
            int mouseX = Client.getInstance().window.mouseX;
            int mouseY = Client.getInstance().window.mouseY;

            switch (button) {
                case GLFW_MOUSE_BUTTON_1 -> pos.set(mouseX, mouseY, 0);
                case GLFW_MOUSE_BUTTON_2 -> aabb.set(mouseX - size, mouseY - size, -size, mouseX + size, mouseY + size, size);
            }
        }

        return false;
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        boolean press = action != GLFW_RELEASE;
        switch (key) {
            case GLFW_KEY_LEFT  -> l = press;
            case GLFW_KEY_RIGHT -> r = press;
            case GLFW_KEY_UP    -> u = press;
            case GLFW_KEY_DOWN  -> d = press;
            default -> {return super.keyPress(key, scancode, action, mods);}
        }
        return true;
    }
}
