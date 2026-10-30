package cinnamon.animation;

import org.joml.Vector3f;

public class Keyframe implements Comparable<Keyframe> {

    private final int time;
    private final Vector3f preValue, value;
    private final Interpolation interpolation;

    public Keyframe(int time, Vector3f value, Interpolation interpolation) {
        this(time, value, null, interpolation);
    }

    public Keyframe(int time, Vector3f value, Vector3f preValue, Interpolation interpolation) {
        this.time = time;
        this.value = value;
        this.preValue = preValue;
        this.interpolation = interpolation;
    }

    public int getTime() {
        return time;
    }

    public Vector3f getValue() {
        return value;
    }

    public Vector3f getPreValue() {
        return preValue == null ? value : preValue;
    }

    public Interpolation getInterpolation() {
        return interpolation;
    }

    @Override
    public int compareTo(Keyframe o) {
        return Integer.compare(time, o.getTime());
    }
}
