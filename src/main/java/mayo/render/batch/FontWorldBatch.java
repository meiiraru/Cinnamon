package mayo.render.batch;

import mayo.Client;
import mayo.render.shader.Attributes;
import mayo.render.shader.Shaders;

public class FontWorldBatch extends Batch {

    public FontWorldBatch() {
        super(Shaders.FONT_WORLD, 6, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA | Attributes.NORMAL);
    }

    @Override
    protected void preRender() {
        super.preRender();
        Client.getInstance().world.uploadLightUniforms(this.shader);
    }
}
