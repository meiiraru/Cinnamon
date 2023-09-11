package mayo.render.shader;

import mayo.utils.Resource;

public enum Shaders {
    MAIN,
    FONT,
    MODEL;

    private final Shader shader;

    Shaders() {
        this.shader = new Shader(new Resource("shaders/" + this.name().toLowerCase() + ".glsl"));
    }

    public Shader getShader() {
        return shader;
    }
}
