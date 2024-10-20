package cinnamon.gui.screens.extras.jam;

import cinnamon.Client;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.*;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class JamScreen extends ParentedScreen {

    //player
    private static final Resource CAT_TEXTURE = new Resource("textures/jam/cat.png");
    private final Vector2f
            oldPos = new Vector2f(),
            pos = new Vector2f(20f, 238 - size); //ground level
    float oRot, rot;
    private boolean onGround = false;
    private static final float size = 19f;
    private final AABB player = new AABB(new Vector3f(10, 10, 2));
    private final float bb = 5f;

    //physics
    private final Vector3f move = new Vector3f();
    private static final float gravity = 9.8f / 20; //ticks
    private boolean noCollide;

    //drag
    private final Vector2f anchorPoint = new Vector2f();
    private final float dragDistance = 50f, dragForce = 15f;
    private boolean dragging = false;

    //level
    private Level level;
    private int levelIndex = 0;
    private final boolean[] kills = {false, false};

    //death
    private float overlay = 1f, overlayTarget = 1f;
    private static final Resource DEATH = new Resource("textures/jam/death.png");
    private static final Resource END = new Resource("textures/jam/ending.png");

    public JamScreen(Screen parentScreen) {
        super(parentScreen);
        spawn();
    }

    @Override
    protected void addBackButton() {
        //super.addBackButton();
    }



    // stuff //

    private void killPlayer() {
        overlayTarget = 1f;
        move.set((Math.random() < 0.5 ? -1 : 1) * Math.random() * 5, Math.random() * -5 - 5, 0);
        noCollide = true;
    }

    private void spawn() {
        if (kills[0] && kills[1]) {
            levelIndex++;
            kills[0] = kills[1] = false;
        }
        noCollide = false;
        oldPos.set(pos.set(20f, 238 - size));
        move.zero();
        player.set(-100, -100, -1, -100, -100, 1);
        level = (Level) levels[levelIndex].get();
        overlayTarget = 0f;
    }

    private boolean shouldAppearWinScreen() {
        return kills[0] && kills[1] && levelIndex == levels.length - 1;
    }



    // tick //

    @Override
    public void tick() {
        level.tick();
        tickPlayer();
        super.tick();
    }

    private void tickPlayer() {
        //apply forces
        move.y += gravity;
        if (onGround)
            move.mul(0.8f, 0.8f, 1f);
        else
            move.mul(0.91f, 0.98f, 1f);

        //tick collisions
        onGround = false;
        Vector2f toMove = noCollide ? new Vector2f(move.x, move.y) : tickCollisions();

        //add to player
        oldPos.set(pos);
        pos.add(toMove.x, toMove.y);

        //don't let player go below screen
        if (!noCollide) {
            pos.y = Math.min(pos.y, height);
            pos.x = Math.clamp(pos.x, 0, width - size);
        }

        //update hitbox
        player.set(pos.x + bb, pos.y - bb, -1, pos.x + size - bb, pos.y - size + bb, 1);

        oRot = rot;
        rot += move.x * 10f;
    }

    private Vector2f tickCollisions() {
        //early exit
        if (move.lengthSquared() < 0.001f)
            return new Vector2f();

        //prepare variables
        Vector3f pos = player.getCenter();
        Vector3f inflate = player.getDimensions().mul(0.5f);
        Vector3f toMove = new Vector3f(move.x, move.y, 0f);

        //try to resolve collisions in max 3 steps
        for (int i = 0; i < 3; i++) {
            CollisionResult collision = null;

            for (AABB terrainBB : level.terrains) {
                //update bb to include this source's bb
                AABB temp = new AABB(terrainBB).inflate(inflate);

                //check collision
                CollisionResult result = CollisionDetector.collisionRay(temp, pos, toMove);
                if (result != null && (collision == null || collision.near() > result.near())) {
                    collision = result;
                }
            }

            //resolve collision
            if (collision != null) {
                //set ground state
                if (collision.normal().y < 0)
                    this.onGround = true;

                //resolve collision
                CollisionResolver.slide(collision, move, toMove);
            } else {
                //no collision detected
                break;
            }
        }

        return new Vector2f(toMove.x, toMove.y);
    }



    // render //

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        level.render(matrices, delta);
        renderPlayer(matrices, delta);
        renderDragLine(matrices, mouseX, mouseY);
        renderOverlay(matrices);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        VertexConsumer.GUI.consume(GeometryHelper.rectangle(
                matrices,
                0, 0,
                width, height,
                -999, 0
        ));
    }

    private void renderPlayer(MatrixStack matrices, float delta) {
        Vector2f p = Maths.lerp(oldPos, pos, delta);
        matrices.push();
        float s = size / 2f;
        matrices.translate(p.x + s, p.y - s, 0);

        float angle = Maths.lerpAngle(oRot, rot, delta);
        matrices.rotate(Rotation.Z.rotationDeg(angle));

        VertexConsumer.GUI.consume(GeometryHelper.quad(
                matrices,
                -s, -s,
                size, size
        ), CAT_TEXTURE);
        matrices.pop();
    }

    private void renderDragLine(MatrixStack matrices, int mouseX, int mouseY) {
        if (dragging) {
            Vector3f pos = player.getCenter();
            Vector2f drag = calculateDrag(mouseX, mouseY).mul(dragDistance / dragForce);
            int color = ColorUtils.lerpRGBColor(0xFFFFFF, 0xFF0000, drag.length() / dragDistance);

            VertexConsumer.GUI.consume(GeometryHelper.line(
                    matrices,
                    pos.x, pos.y,
                    mouseX, mouseY, 1f,
                    color + (0xFF << 24)
            ));
        }
    }

    private void renderOverlay(MatrixStack matrices) {
        overlay = Maths.lerp(overlay, overlayTarget, UIHelper.tickDelta(0.9f));

        if (overlay <= 0.01f)
            return;

        int alpha = (int) (Math.min(overlay, 1f) * 0xFF) << 24;

        VertexConsumer.GUI.consume(GeometryHelper.rectangle(
                matrices,
                0, 0,
                width, height,
                alpha
        ));

        if (noCollide) {
            Vertex[] vertices = GeometryHelper.quad(matrices, (width - 393) / 2f, (height - 218) / 2f, 393, 218);
            for (Vertex vertex : vertices)
                vertex.color(alpha + 0xFFFFFF);
            VertexConsumer.GUI.consume(vertices, shouldAppearWinScreen() ? END : DEATH);
        }
    }

    private static void debugAABB(MatrixStack matrices, AABB aabb) {
        //debug guillotine
        VertexConsumer.GUI.consume(GeometryHelper.rectangle(
                matrices,
                aabb.minX(), aabb.minY(),
                aabb.maxX(), aabb.maxY(),
                0, 0x88FF72AD
        ));
    }



    // listeners //

    @Override
    public boolean mousePress(int button, int action, int mods) {
        Window w = Client.getInstance().window;

        if (action == GLFW_PRESS) {
            if (noCollide) {
                if (shouldAppearWinScreen()) {
                    close();
                    return true;
                }

                spawn();
                return true;
            }

            if (onGround && new AABB(player).inflate(bb).isInside(w.mouseX, w.mouseY, 0f)) {
                anchorPoint.set(w.mouseX, w.mouseY);
                dragging = true;
                return true;
            }
        } else if (dragging && action == GLFW_RELEASE) {
            dragging = false;
            Vector2f drag = calculateDrag(w.mouseX, w.mouseY);
            if (!Maths.isNaN(drag))
                move.add(drag.x, drag.y, 0f);
        }

        return super.mousePress(button, action, mods);
    }

    private Vector2f calculateDrag(int mouseX, int mouseY) {
        Vector2f drag = new Vector2f(anchorPoint.x - mouseX, anchorPoint.y - mouseY);
        float length = drag.length();
        drag.normalize().mul(Math.min(length, dragDistance) / dragDistance * dragForce);
        return drag;
    }



    // level //

    private static class Level {
        private final List<AABB> terrains = new ArrayList<>();
        private final List<Thing> things = new ArrayList<>();

        private final JamScreen world;
        private final Resource background;

        public Level(JamScreen screen, Resource background) {
            this.world = screen;
            this.background = background;
        }

        public void tick() {
            for (Thing thing : things) {
                if (world.player.intersects(thing.bb))
                    if (thing.onCollide.apply(thing))
                        world.kills[things.indexOf(thing)] = true;
            }
        }

        private void render(MatrixStack matrices, float delta) {
            //render background (500x300)
            VertexConsumer.GUI.consume(GeometryHelper.quad(
                    matrices,
                    0, 0,
                    500, 300
            ), background);

            for (Thing thing : things)
                thing.render(matrices, delta);
        }
    }

    private static final class Thing {
        private final AABB bb;
        private final int frameCount;
        private final Resource texture;
        private final Function<Thing, Boolean> onCollide;

        private int frames = 0;

        private Thing(AABB bb, int frameCount, Resource texture, Function<Thing, Boolean> onCollide) {
            this.bb = bb;
            this.frameCount = frameCount;
            this.texture = texture;
            this.onCollide = onCollide;
        }

        public void render(MatrixStack matrices, float delta) {
            if (texture == null)
                return;

            float width = bb.getWidth();
            float height = bb.getHeight();
            VertexConsumer.GUI.consume(GeometryHelper.quad(
                    matrices,
                    bb.minX(), bb.minY(),
                    width, bb.getHeight(),
                    width * frames, 0f, width, height,
                    (int) (width * frameCount), (int) height
            ), texture);
        }
    }

    private final Supplier[] levels = {
            //level 1
            () -> {
                Level l = new Level(this, new Resource("textures/jam/background1.png"));
                //                                  //width, height, depth             //x, y, z
                l.terrains.add(new AABB(new Vector3f(500, 62, 2)).translate(0, 238, -1)); //ground
                l.terrains.add(new AABB(new Vector3f(97, 17, 2)).translate(250, 148, -1));
                l.terrains.add(new AABB(new Vector3f(84, 15, 2)).translate(369, 189, -1));

                //
                l.things.add(new Thing(new AABB(new Vector3f(55, 62, 2)).translate(245, 85, -1), 1,
                        new Resource("textures/jam/cactus.png"),
                        thing -> {
                            killPlayer();
                            return true;
                        }));
                l.things.add(new Thing(new AABB(new Vector3f(45, 30, 2)).translate(456, 212, -1), 1,
                        new Resource("textures/jam/spikes.png"),
                        thing -> {
                            killPlayer();
                            return true;
                        }));
                l.things.add(new Thing(new AABB(new Vector3f(61, 47, 2)).translate(446, 197, -1), 1,
                        new Resource("textures/jam/bush.png"),
                        thing -> false));
                return l;
            },
            //level 2
            () -> {
                Level l = new Level(this, new Resource("textures/jam/background2.png"));
                //                                  //width, height, depth             //x, y, z
                l.terrains.add(new AABB(new Vector3f(500, 62, 2)).translate(0, 238, -1)); //ground
                l.terrains.add(new AABB(new Vector3f(76, 49, 2)).translate(363, 160, -1));
                l.terrains.add(new AABB(new Vector3f(98, 19, 2)).translate(187, 158, -1));
                l.terrains.add(new AABB(new Vector3f(78, 48, 2)).translate(40, 130, -1));

                //
                Thing fire = new Thing(new AABB(), 1,
                        new Resource("textures/jam/fire.png"), thing2 -> false);
                l.things.add(new Thing(new AABB(new Vector3f(40, 43, 2)).translate(35, 95, -1), 0,null,
                        thing -> {
                            move.mul(0.1f);
                            thing.frames++;
                            if (thing.frames < 1)
                                return false;
                            if (thing.frames == 100)
                                Client.getInstance().queueTick(() -> l.things.add(fire));
                            fire.bb.set(new AABB(new Vector3f(35, 51, 2)).translate(player.getCenter().x - 35 / 2f, player.minY() - 51, -1));
                            if (thing.frames >= 150) {
                                killPlayer();
                                return true;
                            }
                            return false;
                        }));
                l.things.add(new Thing(new AABB(new Vector3f(152, 11, 2)).translate(374, 237, -1), 1,
                        new Resource("textures/jam/puddle.png"),
                        thing -> {
                            move.mul(0.1f);
                            thing.frames++;
                            if (thing.frames >= 150) {
                                killPlayer();
                                return true;
                            }
                            return false;
                        }));
                return l;
            },
            //level 3
            () -> {
                Level l = new Level(this, new Resource("textures/jam/background3.png"));
                //                                  //width, height, depth             //x, y, z
                l.terrains.add(new AABB(new Vector3f(500, 62, 2)).translate(0, 238, -1)); //ground
                l.terrains.add(new AABB(new Vector3f(95, 21, 2)).translate(129, 134, -1));
                l.terrains.add(new AABB(new Vector3f(46, 30, 2)).translate(241, 215, -1));

                //
                l.things.add(new Thing(new AABB(new Vector3f(42, 11, 2)).translate(445, 237, -1), 1,
                        new Resource("textures/jam/hole.png"),
                        thing -> {
                            killPlayer();
                            return true;
                        }));
                l.things.add(new Thing(new AABB(new Vector3f(37, 36, 2)).translate(154, 104, -1), 1,
                        new Resource("textures/jam/choco.png"),
                        thing -> {
                            killPlayer();
                            return true;
                        }));
                return l;
            }
    };
}
