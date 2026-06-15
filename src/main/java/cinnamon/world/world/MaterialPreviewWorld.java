package cinnamon.world.world;

import cinnamon.model.ModelManager;
import cinnamon.model.material.Material;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WaterRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import cinnamon.world.particle.Particle;
import cinnamon.world.particle.TextParticle;
import cinnamon.world.terrain.Terrain;
import org.joml.Math;
import org.joml.Vector3f;

public class MaterialPreviewWorld extends WorldClient {

    private static final ModelRenderer
            SPHERE = ModelManager.load(new Resource("models/terrain/sphere/sphere.obj")),
            BOX = ModelManager.load(new Resource("models/terrain/box/box.obj"));

    @Override
    protected void levelLoad() {
        //super.levelLoad();
        player.updateMovementFlags(false, false, true);
        player.setPos(-2f, 2f, -2f);
        player.rotate(0f, 135f, 0f);

        MaterialRegistry[] values = MaterialRegistry.values();
        int grid = (int) Math.ceil(Math.sqrt(values.length));
        Vector3f position = new Vector3f();

        for (int i = 0; i < values.length; i++) {
            Material mat = values[i].material;
            position.set(i % grid * 6f, 0f, (float) (i / grid * 3));

            Terrain sphere = TerrainRegistry.SPHERE.getFactory().get();
            sphere.setMaterial(mat);
            sphere.setPos(position);
            sphere.getCollisionMask().setExcludeMask(0, true);
            addTerrain(sphere);

            position.add(3f, 0f, 0f);

            Terrain box = TerrainRegistry.BOX.getFactory().get();
            box.setMaterial(mat);
            box.setPos(position);
            box.getCollisionMask().setExcludeMask(0, true);
            addTerrain(box);

            //addLight(new Spotlight().castsShadows(false).intensity(50).pos(position.x, position.y + 1.5f, position.z).color(Colors.WHITE.argb));

            position.add(-1f, 1.5f, 0.5f);

            Particle text = new TextParticle(Text.translated("material." + values[i].name().toLowerCase()).withStyle(Style.EMPTY.shadow(true).shadowColor(Colors.PURPLE)), -1, position);
            text.setMotion(0, 0, 0);
            addParticle(text);
        }
    }

    @Override
    public void renderWater(Camera camera, MatrixStack matrices, float delta) {
        WaterRenderer.renderDefaultWaterPlane(camera, matrices, -3f, getSky().fogEnd);
    }
}
