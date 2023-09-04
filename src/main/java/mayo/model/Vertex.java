package mayo.model;

import mayo.utils.ColorUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Vertex {

    private final Vector3f pos, color, normal;
    private final Vector2f uv;

    public Vertex(float x, float y, float z) {
        this(x, y, z, 0f, 0f);
    }

    public Vertex(float x, float y, float z, float u, float v) {
        this(x, y, z, u, v, 1f, 1f, 1f);
    }

    public Vertex(float x, float y, float z, float u, float v, int rgb) {
        this(x, y, z, u, v, 1f, 1f, 1f, 1f, 1f, 1f);
        color.set(ColorUtils.intToRGB(rgb));
    }

    public Vertex(float x, float y, float z, float u, float v, float r, float g, float b) {
        this(x, y, z, u, v, r, g, b, 1f, 1f, 1f);
    }

    public Vertex(float x, float y, float z, float u, float v, float r, float g, float b, float nx, float ny, float nz) {
        this.pos = new Vector3f(x, y, z);
        this.uv = new Vector2f(u, v);
        this.color = new Vector3f(r, g, b);
        this.normal = new Vector3f(nx, ny, nz);
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
}
