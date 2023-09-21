package mayo.render.batch;

import mayo.model.Vertex;
import mayo.render.shader.Attributes;
import mayo.render.shader.Shaders;

public class FontBatch extends Batch {

    public FontBatch() {
        super(Shaders.FONT, 6, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA | Attributes.INDEX);
    }

    @Override
    protected void fillVertexBuffers(Vertex vertex) {
        pos.set(vertex.getPosition());
        uv.set(vertex.getUV());
        color.set(vertex.getColor());
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

        //put index
        buffer.put(vertex.getIndex());
    }
}
