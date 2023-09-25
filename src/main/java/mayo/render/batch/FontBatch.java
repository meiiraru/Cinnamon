package mayo.render.batch;

import mayo.render.shader.Attributes;
import mayo.render.shader.Shaders;

public class FontBatch extends Batch {

    public FontBatch() {
        super(Shaders.FONT, 6, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA);
    }
}
