package mayo.render.batch;

import mayo.model.Vertex;
import mayo.render.shader.Shaders;

public abstract class GenericBatch extends Batch {

    public GenericBatch(Shaders shader, int flags) {
        super(shader, 6, flags);
    }

    @Override
    protected void pushVertex(Vertex vertex, int textureID) {
        fillVertexBuffers(vertex);

        //push pos
        buffer.put(pos.x);
        buffer.put(pos.y);
        buffer.put(pos.z);

        //push texture
        buffer.put(textureID);
        buffer.put(uv.x);
        buffer.put(uv.y);

        //push color
        buffer.put(color.x);
        buffer.put(color.y);
        buffer.put(color.z);
        buffer.put(color.w);
    }
}