package cinnamon.world.world;

import cinnamon.math.collision.*;
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
    private final Sphere   sp = new Sphere(0, 9, 5, 1f);
    private final AABB     bb = new AABB(3, 3, 3, 7, 7, 7);
    private final OBB      ob = new OBB(5, 9.5f, 5, 0.5f, 0.5f, 0.5f).rotateZ(45f);

    private final Collider<?>[] shapes = new Collider[] {main, sp, bb, ob};

    @Override
    protected void tempLoad() {
        int r = 32;
        TerrainGenerator.fill(this, -r, 0, -r, r, 0, r, MaterialRegistry.DEFAULT);
    }

    @Override
    public void renderDebug(Camera camera, MatrixStack matrices, float delta) {
        float deltaTime = worldTime + delta;

        //animate shapes
        main.setCenter(Math.sin(deltaTime * 0.05f) * 2f + 2f, 8f, 5);
        main.identityRotation().rotateZ(deltaTime).rotateY(45f);
        ob.identityRotation().rotateZ(45f).rotateY(-deltaTime);

        //render shapes (+ point)
        DebugRenderer.renderPoint (matrices, po, 0.1f, main.containsPoint(po) ? 0xFFFFFF00 : 0xFFFFFFFF);
        DebugRenderer.renderShape(matrices, main, 0xFFAD72FF);
        DebugRenderer.renderShape(matrices, sp, sp.intersects(main) ? 0xFFFFFF00 : 0xFFFFFFFF);
        DebugRenderer.renderShape(matrices, bb, bb.intersects(main) ? 0xFFFFFF00 : 0xFFFFFFFF);
        DebugRenderer.renderShape(matrices, ob, ob.intersects(main) ? 0xFFFFFF00 : 0xFFFFFFFF);

        //raycast
        boolean hasHit = false;
        for (Collider<?> s : shapes) {
            Hit hit = s.collideRay(ray);
            if (hit != null) {
                hasHit = true;

                //near hit
                DebugRenderer.renderPoint(matrices, hit.position(), 0.1f, 0xFF00FF00);

                //far hit
                Vector3f farHit = new Vector3f(ray.getDirection()).mul(hit.tFar() * ray.getMaxDistance()).add(ray.getOrigin());
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
