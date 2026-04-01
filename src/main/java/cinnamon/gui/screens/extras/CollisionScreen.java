package cinnamon.gui.screens.extras;

import cinnamon.Client;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.math.Maths;
import cinnamon.math.collision.*;
import cinnamon.model.GeometryHelper;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.math.collision.CollisionResolver.Mode.*;
import static org.lwjgl.glfw.GLFW.*;

public class CollisionScreen extends ParentedScreen {

    private static final float shapeRadius = 10f;
    private static final float speed = 0.5f;
    private static final float rotationSpeed = 5f;

    private final Screen parentScreen;
    private final List<CollisionShape<?>> obstacles;
    private final AABB[] boundaries;
    private final Vector3f rayPos, lastPlayerPos, impulse, velocity;
    private final List<Collision> collidedWith;

    private CollisionShape<?> player;
    private boolean l, r, u, d, rl, rr;
    private CollisionResolver.Mode collisionMode = SLIDE;

    public CollisionScreen(Screen parentScreen) {
        super(parentScreen);
        this.parentScreen = parentScreen;
        this.rayPos = new Vector3f(25f, 25f, 0f);
        this.lastPlayerPos = new Vector3f();
        this.impulse = new Vector3f();
        this.velocity = new Vector3f();
        this.collidedWith = new ArrayList<>();

        //create player shape at starting position
        this.player = new OBB(50, 50, 0, shapeRadius, shapeRadius, shapeRadius).rotateZ(45f);

        //create obstacles
        this.obstacles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                float x = 100 + (j * 50);
                float y = 50 + (i * 50);
                obstacles.add(switch (i) {
                    case 0 -> new AABB(x - shapeRadius, y - shapeRadius, -shapeRadius, x + shapeRadius, y + shapeRadius, shapeRadius);
                    case 1 -> new OBB(x, y, 0, shapeRadius, shapeRadius, shapeRadius).rotateZ((j + 1) * 12.5f);
                    case 2 -> new Sphere(x, y, 0, shapeRadius);
                    default -> throw new IllegalStateException();
                });
            }
        }

        //add a corner
        obstacles.add(new AABB(300 - shapeRadius,     50 + shapeRadius, -shapeRadius, 300 + shapeRadius,     50 + shapeRadius * 3, shapeRadius));
        obstacles.add(new AABB(300 + shapeRadius,     50 - shapeRadius, -shapeRadius, 300 + shapeRadius * 3, 50 + shapeRadius,     shapeRadius));
        obstacles.add(new AABB(300 + shapeRadius * 3, 50 + shapeRadius, -shapeRadius, 300 + shapeRadius * 5, 50 + shapeRadius * 3, shapeRadius));

        //add a wall of small obstacles
        for (int i = 0; i < 9; i++) {
            float x = 400;
            float y = 50 + (i * shapeRadius * 2);
            obstacles.add(new AABB(x - shapeRadius, y - shapeRadius, -shapeRadius, x + shapeRadius, y + shapeRadius, shapeRadius));
        }

        //boundaries
        boundaries = new AABB[4];
        for (int i = 0; i < 4; i++)
            boundaries[i] = new AABB();
    }

    @Override
    public void init() {
        super.init();

        //generate boundaries
        //left
        boundaries[0].set(0, 0, -shapeRadius, shapeRadius, height, shapeRadius);
        //right
        boundaries[1].set(width - shapeRadius, 0, -shapeRadius, width, height, shapeRadius);
        //top
        boundaries[2].set(0, 0, -shapeRadius, width, shapeRadius, shapeRadius);
        //bottom
        boundaries[3].set(0, height - shapeRadius, -shapeRadius, width, height, shapeRadius);
    }

    @Override
    public void tick() {
        super.tick();
        this.lastPlayerPos.set(player.getCenter());
        tickInput();
        tickCollisions();
    }

    private void tickInput() {
        //movement
        if (l) impulse.x -= 1;
        if (r) impulse.x += 1;
        if (u) impulse.y -= 1;
        if (d) impulse.y += 1;

        if (impulse.lengthSquared() > Maths.KINDA_SMALL_NUMBER) {
            this.velocity.add(impulse.normalize().mul(speed));
            impulse.set(0f);
        }

        //obb rotation
        if (player instanceof OBB obb) {
            if (rl) obb.rotateZ(-rotationSpeed);
            if (rr) obb.rotateZ(rotationSpeed);
        }
    }

    private void tickCollisions() {
        if (velocity.lengthSquared() < Maths.KINDA_SMALL_NUMBER)
            return;

        player.translate(velocity.x, velocity.y, 0f);
        collidedWith.clear();

        //resolve collisions
        int maxSteps = collisionMode == null ? 0 : 5;
        for (int step = 0; step < maxSteps; step++) {
            Collision collision = null;
            boolean forceSlide = false;

            for (CollisionShape<?> shape : obstacles) {
                Collision col = player.collide(shape);
                if (col != null && (collision == null || col.depth() > collision.depth()))
                    collision = col;
            }

            for (AABB shape : boundaries) {
                Collision col = player.collide(shape);

                if (col != null && (collision == null || col.depth() > collision.depth())) {
                    collision = col;
                    forceSlide = true;
                }
            }

            //early exit when there is no collisions
            if (collision == null)
                break;

            collidedWith.add(collision);
            Vector3f thisMove = new Vector3f();
            Vector3f obstacleMove = new Vector3f();

            //resolve the collision
            CollisionResolver.Mode mode = forceSlide ? SLIDE : collisionMode;
            switch (mode) {
                case SLIDE  -> CollisionResolver.slide (collision, velocity, thisMove);
                case STICK  -> CollisionResolver.stick (collision, velocity, thisMove);
                case BOUNCE -> CollisionResolver.bounce(collision, velocity, thisMove, 2f);
                case FORCE  -> CollisionResolver.force (collision, velocity, 0.03f);
                case PUSH   -> CollisionResolver.push  (collision, obstacleMove);
            }

            //apply movements
            collision.shapeA().translate(thisMove.x, thisMove.y, 0f);
            collision.shapeB().translate(obstacleMove.x, obstacleMove.y, 0f);
        }

        //decay momentum
        velocity.mul(0.9f);
        if (velocity.lengthSquared() < Maths.KINDA_SMALL_NUMBER)
            velocity.set(0f);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        //draw boundaries
        for (int i = 0; i < 4; i++) {
            AABB boundary = boundaries[i];
            DebugRenderer.renderShape(matrices, boundary, 0xFF727272);
        }

        //draw shapes and collisions
        shapeVsShape(matrices);
        rayVsShape(matrices, mouseX, mouseY);
        DebugRenderer.renderShape(matrices, player.clone().setCenter(Maths.lerp(lastPlayerPos, player.getCenter(), delta)), 0xFFFF72AD);

        //draw UI
        Text.of("Mode: " + (collisionMode == null ? "NONE" : collisionMode.name())).withStyle(Style.EMPTY.outlined(true))
                .append(Text.of("\n1-6").withStyle(Style.EMPTY.color(Colors.DARK_GRAY)))
                .render(VertexConsumer.MAIN, matrices, width - 4, 4, Alignment.TOP_RIGHT);
    }

    private void rayVsShape(MatrixStack matrices, int mouseX, int mouseY) {
        //length
        Vector3f len = new Vector3f(mouseX - rayPos.x, mouseY - rayPos.y, 0);

        //collision
        Ray ray = new Ray(rayPos.x, rayPos.y, 0f, len.x, len.y, 0f, len.length());
        Hit hit = player.collideRay(ray);
        boolean collided = hit != null;

        //draw line
        VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, rayPos.x, rayPos.y, mouseX, mouseY, 1, collided ? 0xFFFFFF00 : 0xFFFFFFFF));

        if (collided) {
            Vector3f near = hit.position();
            DebugRenderer.renderPoint(matrices, near, 3, 0xFF72ADFF);

            Vector3f far = ray.getOrigin().fma(hit.tFar(), ray.getDirection(), new Vector3f());
            DebugRenderer.renderPoint(matrices, far, 3, 0xFFFF7200);

            Vector3f normal = hit.normal();
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, near.x, near.y, near.x + normal.x * 10, near.y + normal.y * 10, 1, 0xFF72FF72));
        }
    }

    private void shapeVsShape(MatrixStack matrices) {
        Vector3f point = new Vector3f();
        Vector3f center = player.getCenter();

        //draw all obstacles
        for (CollisionShape<?> shape : obstacles) {
            //draw shape
            DebugRenderer.renderShape(matrices, shape, 0xFF72ADFF);
            DebugRenderer.renderPoint(matrices, shape.closestPoint(center, point), 3, 0xFF72ADFF);

        }

        //draw collisions
        for (Collision col : collidedWith) {
            //draw collision
            DebugRenderer.renderShape(matrices, col.shapeB(), 0xFFFFFF00);

            //preview the MTV
            Vector3f normal = col.normal();
            float depth = col.depth();

            //invert normal points to get the direction to move the player out of collision
            float mtvX = -normal.x * depth;
            float mtvY = -normal.y * depth;

            float endX = center.x + mtvX;
            float endY = center.y + mtvY;

            //draw a line showing the Minimum Translation Vector
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, center.x, center.y, endX, endY, 2, 0xFFFF00FF));

            //draw a point at the tip of the MTV arrow
            DebugRenderer.renderPoint(matrices, point.set(endX, endY, 0), 4, 0xFFFF00FF);
        }
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean child = super.mousePress(button, action, mods);
        if (child) return true;

        if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
            int mouseX = Client.getInstance().window.mouseX;
            int mouseY = Client.getInstance().window.mouseY;
            rayPos.set(mouseX, mouseY, 0);
        }

        return false;
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        boolean press = action != GLFW_RELEASE;
        switch (key) {
            case GLFW_KEY_A, GLFW_KEY_LEFT  -> l = press;
            case GLFW_KEY_D, GLFW_KEY_RIGHT -> r = press;
            case GLFW_KEY_W, GLFW_KEY_UP    -> u = press;
            case GLFW_KEY_S, GLFW_KEY_DOWN  -> d = press;
            case GLFW_KEY_Q, GLFW_KEY_PAGE_UP   -> rl = press;
            case GLFW_KEY_E, GLFW_KEY_PAGE_DOWN -> rr = press;

            case GLFW_KEY_R -> client.setScreen(new CollisionScreen(parentScreen));

            case GLFW_KEY_1 -> this.collisionMode = SLIDE;
            case GLFW_KEY_2 -> this.collisionMode = STICK;
            case GLFW_KEY_3 -> this.collisionMode = BOUNCE;
            case GLFW_KEY_4 -> this.collisionMode = FORCE;
            case GLFW_KEY_5 -> this.collisionMode = PUSH;
            case GLFW_KEY_6 -> this.collisionMode = null;

            case GLFW_KEY_F -> {
                Vector3f center = player.getCenter();
                this.player = new OBB(shapeRadius).translate(center).rotateZ(45f);
            }
            case GLFW_KEY_G -> {
                Vector3f center = player.getCenter();
                this.player = new AABB().inflate(shapeRadius).translate(center);
            }
            case GLFW_KEY_H -> {
                Vector3f center = player.getCenter();
                this.player = new Sphere(shapeRadius).translate(center);
            }

            default -> {return super.keyPress(key, scancode, action, mods);}
        }
        return true;
    }
}
