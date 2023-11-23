package mayo.world;

import mayo.model.GeometryHelper;
import mayo.model.ModelManager;
import mayo.render.Camera;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.utils.Resource;
import mayo.utils.Rotation;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.glDepthMask;

public class SkyBox {

    private static final Model MODEL = ModelManager.load(new Resource("models/skybox/skybox.obj"));
    private static final Texture SUN = Texture.of(new Resource("textures/environment/sun.png"));
    private static final float SUN_ROLL = (float) Math.toRadians(30f);

    private final Vector3f sunDir = new Vector3f(1, 0, 0);
    private float sunAngle;

    public void render(Camera camera, MatrixStack matrices) {
        matrices.push();
        matrices.translate(camera.getPos());

        //disable depth
        glDepthMask(false);

        //render model
        Shader.activeShader.applyMatrixStack(matrices);
        MODEL.render();

        //translate sun
        matrices.rotate(Rotation.Y.rotationDeg(90f));
        matrices.rotate(Rotation.Z.rotation(SUN_ROLL));
        matrices.rotate(Rotation.X.rotationDeg(-sunAngle));
        matrices.translate(0, 0, 512);

        //render sun
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, -32, -32, 64, 64), SUN.getID());
        VertexConsumer.MAIN.finishBatch(camera.getPerspectiveMatrix(), camera.getViewMatrix());

        //cleanup rendering
        glDepthMask(true);
        matrices.pop();
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
