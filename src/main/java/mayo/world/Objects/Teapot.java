package mayo.world.Objects;

import mayo.model.ModelManager;
import mayo.utils.Resource;
import mayo.world.WorldObject;
import org.joml.Vector3f;

public class Teapot extends WorldObject {

    private static final Resource RESOURCE = new Resource("models/teapot/teapot.obj");

    public Teapot(Vector3f position) {
        super(ModelManager.load(RESOURCE));
        this.transform.setScale(1 / 8f);
        this.transform.setPos(position.mul(8f));
    }
}
