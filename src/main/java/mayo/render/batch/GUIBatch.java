package mayo.render.batch;

import mayo.render.shader.Attributes;
import mayo.render.shader.Shaders;

public class GUIBatch extends GenericBatch {
    public GUIBatch() {
        super(Shaders.GUI, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA);
    }
}
