package cinnamon.model;

import cinnamon.render.MatrixStack;
import cinnamon.utils.ColorUtils;
import org.joml.*;

public class Vertex implements Comparable<Vertex> {
    public static final Vector3f DEFAULT_TANGENT = new Vector3f(0, 0, 1);
    public static final Vector3f DEFAULT_NORMAL = new Vector3f(0, 0, 1);
    public static final Vector2f DEFAULT_UV = new Vector2f(0, 0);
    public static final Vector4f DEFAULT_COLOR = new Vector4f(1, 1, 1, 1);

    private final Vector4f
            color = new Vector4f(1, 1, 1, 1);
    private final Vector3f
            pos = new Vector3f(),
            normal = new Vector3f(0, 0, -1),
            tangent = new Vector3f(0, 0, 1);
    private final Vector2f uv = new Vector2f();
    private int index = -1;

    private Vertex(float x, float y, float z) {
        this.pos.set(x, y, z);
    }

    public static Vertex of(float x, float y, float z) {
        return new Vertex(x, y, z);
    }

    public static Vertex of(float x, float y) {
        return of(x, y, 0);
    }

    public static Vertex of(Vector3f pos) {
        return of(pos.x, pos.y, pos.z);
    }

    public static Vertex of(Vector2f pos) {
        return of(pos.x, pos.y);
    }

    public Vertex color(int color) {
        Vector4f rgba = ColorUtils.argbIntToRGBA(color);
        return color(rgba.x, rgba.y, rgba.z, rgba.w);
    }

    public Vertex color(float r, float g, float b) {
        return this.color(r, g, b, 1f);
    }

    public Vertex color(Vector3f color) {
        return color(color.x, color.y, color.z);
    }

    public Vertex color(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
        return this;
    }

    public Vertex color(Vector4f color) {
        return color(color.x, color.y, color.z, color.w);
    }

    public Vertex normal(float x, float y, float z) {
        this.normal.set(x, y, z).normalize();
        return this;
    }

    public Vertex normal(Vector3f normal) {
        return normal(normal.x, normal.y, normal.z);
    }

    public Vertex tangent(float x, float y, float z) {
        this.tangent.set(x, y, z).normalize();
        return this;
    }

    public Vertex tangent(Vector3f tangent) {
        return tangent(tangent.x, tangent.y, tangent.z);
    }

    public Vertex uv(float u, float v) {
        this.uv.set(u, v);
        return this;
    }

    public Vertex uv(Vector2f uv) {
        return uv(uv.x, uv.y);
    }

    public Vertex index(int index) {
        this.index = index;
        return this;
    }

    public Vertex mul(MatrixStack matrices) {
        MatrixStack.Matrices mat = matrices.peek();
        return mulPosition(mat.pos()).mulNormal(mat.normal());
    }

    public Vertex mulPosition(Matrix4f mat) {
        pos.mulPosition(mat);
        return this;
    }

    public Vertex mulNormal(Matrix3f mat) {
        normal.mul(mat);
        return this;
    }

    public Vector3f getPosition() {
        return this.pos;
    }

    public Vector2f getUV() {
        return uv;
    }

    public Vector4f getColor() {
        return this.color;
    }

    public Vector3f getNormal() {
        return this.normal;
    }

    public Vector3f getTangent() {
        return tangent;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return String.format("[Pos]: %s, %s, %s [UV]: %s, %s [Color]: %s, %s, %s [Normal]: %s, %s, %s [Tangent]: %s, %s, %s",
                pos.x, pos.y, pos.z, uv.x, uv.y, color.x, color.y, color.z, normal.x, normal.y, normal.z, tangent.x, tangent.y, tangent.z);
    }

    @Override
    public int compareTo(Vertex o) {
        return Integer.compare(index, o.index);
    }
}
