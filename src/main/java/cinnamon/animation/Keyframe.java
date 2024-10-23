package cinnamon.animation;

import org.joml.Vector3f;

public class Keyframe implements Comparable<Keyframe> {

    private final int time;
    private final Vector3f preValue, value;
    private final boolean catmullrom;

    public Keyframe(int time, Vector3f value, boolean catmullrom) {
        this(time, value, null, catmullrom);
    }

    public Keyframe(int time, Vector3f value, Vector3f preValue, boolean catmullrom) {
        this.time = time;
        this.value = value;
        this.preValue = preValue;
        this.catmullrom = catmullrom;
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

    public boolean isCatmullrom() {
        return catmullrom;
    }

    @Override
    public int compareTo(Keyframe o) {
        return Integer.compare(time, o.getTime());
    }
}
