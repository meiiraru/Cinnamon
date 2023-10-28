package mayo.world;

import mayo.model.ModelManager;
import mayo.render.Camera;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.shader.Shader;
import mayo.utils.Resource;

import static org.lwjgl.opengl.GL11.glDepthMask;

public class SkyBox {

    private static final Model MODEL = ModelManager.load(new Resource("models/skybox/skybox.obj"));

    public void render(Camera camera, MatrixStack matrices) {
        matrices.push();
        matrices.translate(camera.getPos());

        //disable depth
        glDepthMask(false);

        //render model
        Shader.activeShader.applyMatrixStack(matrices);
        MODEL.render();

        glDepthMask(true);
        matrices.pop();
    }
}
