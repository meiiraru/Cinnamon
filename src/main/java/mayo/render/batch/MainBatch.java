package mayo.render.batch;

import mayo.model.Vertex;
import mayo.render.shader.Attributes;
import mayo.render.shader.Shaders;

public class MainBatch extends GenericBatch {
    public MainBatch() {
        super(Shaders.GENERIC, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA | Attributes.NORMAL);
    }

    @Override
    protected void fillVertexBuffers(Vertex vertex) {
        super.fillVertexBuffers(vertex);
        normal.set(vertex.getNormal());
    }

    @Override
    protected void pushVertex(Vertex vertex, int textureID) {
        super.pushVertex(vertex, textureID);

        //push normal
        buffer.put(normal.x);
        buffer.put(normal.y);
        buffer.put(normal.z);
    }
}
