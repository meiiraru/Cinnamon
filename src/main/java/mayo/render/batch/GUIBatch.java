package mayo.render.batch;

import mayo.render.shader.Shaders;

public class GUIBatch extends GenericBatch {
    public GUIBatch() {
        super(Shaders.GUI);
    }
}
