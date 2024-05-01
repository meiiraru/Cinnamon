package mayo.world;

import mayo.model.ModelManager;
import mayo.registry.MaterialRegistry;
import mayo.render.Camera;
import mayo.render.MaterialApplier;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.render.texture.Texture;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Alignment;
import mayo.utils.Colors;
import mayo.utils.Resource;

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
        scheduledTicks.add(lights::clear);
        skyBox.renderSun = false;
    }

    @Override
    protected void renderShadows(Camera camera, MatrixStack matrices, float delta) {
        //super.renderShadows(camera, matrices, delta);
    }

    @Override
    protected void renderWorld(Camera camera, MatrixStack matrices, float delta) {
        super.renderWorld(camera, matrices, delta);

        //setup pbr shader
        Shader sh = Shaders.WORLD_MODEL_PBR.getShader().use();
        sh.setup(client.camera.getPerspectiveMatrix(), client.camera.getViewMatrix());

        applyWorldUniforms(sh);
        applyShadowUniforms(sh);
        applySkyboxUniforms(sh);

        //render materials
        MaterialRegistry[] values = MaterialRegistry.values();
        int grid = (int) Math.ceil(Math.sqrt(values.length));

        //render
        for (int i = 0; i < values.length; i++) {
            matrices.push();
            matrices.translate(i % grid * 6f, 0f, (int) (i / grid * 3f));

            int texCount = MaterialApplier.applyMaterial(values[i].material);

            sh.applyMatrixStack(matrices);
            SPHERE.renderWithoutMaterial();

            matrices.translate(3f, 0f, 0f);
            sh.applyMatrixStack(matrices);
            BOX.renderWithoutMaterial();

            matrices.translate(-1f, 1.5f, 0.5f);
            matrices.scale(-1 / 48f);
            camera.billboard(matrices);
            client.font.render(
                    VertexConsumer.WORLD_FONT, matrices,
                    0f, 0f,
                    Text.of(values[i].name()).withStyle(Style.EMPTY.shadow(true).shadowColor(Colors.PURPLE)),
                    Alignment.CENTER
            );

            Texture.unbindAll(texCount);
            matrices.pop();
        }
    }
}
