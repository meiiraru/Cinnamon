package mayo.world;

import mayo.model.Transform;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.shader.Shader;
import org.joml.Vector3f;

public abstract class WorldObject {

    public final Transform transform = new Transform();
    protected final Model model;
    private final Vector3f dimensions;

    public WorldObject(Model model) {
        this.model = model;
        this.dimensions = model.getMesh().getBoundingBox();
    }

    public void render(MatrixStack matrices, float delta) {
        matrices.push();
        matrices.mulPos(transform.getPositionMatrix());

        Shader.activeShader.setMatrixStack(matrices);
        model.render();

        matrices.pop();
    }

    public Vector3f getDimensions() {
        return dimensions;
    }
}
