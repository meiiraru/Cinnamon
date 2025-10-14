package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.gui.Toast;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.PostProcess;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.Rotation;
import cinnamon.world.DamageType;
import cinnamon.world.Hud;
import cinnamon.world.entity.living.Player;
import cinnamon.world.entity.misc.TriggerArea;
import cinnamon.world.terrain.PrimitiveTerrain;
import cinnamon.world.terrain.Terrain;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

import static org.lwjgl.glfw.GLFW.*;

public class PrimitiveTestWorld extends WorldClient {

    private static final MatrixStack mat = new MatrixStack(); //dummy matrix stack
    private static final PrimitiveTerrain floor = new PrimitiveTerrain(GeometryHelper.plane(mat, -1000f, 0f, -1000f, 1000f, 1000f, 1, 1, Colors.GREEN.argb));

    private boolean renderNormals;

    @Override
    protected void tempLoad() {
        Toast.clear(Toast.ToastType.WORLD);

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

        //spikes
        spikes(-16, 0, 8);

        this.hud = new Hud2();
        this.hud.init();

        //set sky fog
        //sky.fogStart = 0; sky.fogEnd = 8; sky.fogColor = 0;
        //sky.setSkyBox(SkyBoxRegistry.SPACE.resource);
    }

    @Override
    public void render(MatrixStack matrices, float delta) {
        super.render(matrices, delta);
        PostProcess.apply(PostProcess.TOON_OUTLINE);
    }

    @Override
    public void renderDebug(Camera camera, MatrixStack matrices, float delta) {
        if (renderNormals) {
            for (Terrain terrain : terrainManager.queryCustom(camera::isInsideFrustum)) {
                if (terrain instanceof PrimitiveTerrain pt)
                    TransparentWorld.renderNormals(matrices, pt.getVertices());
            }
        }

        super.renderDebug(camera, matrices, delta);
    }

