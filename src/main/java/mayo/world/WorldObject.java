package mayo.world;

import mayo.model.Transform;
import mayo.model.obj.Mesh;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;

public abstract class WorldObject {

    public final Transform transform = new Transform();
    protected final Mesh mesh;

    public WorldObject(Mesh mesh) {
        this.mesh = mesh;
    }

    public void render(Shader shader, MatrixStack matrices, float delta) {
        matrices.push();

        shader.setModelMatrix(matrices.peek().mul(transform.getPositionMatrix()));
        mesh.render();

        matrices.pop();
    }
}
