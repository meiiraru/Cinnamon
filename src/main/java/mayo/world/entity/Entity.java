package mayo.world.entity;

import mayo.Client;
import mayo.model.obj.Mesh;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import mayo.world.World;
import org.joml.Vector3f;

public abstract class Entity {

    private final Mesh model;
    private final World world;
    private final Vector3f dimensions;
    private final Vector3f pos = new Vector3f();

    public Entity(Mesh model, World world, Vector3f dimensions) {
        this.model = model;
        this.world = world;
        this.dimensions = dimensions;
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        matrices.translate(pos);
        Shader.activeShader.setModelMatrix(matrices.peek());
        model.render();

        if (shouldRenderText())
            renderTexts(matrices, delta);

        matrices.pop();
    }

    protected void renderTexts(MatrixStack matrices, float delta) {}

    public boolean shouldRenderText() {
        Vector3f cam = Client.getInstance().camera.getPos();
        return cam.distanceSquared(pos) < 256;
    }

    public void setPosition(float x, float y, float z) {
        this.pos.set(x, y, z);
    }

    public Vector3f getDimensions() {
        return dimensions;
    }
}
