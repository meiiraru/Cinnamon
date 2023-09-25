package mayo.render.batch;

import mayo.render.shader.Attributes;
import mayo.render.shader.Shaders;

public class MainBatch extends GenericBatch {
    public MainBatch() {
        super(Shaders.GENERIC, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA | Attributes.NORMAL);
    }
}
