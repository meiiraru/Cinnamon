package cinnamon.world.world;

import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Colors;
import cinnamon.utils.Rotation;
import cinnamon.world.entity.Entity;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_G;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class PrimitiveTestWorld extends WorldClient {

    private static final MatrixStack mat = new MatrixStack(); //dummy matrix stack
    private static final PrimitiveTerrain floor = new PrimitiveTerrain(new Vertex[][]{GeometryHelper.plane(mat, -1000f, 0f, -1000f, 1000f, 1000f, Colors.GREEN.rgba)});

    private static boolean renderNormals;

    @Override
    protected void tempLoad() {
        //floor
        addTerrain(floor);

        //houses
        house(0, 0, -10, 0, 0xFFcac5b8, 0xFF866451);
        house(0, 0, 10, 180, 0xFFcac5b8, 0xFF866451);
        house(-10, 0, 0, 90, 0xFFcac5b8, 0xFF866451);
        house(10, 0, 0, -90, 0xFFcac5b8, 0xFF866451);

        house(0, 0, 25, 90, 0xFFcac5b8, 0xFF866451);
        house(10, 0, 25, -90, 0xFFcac5b8, 0xFF866451);

        house(-15, 0, 30, 180, 0xFFcac5b8, 0xFF866451);
        house(-15, 0, 20, 0, 0xFFcac5b8, 0xFF866451);
        house(-30, 0, 20, 180, 0xFFcac5b8, 0xFF866451);

        house(-25, 0, -10, 90, 0xFFcac5b8, 0xFF866451);

        //church
        church(-30, 0, 7);

        //border
        wall(-36, 0, -19, 56, 0);
        wall(-36, 0, 38, 56, 0);
        wall(-36, 0, -19, 57, -90);
        wall(20, 0, -19, 57, -90);

        //towers
        tower(-35, 0, -18);
        tower(-35, 0, 39);
        tower(21, 0, -18);
        tower(21, 0, 39);

        //fountain
        fountain(3.5f, 0, 2.5f);

        //gateway
        gateway(-8, 0, -19.5f);

        //dont show the player model 
        player.setVisible(false);
    }

    @Override
    public int renderEntities(Camera camera, MatrixStack matrices, float delta) {
        //player
        Vector3f playerPos = player.getPos(delta);
        Vector3f playerDim = player.getAABB().getDimensions();
        render(matrices, GeometryHelper.capsule(matrices, playerPos.x, playerPos.y, playerPos.z, playerDim.x / 2f, playerDim.y, 12, Colors.PINK.rgba));

        return super.renderEntities(camera, matrices, delta);
    }

    private static void render(MatrixStack matrices, Vertex[][] vertices) {
        VertexConsumer.WORLD_MAIN.consume(vertices);
        if (renderNormals)
            TransparentWorld.renderNormals(matrices, vertices);
    }

    private void house(float x, float y, float z, float rotY, int colA, int colB) {
        float w = 8, h = 3, d = 5;

        mat.pushMatrix();
        mat.translate(x + w / 2f, y + h / 2f, z + d / 2f);
        mat.rotate(Rotation.Y.rotationDeg(rotY));
        mat.translate(- w / 2f, - h / 2f, - d / 2f);

        //base
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, 0, 0, 0, w, h, d, colA)));

        mat.translate(0f, h, d / 2f);
        mat.rotate(Rotation.X.rotationDeg(45f));

        float eps = 0.01f;
        float r = (float) Math.sqrt(d * d + d * d) / 4f;

        //base 2
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, eps, -r, -r, w - eps, r, r, colA)));

        //roof
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, -0.5f, r, -r - 0.5f + eps, w + 0.5f, r + 0.5f, r + 0.5f, colB)));
        //roof 2
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, -0.5f + eps, -r - 0.5f, -r - 0.5f, w + 0.5f - eps, r + 0.5f - eps, -r, colB)));

        //windows
        mat.rotate(Rotation.X.rotationDeg(-45f));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, 4, -h + 1, -d / 2f - eps, 5, -h + 2, d / 2f + eps, 0xFF444444)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, 6, -h + 1, -d / 2f - eps, 7, -h + 2, d / 2f + eps, 0xFF444444)));

        //door
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, 1, -h, 1, 2.5f, -h + 2f, d / 2f + eps, colB)));

        mat.popMatrix();
    }

    private void church(float x, float y, float z) {
        house(x, y, z, 90, 0xFF808080, 0xFF1d1119);

        float w = 5, h = 8f, d = 5f;
        
        mat.pushMatrix();
        mat.translate(x + 1.5f, y, z - 1.5f - 5f);

        //base
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, 0, 0, 0, w, h, d, 0xFF808080)));

        //roof
        addTerrain(new PrimitiveTerrain(GeometryHelper.pyramid(mat, -0.5f, h, -0.5f, w + 0.5f, h + 4, d + 0.5f, 0xFF1d1119)));

        //cross
        float eps = 0.01f;
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, w / 2f - 0.25f, h + 3.5f, d / 2f - 0.25f, w / 2f + 0.25f, h + 6.5f, d / 2f + 0.25f, 0xFFd4af37)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, w / 2f - 0.25f - eps, h + 5.5f, d / 2f - 1f, w / 2f + 0.25f + eps, h + 6f, d / 2f + 1f, 0xFFd4af37)));

        //windows
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, w / 2f - 0.5f, 1, -eps, w / 2f + 0.5f, 3, d + eps, 0xFF444444)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, w / 2f - 0.5f, 5, -eps, w / 2f + 0.5f, 7, d + eps, 0xFF444444)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, -eps, 1, d / 2f - 0.5f, w + eps, 3, d / 2f + 0.5f, 0xFF444444)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, -eps, 5, d / 2f - 0.5f, w + eps, 7, d / 2f + 0.5f, 0xFF444444)));

        mat.popMatrix();
    }

    private void wall(float x, float y, float z, float len, float rotY) {
        float h = 5f;
        float d = 2f;

        mat.pushMatrix();
        mat.translate(x + d / 2f, y, z + d / 2f);
        mat.rotate(Rotation.Y.rotationDeg(rotY));

        //center wall
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, 0, 0, -d / 2f, len, h - 1f, d / 2f, 0xFF808080)));
        //left wall
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, 0, 0, -d / 2f - 0.5f, len, h, -d / 2f, 0xFF808080)));
        //right wall
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, 0, 0, d / 2f, len, h, d / 2f + 0.5f, 0xFF808080)));
        
        mat.popMatrix();
    }

    private void tower(float x, float y, float z) {
        float r = 3f, h = 8f;
        
        mat.pushMatrix();
        mat.translate(x, y, z);

        //base
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, 0, 0, 0, r, h, 12, 0xFF808080)));

        //roof
        addTerrain(new PrimitiveTerrain(GeometryHelper.cone(mat, 0, h, 0, r + 0.5f, 4f, 12, 0xFFff5252)));

        mat.popMatrix();
    }

    private void fountain(float x, float y, float z) {
        float r = 2f, h = 0.5f;
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, x, y, z, r, h, 8, 0xFF808080)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, x, y, z, r - 0.5f, h * 2, 6, 0xFF80a0a0)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, x, y, z, r - 1f, h * 3, 5, 0xFF80c0e0)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, x, y, z, r - 1.5f, h * 4, 4, 0xFF80e0f0)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.sphere(mat, x, y + h * 5, z, r - 1.5f, 8, 0xFF80e0f0)));
    }

    private void gateway(float x, float y, float z) {
        mat.pushMatrix();
        mat.translate(x, y + 2f, z);

        float eps = 0.01f;
        //cube - bottom
        addTerrain(new PrimitiveTerrain(GeometryHelper.cube(mat, -2f, -2f, -eps * 2f, 2f, 0f, 3f + eps * 2f, 0xFF202020)));

        //cylinder - top
        mat.rotate(Rotation.X.rotationDeg(90f));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, 0, -eps, 0, 2f, 3f + eps * 2f, 12, 0xFF202020)));
        mat.popMatrix();
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS && key == GLFW_KEY_G)
            renderNormals = !renderNormals;
        super.keyPress(key, scancode, action, mods);
    }

    private static class PrimitiveTerrain extends Terrain {
        private final Vertex[][] vertices;

        public PrimitiveTerrain(Vertex[][] vertices) {
            super(null, TerrainRegistry.CUSTOM);
            this.vertices = vertices;
            this.preciseAABB.add(aabb);
            updateAABB();
        }

        @Override
        public boolean isSelectable(Entity entity) {
            return false;
        }

        @Override
        public void render(MatrixStack matrices, float delta) {
            PrimitiveTestWorld.render(matrices, vertices);
        }

        @Override
        protected void updateAABB() {
            if (vertices == null || vertices.length == 0 || vertices[0].length == 0) {
                super.updateAABB();
                return;
            }

            float minX = Integer.MAX_VALUE; float maxX = Integer.MIN_VALUE;
            float minY = Integer.MAX_VALUE; float maxY = Integer.MIN_VALUE;
            float minZ = Integer.MAX_VALUE; float maxZ = Integer.MIN_VALUE;

            for (Vertex[] vertexArr : vertices) {
                for (Vertex vertex : vertexArr) {
                    Vector3f pos = vertex.getPosition();
                    if (pos.x < minX) minX = pos.x;
                    if (pos.x > maxX) maxX = pos.x;
                    if (pos.y < minY) minY = pos.y;
                    if (pos.y > maxY) maxY = pos.y;
                    if (pos.z < minZ) minZ = pos.z;
                    if (pos.z > maxZ) maxZ = pos.z;
                }
            }

            this.aabb.set(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
