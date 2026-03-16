package cinnamon.physics.component;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Transform extends Component {

    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f position = new Vector3f();
    private final Vector3f scale = new Vector3f(1, 1, 1);

    public Quaternionf getRotation() {
        return rotation;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getScale() {
        return scale;
    }

    public void copyTo(Transform target) {
        target.getPosition().set(this.position);
        target.getRotation().set(this.rotation);
        target.getScale().set(this.scale);
    }

    @Override
    public ComponentType getType() {
        return ComponentType.TRANSFORM;
    }
}
