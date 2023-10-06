package mayo.world;

import mayo.model.ModelManager;
import mayo.render.Camera;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.shader.Shader;
import mayo.utils.Resource;

public class SkyBox {

    private static final Model MODEL = ModelManager.load(new Resource("models/skybox/skybox.obj"));

    public void render(Camera camera, MatrixStack matrices) {
        matrices.push();
        matrices.translate(camera.getPos());

        matrices.scale((float) (Camera.FAR_PLANE / 2f / Math.sqrt(2)));

        //render model
        Shader.activeShader.setMatrixStack(matrices);
        MODEL.render();

        matrices.pop();
    }
}
