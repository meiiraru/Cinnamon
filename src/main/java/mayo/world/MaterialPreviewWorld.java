package mayo.world;

import mayo.gui.Toast;
import mayo.model.ModelManager;
import mayo.registry.MaterialRegistry;
import mayo.render.*;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.render.texture.Texture;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.AABB;
import mayo.utils.Alignment;
import mayo.utils.Colors;
import mayo.utils.Resource;
import org.lwjgl.glfw.GLFW;

public class MaterialPreviewWorld extends WorldClient {

    private static final Model
            SPHERE = new Model(ModelManager.load(new Resource("models/terrain/sphere/sphere.obj")).getMesh()),
            BOX = new Model(ModelManager.load(new Resource("models/terrain/box/box.obj")).getMesh());

    private boolean useDeferredRendering = true;

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void tempLoad() {
        //super.tempLoad();
        player.updateMovementFlags(false, false, true);
        player.setPos(-2f, 2f, -2f);
        player.rotate(0f, 135f);
        scheduledTicks.add(lights::clear);
        skyBox.renderSun = false;

        //MaterialRegistry[] values = MaterialRegistry.values();
        //int grid = (int) Math.ceil(Math.sqrt(values.length));
        //
        //for (int i = 0; i < values.length; i++) {
        //    addLight(new Light().pos(i % grid * 6f + 1f, 1f, (int) (i / grid * 3f) + 0.5f).color(Colors.randomRainbow().rgb));
        //}
    }

    @Override
    protected void renderShadows(Camera camera, MatrixStack matrices, float delta) {
        //super.renderShadows(camera, matrices, delta);
    }

    @Override
    protected void renderWorld(Camera camera, MatrixStack matrices, float delta) {
        //setup pbr renderer
        Shader prevShdr = Shader.activeShader;

        Shader sh;
        if (useDeferredRendering) {
            sh = WorldRenderer.prepareGeometry();
            sh.setVec3("camPos", client.camera.getPos());
        } else {
            sh = Shaders.WORLD_MODEL_PBR.getShader().use();
            applyWorldUniforms(sh);
            applyShadowUniforms(sh);
            applySkyboxUniforms(sh);
        }
        sh.setup(client.camera.getPerspectiveMatrix(), client.camera.getViewMatrix());

        //render materials
        MaterialRegistry[] values = MaterialRegistry.values();
        int grid = (int) Math.ceil(Math.sqrt(values.length));

        //render
        for (int i = 0; i < values.length; i++) {
            matrices.push();
            matrices.translate(i % grid * 6f, 0f, (int) (i / grid * 3f));

            int texCount = MaterialApplier.applyMaterial(values[i].material);
            boolean visible = false;

            AABB sphereBB = SPHERE.getMeshAABB();
            sphereBB.applyMatrix(matrices.peek().pos());
            if (camera.isInsideFrustum(sphereBB)) {
                sh.applyMatrixStack(matrices);
                SPHERE.renderWithoutMaterial();
                visible = true;
            }

            matrices.translate(3f, 0f, 0f);

            AABB boxBB = BOX.getMeshAABB();
            boxBB.applyMatrix(matrices.peek().pos());
            if (camera.isInsideFrustum(boxBB)) {
                sh.applyMatrixStack(matrices);
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

        if (useDeferredRendering)
            WorldRenderer.render(s -> {
                applyWorldUniforms(s);
                applySkyboxUniforms(s);
            });

        if (prevShdr != null) prevShdr.use();
        super.renderWorld(camera, matrices, delta);
    }

    @Override
    public void onWindowResize(int width, int height) {
        super.onWindowResize(width, height);
        WorldRenderer.resize(width, height);
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        super.keyPress(key, scancode, action, mods);
        if (key == GLFW.GLFW_KEY_F && action == GLFW.GLFW_PRESS) {
            useDeferredRendering = !useDeferredRendering;
            Toast.addToast(Text.of("Deferred Rendering: " + useDeferredRendering), client.font);
        }
    }
}
