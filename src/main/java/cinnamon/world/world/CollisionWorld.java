package cinnamon.world.world;

import cinnamon.math.shape.*;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.Camera;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.world.entity.Entity;
import cinnamon.world.worldgen.TerrainGenerator;
import org.joml.Math;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class CollisionWorld extends WorldClient {

    private final Ray ray = new Ray(11.5f, 9f, 5.5f, -1f, 0, 0, 20f);
    private boolean autoRay;

    private final OBB main = new OBB(2, 8f, 5, 1f, 1f, 1f).rotateY(45f);

    private final Vector3f po = new Vector3f(2, 8, 6);
    private final AABB     bb = new AABB(3, 3, 3, 7, 7, 7);
    private final Sphere   sp = new Sphere(0, 9, 5, 1f);
    private final Plane    pl = new Plane(new Vector3f(1, 1, 1).normalize(), -7.5f);
    private final OBB      ob = new OBB(5, 9.5f, 5, 0.5f, 0.5f, 0.5f).rotateZ(45f);

    private final Shape[] shapes = new Shape[] {main, pl, bb, sp, ob};

    @Override
    protected void tempLoad() {
        int r = 32;
        TerrainGenerator.fill(this, -r, 0, -r, r, 0, r, MaterialRegistry.DEFAULT);
    }

    @Override
    public void renderDebug(Camera camera, MatrixStack matrices, float delta) {
        float deltaTime = worldTime + delta;
        int collisionColor = 0xFFFFFF00;

        //animate shapes
        main.setCenter(Math.sin(deltaTime * 0.05f) * 2f + 2f, 8f, 5);
        main.setRotation(main.getRotation().identity().rotateY(Math.toRadians(45f)).rotateZ(Math.toRadians(deltaTime)));
        ob.setRotation(ob.getRotation().identity().rotateY(Math.toRadians(-deltaTime)).rotateZ(Math.toRadians(45f)));

        //render main
        DebugRenderer.renderOBB(matrices, main, 0xFFAD72FF);

        //render shapes (+ point)
        DebugRenderer.renderPoint (matrices, po, 0.1f, main.containsPoint(po) ? collisionColor : 0xFFFFFFFF);
        DebugRenderer.renderAABB  (matrices, bb, bb.intersectsOBB(main) ? collisionColor : 0xFFFFFFFF);
        DebugRenderer.renderSphere(matrices, sp, sp.intersectsOBB(main) ? collisionColor : 0xFFFFFFFF);
        DebugRenderer.renderPlane (matrices, pl, 15f, pl.intersectsOBB(main) ? collisionColor : 0xFFFFFFFF);
        DebugRenderer.renderOBB   (matrices, ob, ob.intersectsOBB(main) ? collisionColor : 0xFFFFFFFF);

        //raycast
        boolean hasHit = false;
        for (Shape s : shapes) {
            Ray.Hit hit = s.collideRay(ray);
            if (hit != null) {
                hasHit = true;

                //near hit
                DebugRenderer.renderPoint(matrices, hit.position(), 0.1f, 0xFF00FF00);

                //far hit
                Vector3f farHit = new Vector3f(ray.getDirection()).mul(hit.tFar()).add(ray.getOrigin());
                DebugRenderer.renderPoint(matrices, farHit, 0.1f, 0xFFFF2200);

                //normal
                matrices.pushMatrix();
                matrices.translate(hit.position());
                DebugRenderer.renderArrow(matrices, hit.normal(), 0.5f, 0xFF00FFFF);
                matrices.popMatrix();
            }
        }

        //render arrow
        matrices.pushMatrix();
        matrices.translate(ray.getOrigin());
        DebugRenderer.renderArrow(matrices, ray.getDirection(), ray.getMaxDistance(), hasHit ? 0xFFFF72AD : 0xFFFFFFFF);
        matrices.popMatrix();

        VertexConsumer.finishAllBatches(camera);
        super.renderDebug(camera, matrices, delta);
    }

    @Override
    protected void updateCamera(Camera sourceCamera, Entity camEntity, int cameraMode, float delta) {
        super.updateCamera(sourceCamera, camEntity, cameraMode, delta);

        if (autoRay) {
            ray.setOrigin(WorldRenderer.camera.getPos());
            ray.setDirection(WorldRenderer.camera.getForwards());
            ray.setMaxDistance(20f);
        }
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        super.keyPress(key, scancode, action, mods);
        if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_F)
            autoRay = !autoRay;
    }

    @Override
    public void respawn(boolean init) {
        super.respawn(init);
        player.setPos(2, 1, 16);
    }
}
