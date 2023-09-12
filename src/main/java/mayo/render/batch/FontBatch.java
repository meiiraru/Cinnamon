package mayo.render.batch;

import mayo.model.Vertex;
import mayo.render.shader.Attributes;
import mayo.render.shader.Shaders;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class FontBatch extends Batch {

    public FontBatch() {
        super(Shaders.FONT, 6, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR | Attributes.INDEX);
    }

    @Override
    protected void pushVertex(Vertex vertex, int textureID) {
        Vector3f pos = vertex.getPosition();
        Vector2f uv = vertex.getUV();
        Vector3f color = vertex.getColor();

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

        //put index
        buffer.put(vertex.getIndex());
    }
}
