package mayo.model;

import mayo.utils.ColorUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Vertex implements Comparable<Vertex> {

    private final Vector3f
            pos = new Vector3f(),
            color = new Vector3f(1f, 1f, 1f),
            normal = new Vector3f(1f, 1f, 1f);
    private final Vector2f uv = new Vector2f();
    private int index = -1;

    private Vertex(float x, float y, float z) {
        this.pos.set(x, y, z);
    }

    public static Vertex of(float x, float y, float z) {
        return new Vertex(x, y, z);
    }

    public Vertex color(int color) {
        Vector3f rgb = ColorUtils.intToRGB(color);
        return color(rgb.x, rgb.y, rgb.z);
    }

    public Vertex color(float r, float g, float b) {
        this.color.set(r, g, b);
        return this;
    }

    public Vertex normal(float x, float y, float z) {
        this.normal.set(x, y, z);
        return this;
    }

    public Vertex uv(float u, float v) {
        this.uv.set(u, v);
        return this;
    }

    public Vertex index(int index) {
        this.index = index;
        return this;
    }

    public Vertex mulPosition(Matrix4f mat) {
        pos.mulPosition(mat);
        return this;
    }

    public Vector3f getPosition() {
        return this.pos;
    }

    public Vector2f getUV() {
        return uv;
    }

    public Vector3f getColor() {
        return this.color;
    }

    public Vector3f getNormal() {
        return this.normal;
    }

    @Override
    public String toString() {
        return String.format("[Pos]: %s, %s, %s [UV]: %s, %s [Color]: %s, %s, %s [Normal] %s, %s, %s",
                pos.x, pos.y, pos.z, uv.x, uv.y, color.x, color.y, color.z, normal.x, normal.y, normal.z);
    }

    @Override
    public int compareTo(Vertex o) {
        return Integer.compare(index, o.index);
    }
}
