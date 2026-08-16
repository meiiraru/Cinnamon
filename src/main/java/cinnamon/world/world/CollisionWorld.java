package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.math.Rotation;
import cinnamon.math.collision.*;
import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.world.entity.Entity;
import cinnamon.world.terrain.Button;
import cinnamon.world.terrain.MeshTerrain;
import cinnamon.world.terrain.PlaneTerrain;
import cinnamon.world.terrain.PrimitiveTerrain;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.worldgen.TerrainGenerator;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class CollisionWorld extends WorldClient {

    private boolean showDebug = false;

    private final Ray ray = new Ray(11.5f, 9f, 5.5f, -1f, 0, 0, 20f);
    private boolean autoRay;

    private final OBB main = new OBB(2, 8f, 5, 1f, 1f, 1f).rotateY(45f);

    private final Vector3f po = new Vector3f(2, 8, 6);
    private final Sphere   sp = new Sphere(0, 9, 5, 1f);
    private final AABB     bb = new AABB(3, 3, 3, 7, 7, 7);
    private final OBB      ob = new OBB(5, 9.5f, 5, 0.5f, 0.5f, 0.5f).rotateZ(45f);
    private final Plane    pl = new Plane(1, 0, 0, 2f);
    private final MeshCollider mc = new MeshCollider(ModelManager.getMesh(TerrainModelRegistry.TEAPOT.resource)).translate(3.15f, 8.75f, 5.5f).rotateY(45f);

    private final Collider<?>[] shapes = new Collider[] {main, sp, bb, ob, pl, mc};

    @Override
    protected void levelLoad() {
        //floor
        int r = 32;
        TerrainGenerator.fill(this, -r, 0, -r, r, 0, r, MaterialRegistry.DEFAULT.material);

        //spawn platform
        Material debugMat = MaterialRegistry.DEBUG.material;
        TerrainGenerator.fill(this, -12, 1, 16, -11, 5, 17, debugMat);

        //spheres
        for (int i = 0; i < 5; i++) {
            cinnamon.world.terrain.Sphere s = new cinnamon.world.terrain.Sphere();
            s.setPos(i * 5, 1, 1);
            s.setMaterial(debugMat);
            this.addTerrain(s);
        }
        for (int i = 0; i <= 20; i += 2) {
            cinnamon.world.terrain.Sphere s = new cinnamon.world.terrain.Sphere();
            s.setPos(i, 1, -1);
            s.setMaterial(debugMat);
            this.addTerrain(s);
        }

        //ramps
        for (int i = 1; i <= 6; i++)
            this.addTerrain(new SlopeTerrain((i - 1) * 5, -5f, -15, 1.5f, 15f, 15f, Rotation.X.rotationDeg(15f * i), debugMat));

        //floating ramps
        for (int i = 1; i <= 6; i++)
            this.addTerrain(new SlopeTerrain((i - 1) * 5, 3f, 15, 1.5f, 3f, 3f, Rotation.X.rotationDeg(15f * i), debugMat));

        //rotated pillars
        for (int i = 0; i < 3; i++)
            this.addTerrain(new SlopeTerrain(-25, 3, 10 + 5 * i, 2f, 4f, 2f, Rotation.Y.rotationDeg(15f * (i + 1)), debugMat));

        //exclamation mark
        TerrainGenerator.fill(this, -15, 1, 5, -15, 1, 5, debugMat);
        TerrainGenerator.fill(this, -15, 4, 5, -15, 4, 5, debugMat);

        //cage
        TerrainGenerator.fill(this, -15, 1, 10, -15, 1, 10, debugMat);
        TerrainGenerator.fill(this, -16, 3, 9, -14, 3, 11, debugMat);
        removeTerrain(new AABB(-14.5f, 3.5f, 10.5f, -14.5f, 3.5f, 10.5f));

        //spiral stair-case
        int h = 1;
        TerrainGenerator.fill(this, -15, 1, -1, -15, h++, -1, debugMat);
        TerrainGenerator.fill(this, -16, 1, -1, -16, h++, -1, debugMat);
        TerrainGenerator.fill(this, -16, 1,  0, -16, h++,  0, debugMat);
        TerrainGenerator.fill(this, -16, 1,  1, -16, h++,  1, debugMat);
        TerrainGenerator.fill(this, -15, 1,  1, -15, h++,  1, debugMat);
        TerrainGenerator.fill(this, -14, 1,  1, -14, h++,  1, debugMat);

        //small bunker
        TerrainGenerator.fill(this, -15, 1, -6, -15, 1, -5, debugMat);
        TerrainGenerator.fill(this, -16, 1, -6, -16, 2, -5, debugMat);

        //throne
        TerrainGenerator.fill(this, -17, 1, -14, -15, 3, -10, debugMat);
        removeTerrain(new AABB(-14.5f, 3.5f, -12.5f, -15.5f, 3.5f, -10.5f));
        removeTerrain(new AABB(-14.5f, 2.5f, -11.5f, -14.5f, 2.5f, -11.5f));

        //wall
        TerrainGenerator.fill(this, 1, 1, 30, 18, 8, 30, debugMat);
        TerrainGenerator.fill(this, 1, 1, 22, 1, 8, 29, debugMat);

        //teapot
        Terrain teapot = new MeshTerrain(TerrainModelRegistry.TEAPOT.resource, TerrainRegistry.CUSTOM);
        teapot.setPos(-3, 1f, 3);
        teapot.setRotation(0, 45, 0);
        addTerrain(teapot);

        //debug button
        Terrain slab = TerrainRegistry.SLAB.getFactory().get();
        slab.setPos(-6f, 1f, 23f);
        addTerrain(slab);

        Button btt = new Button();
        btt.setPos(-6f, 1.5f, 23f);
        btt.setOnPress(e -> showDebug = true);
        btt.setOnRelease(e -> showDebug = false);
        addTerrain(btt);

        //ground plane
        addTerrain(new PlaneTerrain(0, 1, 0, 0.75f));
    }

    @Override
    public void renderDebug(Camera camera, MatrixStack matrices, float delta) {
        if (!showDebug) {
            super.renderDebug(camera, matrices, delta);
            return;
        }

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

        matrices.pushMatrix();
        matrices.translate(0, 5, 5);
        DebugRenderer.renderShape(matrices, pl, pl.intersects(main) ? 0xFFFFFF00 : 0xFFFFFFFF);
        matrices.popMatrix();

        DebugRenderer.renderShape(matrices, mc, mc.intersects(main) ? 0xFFFFFF00 : 0xFFFFFFFF);

        //raycast
        boolean hasHit = false;
        for (Collider<?> s : shapes) {
            Hit hit = ray.rayCast(s);
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
        if (action == GLFW.GLFW_PRESS) {
            if (key == GLFW.GLFW_KEY_F)
                autoRay = !autoRay;
        }
    }

    @Override
    public void respawn(boolean init) {
        super.respawn(init);
        player.setPos(-11, 6, 17);
        player.setRot(0, 45f, 0);
    }

    private static class SlopeTerrain extends PrimitiveTerrain {
        private final OBB obb;

        public SlopeTerrain(float x, float y, float z, float width, float height, float depth, Quaternionf rotation, Material material) {
            super(genVertices(x, y, z, width, height, depth, rotation));

            this.setPos(x, y, z);
            this.obb = new OBB(x, y, z, width / 2f, height / 2f, depth / 2f).rotate(rotation);
            this.preciseCollider.clear();
            this.preciseCollider.add(obb);

            this.setMaterial(material);
        }

        protected static Vertex[][] genVertices(float x, float y, float z, float width, float height, float depth, Quaternionf rotation) {
            MatrixStack matrices = Client.getInstance().matrices;
            matrices.pushMatrix();
            matrices.translate(x, y, z);
            matrices.rotate(rotation);

            float cx = width / 2f, cy = height / 2f, cz = depth / 2f;
            Vertex[][] vertices = GeometryHelper.box(matrices, -cx, -cy, -cz, cx, cy, cz, 0xFFFFFFFF);

            matrices.popMatrix();
            return vertices;
        }

        @Override
        public void calculateBounds() {
            if (this.obb != null)
                this.aabb.set(obb);
            updateTerrainInWorld();
        }
    }
}