    private void house(float x, float y, float z, float rotY, int colA, int colB) {
        float w = 8, h = 3, d = 5;

        mat.pushMatrix();
        mat.translate(x + w / 2f, y + h / 2f, z + d / 2f);
        mat.rotate(Rotation.Y.rotationDeg(rotY));
        mat.translate(- w / 2f, - h / 2f, - d / 2f);

        //base
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, 0, 0, 0, w, h, d, colA)));

        mat.translate(0f, h, d / 2f);
        mat.rotate(Rotation.X.rotationDeg(45f));

        float eps = 0.01f;
        float r = Math.sqrt(d * d + d * d) / 4f;

        //base 2
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, 0, -r, -r, w, r, r, colA)));

        //roof
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, -0.5f, r, -r - 0.5f, w + 0.5f, r + 0.5f, r + 0.5f, colB)));
        //roof 2
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, -0.5f, -r - 0.5f, -r - 0.5f, w + 0.5f, r + 0.5f, -r, colB)));

        //windows
        mat.rotate(Rotation.X.rotationDeg(-45f));
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, 4, -h + 1, -d / 2f - eps, 5, -h + 2, d / 2f + eps, 0xFF444444)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, 6, -h + 1, -d / 2f - eps, 7, -h + 2, d / 2f + eps, 0xFF444444)));

        //door
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, 1, -h, 1, 2.5f, -h + 2f, d / 2f + eps, colB)));

        mat.popMatrix();
    }

    private void church(float x, float y, float z) {
        house(x, y, z, 90, 0xFF808080, 0xFF1d1119);

        float w = 5, h = 8f, d = 5f;

        mat.pushMatrix();
        mat.translate(x + 1.5f, y, z - 1.5f - 5f);

        //base
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, 0, 0, 0, w, h, d, 0xFF808080)));

        //roof
        addTerrain(new PrimitiveTerrain(GeometryHelper.pyramid(mat, -0.5f, h, -0.5f, w + 0.5f, h + 4, d + 0.5f, 0xFF1d1119)));

        //cross
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, w / 2f - 0.25f, h + 3.5f, d / 2f - 0.25f, w / 2f + 0.25f, h + 6.5f, d / 2f + 0.25f, 0xFFd4af37)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, w / 2f - 0.25f, h + 5.5f, d / 2f - 1f, w / 2f + 0.25f, h + 6f, d / 2f + 1f, 0xFFd4af37)));

        //windows
        float eps = 0.01f;
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, w / 2f - 0.5f, 1, -eps, w / 2f + 0.5f, 3, d + eps, 0xFF444444)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, w / 2f - 0.5f, 5, -eps, w / 2f + 0.5f, 7, d + eps, 0xFF444444)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, -eps, 1, d / 2f - 0.5f, w + eps, 3, d / 2f + 0.5f, 0xFF444444)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, -eps, 5, d / 2f - 0.5f, w + eps, 7, d / 2f + 0.5f, 0xFF444444)));

        mat.popMatrix();
    }

    private void wall(float x, float y, float z, float len, float rotY) {
        float h = 5f;
        float d = 2f;

        mat.pushMatrix();
        mat.translate(x + d / 2f, y, z + d / 2f);
        mat.rotate(Rotation.Y.rotationDeg(rotY));

        //center wall
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, 0, 0, -d / 2f, len, h - 1f, d / 2f, 0xFF808080)));
        //left wall
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, 0, 0, -d / 2f - 0.5f, len, h, -d / 2f, 0xFF808080)));
        //right wall
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, 0, 0, d / 2f, len, h, d / 2f + 0.5f, 0xFF808080)));

        mat.popMatrix();
    }

    private void tower(float x, float y, float z) {
        float r = 3f, h = 8f;

        mat.pushMatrix();
        mat.translate(x, y, z);

        //base
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, 0, 0, 0, h, r, 12, 0xFF808080)));

        //roof
        addTerrain(new PrimitiveTerrain(GeometryHelper.cone(mat, 0, h, 0, 4f, r + 0.5f, 12, 0xFFff5252)));

        mat.popMatrix();
    }

    private void fountain(float x, float y, float z) {
        float r = 2f, h = 0.5f;
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, x, y, z, h, r, 8, 0xFF808080)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, x, y, z, h * 2, r - 0.5f, 6, 0xFF80a0a0)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, x, y, z, h * 3, r - 1f, 5, 0xFF80c0e0)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, x, y, z, h * 4, r - 1.5f, 4, 0xFF80e0f0)));
        addTerrain(new PrimitiveTerrain(GeometryHelper.sphere(mat, x, y + h * 5, z, r - 1.5f, 8, 0xFF80e0f0), false));
    }

    private void gateway(float x, float y, float z) {
        mat.pushMatrix();
        mat.translate(x, y + 2f, z);

        float eps = 0.01f;
        //box - bottom
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(mat, -2f, -2f, -eps, 2f, 0f, 3f + eps, 0xFF202020)));

        //cylinder - top
        mat.rotate(Rotation.X.rotationDeg(90f));
        addTerrain(new PrimitiveTerrain(GeometryHelper.cylinder(mat, 0, -eps, 0, 3f + eps * 2f, 2f, 12, 0xFF202020)));
        mat.popMatrix();
    }

    private void spikes(float x, float y, float z) {
        //generate a 3x3 grid of spikes
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++) {
                mat.pushMatrix();
                mat.translate(x + i / 6f, y, z + j / 6f);
                mat.rotate(Rotation.X.rotationDeg(45 * j));
                mat.rotate(Rotation.Z.rotationDeg(-45 * i));
                addTerrain(new PrimitiveTerrain(GeometryHelper.cone(mat, 0, 0, 0, 0.5f, 0.15f, 12, 0xFF804000)));
                mat.popMatrix();
            }

        float w = 1.1f;
        float h = 0.6f;
        float d = 1.1f;

        TriggerArea ta = new TriggerArea(UUID.randomUUID(), e -> e.damage(null, DamageType.TERRAIN, 10, false), w, h, d);
        ta.setPos(x, y + h / 2f, z);
        ta.setOneTime(false);
        addEntity(ta);
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            switch (key) {
                case GLFW_KEY_G -> renderNormals = !renderNormals;
                case GLFW_KEY_H -> {
                    this.close();
                    new PrimitiveTestWorld().init();
                }
            }
        }
        super.keyPress(key, scancode, action, mods);
    }

    @Override
    public void respawn(boolean init) {
        super.respawn(init);

        player.setVisible(false);
        player.addRenderFeature((source, matrices, delta) -> {
            Vector3f playerPos = source.getPos(delta);
            Vector3f playerDim = source.getAABB().getDimensions();
            VertexConsumer.WORLD_MAIN.consume(GeometryHelper.capsule(matrices, playerPos.x, playerPos.y, playerPos.z, playerDim.y, playerDim.x / 2f, 12, Colors.PINK.argb));
        });
        player.getAbilities().godMode(false).canBuild(false);
    }

    private static class Hud2 extends Hud {
        @Override
        public void init() {
            super.init();
            health.setWidth(200);
        }

        @Override
        public void render(MatrixStack matrices, float delta) {
            drawHealth(matrices, Client.getInstance().world.player, delta);
        }

        @Override
        protected void drawHealth(MatrixStack matrices, Player player, float delta) {
            matrices.pushMatrix();

            Window w = Client.getInstance().window;
            matrices.translate(w.getGUIWidth() / 2f - 100, w.getGUIHeight() - 10f, 0);

            Text.empty().withStyle(Style.EMPTY.outlined(true).guiStyle(HUD_STYLE))
                    .append(Text.of("\u2764").withStyle(Style.EMPTY.color(Colors.RED)))
                    .append(" ")
                    .append(player.getHealth())
                    .render(VertexConsumer.MAIN, matrices, 0, -1, Alignment.BOTTOM_LEFT);

            float hp = player.getHealthProgress();
            health.setProgress(hp);
            health.render(matrices, w.mouseX, w.mouseY, delta);

            matrices.popMatrix();
        }
    }
}
