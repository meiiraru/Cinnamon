package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WaterRenderer;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.world.Hud;
import cinnamon.world.entity.living.Player;
import cinnamon.world.light.Light;
import cinnamon.world.light.Spotlight;
import cinnamon.world.terrain.PrimitiveTerrain;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.world.Hud.HUD_STYLE;
import static org.lwjgl.glfw.GLFW.*;

public class PrimitiveTestWorld extends WorldClient {

    private final List<PrimitiveTerrain> primitives = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private boolean renderNormals;

    @Override
    protected void tempLoad() {
        this.hud = new PrimitiveWorldHud();
        this.hud.init();

        int rooms = 10;
        float roomSize = 10f;
        float len = rooms * roomSize;
        float width = 10f;
        float height = 5f;

        //floor
        addTerrain(new PrimitiveTerrain(GeometryHelper.plane(client.matrices, 0f, 0f, 0f, width, len, 1, 1, Colors.DARK_GRAY.argb)));

        //walls
        //back
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(client.matrices, 0f, 0f, -0.5f, width, 1.5f, 0f, Colors.WHITE.argb)));
        //right
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(client.matrices, -0.5f, 0f, 0f, 0f, 1.5f, len, Colors.WHITE.argb)));
        //end
        addTerrain(new PrimitiveTerrain(GeometryHelper.box(client.matrices, 0f, 0f, len, width, 1.5f, len + 0.5f, Colors.WHITE.argb)));

        //pillars
        for (int i = 0; i <= len; i += 5) {
            float x = width * 0.8f;
            float y = height - 0.5f;
            float z = i - 0.25f;
            if (i % roomSize != 0) {
                x = width / 2f;
                y += 0.5f;
            }

            //upper pillars
            addTerrain(new PrimitiveTerrain(GeometryHelper.box(client.matrices, 0f, 0f, z, 0.5f, y, z + 0.5f, Colors.WHITE.argb)));
            addTerrain(new PrimitiveTerrain(GeometryHelper.box(client.matrices, 0f, y, z, x, y + 0.5f, z + 0.5f, Colors.WHITE.argb)));
        }

        //rooms
        float cx = width * 0.75f;
        float cz = roomSize / 2f;

        for (int i = 0; i < rooms; i++) {
            float z = i * roomSize;
            //left wall
            addTerrain(new PrimitiveTerrain(GeometryHelper.box(client.matrices, width * 0.8f, 0f, z, width, height, z + 1f, Colors.LIGHT_GRAY.argb)));
            //right wall
            addTerrain(new PrimitiveTerrain(GeometryHelper.box(client.matrices, width * 0.8f, 0f, z + roomSize - 1f, width, height, z + roomSize, Colors.LIGHT_GRAY.argb)));
            //back wall
            addTerrain(new PrimitiveTerrain(GeometryHelper.box(client.matrices, width, 0f, z, width + 1f, height, z + roomSize, Colors.LIGHT_GRAY.argb)));
            //ceiling
            addTerrain(new PrimitiveTerrain(GeometryHelper.box(client.matrices, width / 2f, height, z, width, height + 0.5f, z + roomSize, Colors.LIGHT_GRAY.argb)));

            //carpet
            PrimitiveTerrain carpet = new PrimitiveTerrain(GeometryHelper.plane(client.matrices, width / 2f, 0.01f, z + 1f, width, z + roomSize - 1f, 1, 1, Colors.RED.argb));
            addTerrain(carpet);
            carpet.getCollisionMask().setExcludeMask(0, true);

            //lamp
            addLight(new Spotlight().angle(40, 45).falloff(roomSize).pos(width / 2f, height - 0.5f, z + cz).direction(0.75f, -1, 0).color(Colors.WHITE.argb).intensity(2f));
        }

        //add each primitive geometry
        float y = 0.25f;
        float r = 0.5f;
        int q = 24;
        float p = 1f;
        boolean c = true;

        //plane
        labels.add("Plane");
        primitives.add(new PrimitiveTerrain(GeometryHelper.plane(client.matrices, cx - r, y, cz - r, cx + r, cz + r, 1, 1, Colors.WHITE.argb), false));
        cz += roomSize;

        //line
        labels.add("Line");
        primitives.add(new PrimitiveTerrain(GeometryHelper.line(client.matrices, cx, y, cz - r - r, cx, y + r + r, cz + r + r, 0.1f, Colors.WHITE.argb), false));
        primitives.add(new PrimitiveTerrain(GeometryHelper.line(client.matrices, cx - r - r, y + r + r, cz - r - r, cx + r + r, y + r * 3, cz + r + r, 0.1f, Colors.WHITE.argb), false));
        cz += roomSize;

        //box
        labels.add("Box");
        primitives.add(new PrimitiveTerrain(GeometryHelper.box(client.matrices, cx - r, y, cz - r, cx + r, y + r + r, cz + r, Colors.WHITE.argb), false));
        cz += roomSize;

        //pyramid
        labels.add("Pyramid");
        primitives.add(new PrimitiveTerrain(GeometryHelper.pyramid(client.matrices, cx - r, y, cz - r, cx + r, y + r + r, cz + r, c, Colors.WHITE.argb), false));
        cz += roomSize;

        //cone
        labels.add("Cone");
        primitives.add(new PrimitiveTerrain(GeometryHelper.cone(client.matrices, cx, y, cz, r + r, r, q, p, c, Colors.WHITE.argb), false));
        cz += roomSize;

        //cylinder
        labels.add("Cylinder");
        primitives.add(new PrimitiveTerrain(GeometryHelper.cylinder(client.matrices, cx, y, cz, r, r, r, q, p, c, Colors.WHITE.argb), false));
        cz += roomSize;

        //tube
        labels.add("Tube");
        primitives.add(new PrimitiveTerrain(GeometryHelper.tube(client.matrices, cx, y, cz, r, r, r / 2f, q, p, c, Colors.WHITE.argb), false));
        cz += roomSize;

        //sphere
        labels.add("Sphere");
        primitives.add(new PrimitiveTerrain(GeometryHelper.sphere(client.matrices, cx, y + r, cz, r, q, q, p, p, Colors.WHITE.argb), false));
        cz += roomSize;

        //capsule
        labels.add("Capsule");
        primitives.add(new PrimitiveTerrain(GeometryHelper.capsule(client.matrices, cx, y, cz, r * 3, r * 0.75f, q, q, p, Colors.WHITE.argb), false));
        cz += roomSize;

        //torus
        labels.add("Torus");
        primitives.add(new PrimitiveTerrain(GeometryHelper.torus(client.matrices, cx, y + r / 2, cz, r * 1.5f, r / 2, q, q, p, p, Colors.WHITE.argb), false));

        //add primitives
        for (PrimitiveTerrain pt : primitives) {
            pt.setMaterial(MaterialRegistry.DEBUG);
            pt.getCollisionMask().setExcludeMask(0, true);
            //pt.setColor(Colors.randomRainbow().argb);
            addTerrain(pt);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (isPaused())
            return;

        for (Light light : lights) {
            if (light.getType() != Light.Type.DIRECTIONAL) {
                //grab player distance to light
                float dist = light.getPos().distance(player.getPos());
                float f = dist < 10f ? (1f - (dist / 10f)) * 3f : 0f;
                light.intensity(f);
                light.glareIntensity(f);
            }
        }
    }

    @Override
    public int renderTerrain(Camera camera, MatrixStack matrices, float delta) {
        int sup = super.renderTerrain(camera, matrices, delta);

        //render labels
        float width = 10f;
        float roomSize = 10f;

        for (int i = 0; i < labels.size(); i++) {
            float z = i * roomSize + roomSize / 2f;
            matrices.pushMatrix();

            matrices.translate(width - 1f, 4f, z);
            matrices.scale(-1 / 48f);
            camera.billboard(matrices);

            Text.of(labels.get(i))
                    .withStyle(Style.EMPTY.outlined(true).guiStyle(HUD_STYLE))
                    .render(VertexConsumer.WORLD_MAIN_EMISSIVE, matrices, 0, 0, Alignment.CENTER, 48);

            matrices.popMatrix();
        }

        return sup;
    }

    @Override
    public void renderDebug(Camera camera, MatrixStack matrices, float delta) {
        if (renderNormals) {
            for (Terrain terrain : terrainManager.queryCustom(camera::isInsideFrustum)) {
                if (terrain instanceof PrimitiveTerrain pt)
                    TransparentWorld.renderNormals(matrices, pt.getVertices());
            }
            VertexConsumer.finishAllBatches(camera);
        }

        super.renderDebug(camera, matrices, delta);
    }

    @Override
    public void renderWater(Camera camera, MatrixStack matrices, float delta) {
        WaterRenderer.renderDefaultWaterPlane(camera, matrices, -0.02f, getSky().fogEnd);
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
        player.addRenderFeature((source, camera, matrices, delta) -> {
            Vector3f playerPos = source.getPos(delta);
            Vector3f playerDim = source.getAABB().getDimensions();
            VertexConsumer.WORLD_MAIN.consume(GeometryHelper.capsule(matrices, playerPos.x, playerPos.y, playerPos.z, playerDim.y, playerDim.x / 2f, 12, Colors.PINK.argb));
        });
        player.getAbilities().godMode(false).canBuild(false);

        player.setPos(3.75f, 0.5f, 1f);
        player.setRot(0f, 180f);
    }

    public static class PrimitiveWorldHud extends Hud {
        @Override
        public void init() {
            super.init();
            health.setWidth(200);
        }

        @Override
        public void render(MatrixStack matrices, float delta) {
            Client c = Client.getInstance();
            drawHealth(matrices, c.world.player, delta);
            VertexConsumer.finishAllBatches(c.camera);
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
