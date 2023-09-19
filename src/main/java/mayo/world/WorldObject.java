package mayo.world;

import mayo.model.Transform;
import mayo.model.obj.Mesh;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import org.joml.Vector3f;

public abstract class WorldObject {

    public final Transform transform = new Transform();
    protected final Mesh mesh;
    private final Vector3f dimensions;

    public WorldObject(Mesh mesh) {
        this.mesh = mesh;
        this.dimensions = mesh.getBBMax().sub(mesh.getBBMin(), new Vector3f());
    }

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        Shader.activeShader.setModelMatrix(matrices.peek().mul(transform.getPositionMatrix()));
        mesh.render();

        matrices.pop();
    }

    public Vector3f getDimensions() {
        return dimensions;
    }
}
