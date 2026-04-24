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
import cinnamon.vr.XrManager;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.math.collision.Resolution.Mode.*;
import static org.lwjgl.glfw.GLFW.*;

public class CollisionScreen extends ParentedScreen {

    private static final float shapeRadius = 10f;
    private static final float speed = 1.5f;
    private static final float maxSpeed = 10f;
    private static final float rotationSpeed = 5f;

    private final Screen parentScreen;
    private final float z;
    private final List<Collider<?>> obstacles;
    private final AABB[] boundaries;
    private final Vector3f rayPos, lastPlayerPos, impulse, velocity;
    private final List<Hit> collidedWith;

    private Collider<?> player;
    private boolean l, r, u, d, rl, rr;
    private Resolution.Mode collisionMode = SLIDE;
    private int points = 0;

    public CollisionScreen(Screen parentScreen) {
        super(parentScreen);
        this.parentScreen = parentScreen;
        this.z = XrManager.isInXR() ? 0f : -100f - shapeRadius;
        this.rayPos = new Vector3f(25f, 25f, z);
        this.lastPlayerPos = new Vector3f();
        this.impulse = new Vector3f();
        this.velocity = new Vector3f();
        this.collidedWith = new ArrayList<>();

        //create player shape at starting position
        this.player = new OBB(50, 50, z, shapeRadius, shapeRadius, shapeRadius).rotateZ(45f);

        //create obstacles
        this.obstacles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                float x = 100 + (j * 50);
                float y = 50 + (i * 50);
                obstacles.add(switch (i) {
                    case 0 -> new AABB(x - shapeRadius, y - shapeRadius, z - shapeRadius, x + shapeRadius, y + shapeRadius, z + shapeRadius);
                    case 1 -> new OBB(x, y, z, shapeRadius, shapeRadius, shapeRadius).rotateZ((j + 1) * 12.5f);
                    case 2 -> new Sphere(x, y, z, shapeRadius);
                    default -> throw new IllegalStateException();
                });
            }
        }

        //add a corner
        obstacles.add(new AABB(300 - shapeRadius,     50 + shapeRadius, z - shapeRadius, 300 + shapeRadius,     50 + shapeRadius * 3, z + shapeRadius));
        obstacles.add(new AABB(300 + shapeRadius,     50 - shapeRadius, z - shapeRadius, 300 + shapeRadius * 3, 50 + shapeRadius,     z + shapeRadius));
        obstacles.add(new AABB(300 + shapeRadius * 3, 50 + shapeRadius, z - shapeRadius, 300 + shapeRadius * 5, 50 + shapeRadius * 3, z + shapeRadius));

        //add a wall of small obstacles
        for (int i = 0; i < 9; i++) {
            float x = 400;
            float y = 50 + (i * shapeRadius * 2);
            obstacles.add(new AABB(x - shapeRadius, y - shapeRadius, z - shapeRadius, x + shapeRadius, y + shapeRadius, z + shapeRadius));
        }

        //add a floor
        for (int i = 0; i < 9; i++) {
            float x = 340 - (i * shapeRadius * 2);
            float y = 210;
            obstacles.add(new AABB(x - shapeRadius, y - shapeRadius, z - shapeRadius, x + shapeRadius, y + shapeRadius, z + shapeRadius));
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
        boundaries[0].set(0, 0, z - shapeRadius, shapeRadius, height, z + shapeRadius);
        //right
        boundaries[1].set(width - shapeRadius, 0, z - shapeRadius, width, height, z + shapeRadius);
        //top
        boundaries[2].set(0, 0, z - shapeRadius, width, shapeRadius, z + shapeRadius);
        //bottom
        boundaries[3].set(0, height - shapeRadius, z - shapeRadius, width, height, z + shapeRadius);
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

        //apply impulse
        if (impulse.lengthSquared() > Maths.KINDA_SMALL_NUMBER) {
            this.velocity.add(impulse.normalize().mul(speed));
            impulse.set(0f);
        }

        //max speed
        if (velocity.lengthSquared() > maxSpeed * maxSpeed)
            velocity.normalize().mul(maxSpeed);

        //obb rotation
        if (player instanceof OBB obb) {
            if (rl) obb.rotateZ(-rotationSpeed);
            if (rr) obb.rotateZ(rotationSpeed);
        }
    }

    private void tickCollisions() {
        //early exit
        if (velocity.lengthSquared() < Maths.KINDA_SMALL_NUMBER)
            return;

        //prepare variables
        collidedWith.clear();
        Vector3f toMove = new Vector3f(velocity);

        //terrain pass
        for (int i = 0; i < 5; i++) {
            Hit collision = null;

            for (AABB boundary : boundaries) {
                Hit result = player.sweep(boundary, toMove);
                if (result != null && result.tNear() >= 0f && (collision == null || result.tNear() < collision.tNear()))
                    collision = result;
            }

            //no collision found - exit loop
            if (collision == null)
                break;

            collidedWith.add(collision);
            Resolution.slide(collision, velocity, toMove);

            //stop if remaining movement is too small
            if (toMove.lengthSquared() < Maths.SMALL_NUMBER) {
                toMove.set(0);
                break;
            }
        }

        //obstacles pass

        //try to resolve collisions with a step limit
        int maxSteps = collisionMode == null ? 0 : 5;
        for (int steps = 0; steps < maxSteps; steps++) {
            //find the closest collision
            Hit collision = null;
            Collider<?> collider = null;

            for (Collider<?> shape : obstacles) {
                //check for collision along the motion ray
                Hit result = player.sweep(shape, toMove);
                if (result != null && (collision == null || result.tNear() < collision.tNear())) {
                    collision = result;
                    collider = shape;
                }
            }

            //no collision found - exit loop
            if (collision == null)
                break;

            collidedWith.add(collision);
            Vector3f pushDelta = new Vector3f();
            Vector3f obstacleMove = new Vector3f();

            //resolve the collision
            switch (collisionMode) {
                case SLIDE  -> Resolution.slide  (collision, velocity, toMove);
                case STICK  -> Resolution.stick  (collision, velocity, toMove);
                case BOUNCE -> {Resolution.bounce(collision, velocity, toMove, 2f); points += 10;}
                case FORCE  -> Resolution.force  (collision, velocity, pushDelta, 0.05f);
                case PUSH   -> Resolution.push   (collision, velocity, obstacleMove);
            }

            //apply resolution movements
            velocity.add(pushDelta);
            collider.translate(obstacleMove.x, obstacleMove.y, 0f);

            //stop if remaining movement is too small
            if (toMove.lengthSquared() < Maths.SMALL_NUMBER) {
                toMove.set(0);
                break;
            }
        }

        player.translate(toMove.x, toMove.y, 0f);

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

        Vector3f pos = player.getCenter();
        Text.of("x: %.3f y: %.3f".formatted(pos.x, pos.y)).withStyle(Style.EMPTY.outlined(true))
                .append(Text.of("\nF: OBB G: AABB H: Sphere").withStyle(Style.EMPTY.color(Colors.DARK_GRAY)))
                .render(VertexConsumer.MAIN, matrices, width / 2f, 4, Alignment.TOP_CENTER);

        if (collisionMode == BOUNCE) {
            Text.of("Score: " + points).withStyle(Style.EMPTY.outlined(true))
                    .render(VertexConsumer.MAIN, matrices, 4, height - 4, Alignment.BOTTOM_LEFT);
        }
    }

    private void rayVsShape(MatrixStack matrices, int mouseX, int mouseY) {
        //length
        Vector3f len = new Vector3f(mouseX - rayPos.x, mouseY - rayPos.y, z - rayPos.z);

        //collision
        Ray ray = new Ray(rayPos.x, rayPos.y, z, len.x, len.y, len.z, len.length());
        Hit hit = player.collideRay(ray);
        boolean collided = hit != null;

        //draw line
        VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, rayPos.x, rayPos.y, mouseX, mouseY, 1, collided ? 0xFFFFFF00 : 0xFFFFFFFF));

        if (collided) {
            Vector3f near = hit.position();
            DebugRenderer.renderPoint(matrices, near, 3, 0xFF72ADFF);

            Vector3f far = ray.getOrigin().fma(hit.tFar() * ray.getMaxDistance(), ray.getDirection(), new Vector3f());
            DebugRenderer.renderPoint(matrices, far, 3, 0xFFFF7200);

            Vector3f normal = hit.normal();
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, near.x, near.y, near.x + normal.x * 10, near.y + normal.y * 10, 1, 0xFF72FF72));
        }
    }

    private void shapeVsShape(MatrixStack matrices) {
        Vector3f point = new Vector3f();
        Vector3f center = player.getCenter();

        //draw all obstacles
        for (Collider<?> shape : obstacles) {
            //draw shape
            DebugRenderer.renderShape(matrices, shape, 0xFF72ADFF);
            DebugRenderer.renderPoint(matrices, shape.closestPoint(center, point), 3, 0xFF72ADFF);

        }

        //draw collisions
        for (Hit col : collidedWith) {
            //draw collision
            DebugRenderer.renderShape(matrices, col.collider(), 0xFFFFFF00);

            //preview the collision ray near and far points
            Vector3f near = col.position();
            DebugRenderer.renderPoint(matrices, near, 3, 0xFFFF72AD);

            Vector3f far = col.ray().getOrigin().fma(col.tFar() * col.ray().getMaxDistance(), col.ray().getDirection(), new Vector3f());
            DebugRenderer.renderPoint(matrices, far, 3, 0xFFFF7200);

            //preview normal
            Vector3f normal = col.normal();
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, near.x, near.y, near.x + normal.x * 10, near.y + normal.y * 10, 1, 0xFF72FF72));
        }
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean child = super.mousePress(button, action, mods);
        if (child) return true;

        if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
            int mouseX = Client.getInstance().window.mouseX;
            int mouseY = Client.getInstance().window.mouseY;
            rayPos.set(mouseX, mouseY, z);
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
            case GLFW_KEY_3 -> {this.collisionMode = BOUNCE; this.points = 0;}
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
