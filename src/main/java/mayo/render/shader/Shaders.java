package mayo.render.shader;

import mayo.utils.Resource;

public enum Shaders {
    GUI,
    FONT,
    FONT_FLAT,
    MODEL,
    MODEL_FLAT,
    MAIN,
    LINES;

    private final Shader shader;

    Shaders() {
        this.shader = new Shader(new Resource("shaders/" + this.name().toLowerCase() + ".glsl"));
    }

    public Shader getShader() {
        return shader;
    }

    public static void freeAll() {
        for (Shaders shader : values())
            shader.getShader().free();
    }
}
