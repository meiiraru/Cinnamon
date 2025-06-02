package cinnamon.vr;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public record XrHandTransform(Vector3f pos, Quaternionf rot, Vector3f vel, Vector3f angularVel) {
    public XrHandTransform() {
        this(new Vector3f(), new Quaternionf(), new Vector3f(), new Vector3f());
    }

    public void setFrom(XrHandTransform o) {
        this.pos.set(o.pos);
        this.rot.set(o.rot);
        this.vel.set(o.vel);
        this.angularVel.set(o.angularVel);
    }
}
