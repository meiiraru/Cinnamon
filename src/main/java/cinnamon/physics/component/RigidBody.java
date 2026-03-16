package cinnamon.physics.component;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DQuaternionC;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DGeom;

public class RigidBody extends Component {

    private final DBody body;
    private final DGeom geom;

    public RigidBody(DBody body, DGeom geom) {
        this.body = body;
        this.geom = geom;
    }

    public DBody getBody() {
        return body;
    }

    public DGeom getGeom() {
        return geom;
    }

    /** Read ODE pose back into the Transform. Uses body when present else geom. */
    public void syncTransform(Transform transform) {
        if (body != null) {
            DVector3C pos = body.getPosition();
            transform.getPosition().set(pos.get0(), pos.get1(), pos.get2());

            DQuaternionC quat = body.getQuaternion();
            transform.getRotation().set((float) quat.get1(), (float) quat.get2(), (float) quat.get3(), (float) quat.get0());
        } else if (geom != null) {
            DVector3C pos = geom.getPosition();
            transform.getPosition().set(pos.get0(), pos.get1(), pos.get2());

            DQuaternionC quat = geom.getQuaternion();
            transform.getRotation().set((float) quat.get1(), (float) quat.get2(), (float) quat.get3(), (float) quat.get0());
        }
    }

    /** Push Transform pose into ODE. Writes to body when present else geom. */
    public void applyTransform(Transform transform) {
        Vector3f p = transform.getPosition();
        Quaternionf q = transform.getRotation();
        // JOML stores (x, y, z, w)
        DQuaternion dq = new DQuaternion(q.w, q.x, q.y, q.z);
        if (body != null) {
            body.setPosition(p.x, p.y, p.z);
            body.setQuaternion(dq);
        } else if (geom != null) {
            geom.setPosition(p.x, p.y, p.z);
            geom.setQuaternion(dq);
        }
    }

    @Override
    public ComponentType getType() {
        return ComponentType.RIGIDBODY;
    }
}
