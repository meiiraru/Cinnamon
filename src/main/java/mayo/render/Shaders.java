package mayo.render;

import mayo.Client;

public enum Shaders {
    MAIN,
    FONT;

    private final Shader shader;

    Shaders() {
        this.shader = new Shader(Client.NAMESPACE, this.name().toLowerCase());
    }

    public Shader getShader() {
        return shader;
    }
}
