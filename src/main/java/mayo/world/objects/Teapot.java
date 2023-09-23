package mayo.world.objects;

import mayo.Client;
import mayo.model.ModelManager;
import mayo.render.MatrixStack;
import mayo.utils.Resource;
import mayo.utils.Rotation;
import mayo.world.WorldObject;
import org.joml.Vector3f;

public class Teapot extends WorldObject {

    private static final Resource RESOURCE = new Resource("models/objects/teapot/teapot.obj");

    public Teapot(Vector3f position) {
        super(ModelManager.load(RESOURCE));
        this.transform.setScale(1 / 10f);
        this.transform.setPos(position.mul(10f));
    }

    @Override
    public void render(MatrixStack matrices, float delta) {
        matrices.push();
        matrices.rotate(Rotation.Y.rotationDeg(Client.getInstance().ticks + delta));
        super.render(matrices, delta);
        matrices.pop();
    }
}
