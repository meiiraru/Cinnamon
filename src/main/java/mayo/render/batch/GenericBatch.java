package mayo.render.batch;

import mayo.render.shader.Attributes;
import mayo.render.shader.Shaders;

public abstract class GenericBatch extends Batch {

    public GenericBatch(Shaders shader, int flags) {
        super(shader, 6, flags);
    }

    public GenericBatch(Shaders shader) {
        this(shader, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA);
    }
}
