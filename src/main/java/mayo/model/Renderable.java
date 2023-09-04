package mayo.model;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class Renderable {

    //properties
    public final Transform transform = new Transform();
    public final int textureID;
    protected final Vertex[] vertices;

    //transform data
    private static final Vector3f
            pos = new Vector3f(),
            normal = new Vector3f(),
            color = new Vector3f();
    private static final Vector2f
            uv = new Vector2f();

    public Renderable(Mesh model) {
        this(model.vertices(), model.texture() == null ? -1 : model.texture().getID());
    }

    public Renderable(Vertex[] vertices, int textureID) {
        this.vertices = vertices;
        this.textureID = textureID;
    }

    public void pushVertices(float[] target, int offset, int texID) {
        for (Vertex vertex : this.vertices) {
            pos.set(vertex.getPosition()).mulPosition(transform.getPositionMatrix());
            uv.set(vertex.getUV()).add(transform.getUV());
            color.set(vertex.getColor()).mul(transform.getColor());
            normal.set(vertex.getNormal()).mul(transform.getNormalMatrix());

            target[offset++] = pos.x;
            target[offset++] = pos.y;
            target[offset++] = pos.z;

            target[offset++] = texID;
            target[offset++] = uv.x;
            target[offset++] = uv.y;

            target[offset++] = color.x;
            target[offset++] = color.y;
            target[offset++] = color.z;

            target[offset++] = normal.x;
            target[offset++] = normal.y;
            target[offset++] = normal.z;
        }
    }

    public int faceCount() {
        return this.vertices.length / 4;
    }
}
