package cinnamon.world.world;

import cinnamon.model.ModelManager;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MaterialApplier;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.texture.Texture;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.AABB;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;

public class MaterialPreviewWorld extends WorldClient {

    private static final ModelRenderer
            SPHERE = ModelManager.load(new Resource("models/terrain/sphere/sphere.obj")),
            BOX = ModelManager.load(new Resource("models/terrain/box/box.obj"));

    @Override
    protected void tempLoad() {
        //super.tempLoad();
        player.updateMovementFlags(false, false, true);
        player.setPos(-2f, 2f, -2f);
        player.rotate(0f, 135f);

        //MaterialRegistry[] values = MaterialRegistry.values();
        //int grid = (int) Math.ceil(Math.sqrt(values.length));
        //
        //for (int i = 0; i < values.length; i++) {
        //    addLight(new Light().pos(i % grid * 6f + 1f, 1f, (int) (i / grid * 3f) + 0.5f).color(Colors.randomRainbow().rgb));
        //}
    }

    @Override
    public int renderTerrain(Camera camera, MatrixStack matrices, float delta) {
        //render materials
        MaterialRegistry[] values = MaterialRegistry.values();
        int grid = (int) Math.ceil(Math.sqrt(values.length));

        //render
        int count = 0;
        for (int i = 0; i < values.length; i++) {
            matrices.pushMatrix();
            matrices.translate(i % grid * 6f, 0f, (float) (i / grid * 3));

            int texCount = MaterialApplier.applyMaterial(values[i].material);
            boolean visible = false;

            AABB sphereBB = SPHERE.getAABB();
            sphereBB.applyMatrix(matrices.peek().pos());
            if (camera.isInsideFrustum(sphereBB)) {
                if (texCount == -1) SPHERE.render(matrices);
                else SPHERE.renderWithoutMaterial(matrices);
                visible = true;
                count++;
            }

            matrices.translate(3f, 0f, 0f);

            AABB boxBB = BOX.getAABB();
            boxBB.applyMatrix(matrices.peek().pos());
            if (camera.isInsideFrustum(boxBB)) {
                if (texCount == -1) BOX.render(matrices);
                else BOX.renderWithoutMaterial(matrices);
                visible = true;
                count++;
            }

            if (visible && !hudHidden()) {
                matrices.translate(-1.5f, 1.5f, 0f);
                matrices.scale(-1 / 48f);
                camera.billboard(matrices);
                Text.translated("material." + values[i].name().toLowerCase()).withStyle(Style.EMPTY.shadow(true).shadowColor(Colors.PURPLE)).render(VertexConsumer.WORLD_MAIN, matrices, 0f, 0f, Alignment.BOTTOM_CENTER);
            }

            Texture.unbindAll(texCount);
            matrices.popMatrix();
        }

        return super.renderTerrain(camera, matrices, delta) + count;
    }
}
