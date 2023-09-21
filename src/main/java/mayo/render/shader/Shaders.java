package mayo.render.shader;

import mayo.utils.Resource;

public enum Shaders {
    GUI,
    FONT,
    MODEL,
    GENERIC;

    private final Shader shader;

    Shaders() {
        this.shader = new Shader(new Resource("shaders/" + this.name().toLowerCase() + ".glsl"));
    }

    public Shader getShader() {
        return shader;
    }
}
