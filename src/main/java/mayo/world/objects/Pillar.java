package mayo.world.objects;

import mayo.model.ModelManager;
import mayo.utils.Resource;
import mayo.world.WorldObject;
import org.joml.Vector3f;

public class Pillar extends WorldObject {

    private static final Resource RESOURCE = new Resource("models/pillar/pillar.obj");

    public Pillar(Vector3f position) {
        super(ModelManager.load(RESOURCE));
        this.transform.setPos(position);
    }
}
