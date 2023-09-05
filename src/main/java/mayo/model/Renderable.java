package mayo.model;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;

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

    public void pushVertices(FloatBuffer target, int texID) {
        for (Vertex vertex : this.vertices) {
            pos.set(vertex.getPosition()).mulPosition(transform.getPositionMatrix());
            uv.set(vertex.getUV()).add(transform.getUV());
            color.set(vertex.getColor()).mul(transform.getColor());
            normal.set(vertex.getNormal()).mul(transform.getNormalMatrix());

            target.put(pos.x);
            target.put(pos.y);
            target.put(pos.z);

            target.put(texID);
            target.put(uv.x);
            target.put(uv.y);

            target.put(color.x);
            target.put(color.y);
            target.put(color.z);

            target.put(normal.x);
            target.put(normal.y);
            target.put(normal.z);
        }
    }

    public int faceCount() {
        return this.vertices.length / 4;
    }
}
