package mayo.render.shader;

import mayo.Client;

public enum Shaders {
    MAIN,
    FONT,
    MODEL;

    private final Shader shader;

    Shaders() {
        this.shader = new Shader(Client.NAMESPACE, this.name().toLowerCase() + ".glsl");
    }

    public Shader getShader() {
        return shader;
    }
}
