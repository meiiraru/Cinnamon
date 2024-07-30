package cinnamon.world;

import cinnamon.model.ModelManager;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.*;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.AABB;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;

public class MaterialPreviewWorld extends WorldClient {

    private static final Model
            SPHERE = new Model(ModelManager.load(new Resource("models/terrain/sphere/sphere.obj")).getMesh()),
            BOX = new Model(ModelManager.load(new Resource("models/terrain/box/box.obj")).getMesh());

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
    protected void renderWorld(Camera camera, MatrixStack matrices, float delta) {
        //render materials
        Shader s = Shader.activeShader;
        MaterialRegistry[] values = MaterialRegistry.values();
        int grid = (int) Math.ceil(Math.sqrt(values.length));

        //render
        for (int i = 0; i < values.length; i++) {
            matrices.push();
            matrices.translate(i % grid * 6f, 0f, (float) (i / grid * 3));

            int texCount = MaterialApplier.applyMaterial(values[i].material);
            boolean visible = false;

            AABB sphereBB = SPHERE.getMeshAABB();
            sphereBB.applyMatrix(matrices.peek().pos());
            if (camera.isInsideFrustum(sphereBB)) {
                s.applyMatrixStack(matrices);
                SPHERE.renderWithoutMaterial();
                visible = true;
            }

            matrices.translate(3f, 0f, 0f);

            AABB boxBB = BOX.getMeshAABB();
            boxBB.applyMatrix(matrices.peek().pos());
            if (camera.isInsideFrustum(boxBB)) {
                s.applyMatrixStack(matrices);
                BOX.renderWithoutMaterial();
                visible = true;
            }

            if (visible) {
                matrices.translate(-1f, 1.5f, 0.5f);
                matrices.scale(-1 / 48f);
                camera.billboard(matrices);
                client.font.render(
                        VertexConsumer.WORLD_FONT, matrices,
                        0f, 0f,
                        Text.of(values[i].name()).withStyle(Style.EMPTY.shadow(true).shadowColor(Colors.PURPLE)),
                        Alignment.CENTER
                );
            }

            Texture.unbindAll(texCount);
            matrices.pop();
        }

        super.renderWorld(camera, matrices, delta);
    }
}
