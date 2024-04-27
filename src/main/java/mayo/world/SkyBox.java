package mayo.world;

import mayo.model.GeometryHelper;
import mayo.model.ModelManager;
import mayo.render.*;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.utils.Resource;
import mayo.utils.Rotation;
import org.joml.Vector3f;

public class SkyBox {

    private static final CubeMap TEXTURE = CubeMap.of(new Resource("textures/environment/skybox/test"));
    private static final Model MODEL = ModelManager.load(new Resource("models/skybox/skybox.obj"));
    private static final Texture SUN = Texture.of(new Resource("textures/environment/sun.png"));
    private static final float SUN_ROLL = (float) Math.toRadians(30f);
    private static final float CLOUD_SPEED = (float) Math.PI / 2f;

    private final Vector3f sunDir = new Vector3f(1, 0, 0);
    private float sunAngle;

    public boolean
            renderSky = true,
            renderSun = true;

    public void render(Camera camera, MatrixStack matrices) {
        //move to camera position
        matrices.push();
        matrices.translate(camera.getPos());

        //render sky
        if (renderSky)
            renderSky(matrices);

        //render sun
        if (renderSun)
            renderSun(camera, matrices);

        //cleanup rendering
        matrices.pop();
    }

    private void renderSky(MatrixStack matrices) {
        //render model
        matrices.push();
        matrices.rotate(Rotation.Y.rotationDeg(sunAngle * CLOUD_SPEED));

        Shader.activeShader.applyMatrixStack(matrices);

        TEXTURE.bind();
        MODEL.renderWithoutMaterial();

        matrices.pop();
    }

    private void renderSun(Camera camera, MatrixStack matrices) {
        //translate sun
        matrices.rotate(Rotation.Y.rotationDeg(90f));
        matrices.rotate(Rotation.Z.rotation(SUN_ROLL));
        matrices.rotate(Rotation.X.rotationDeg(-sunAngle));
        matrices.translate(0, 0, 512);

        //render sun
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, -32, -32, 64, 64), SUN.getID());
        VertexConsumer.MAIN.finishBatch(camera.getPerspectiveMatrix(), camera.getViewMatrix());
    }

    public void setSunAngle(float angle) {
        this.sunAngle = angle;

        this.sunDir.set(1, 0, 0);
        this.sunDir.rotateZ((float) Math.toRadians(sunAngle));
        this.sunDir.rotateX(SUN_ROLL);
    }

    public Vector3f getSunDirection() {
        return sunDir;
    }
}
