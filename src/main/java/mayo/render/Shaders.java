package mayo.render;

import mayo.Client;

import static mayo.render.Shader.*;

public enum Shaders {
    MAIN(POS | TEXTURE_ID | UV | COLOR | NORMAL),
    FONT(POS | TEXTURE_ID | UV | COLOR | NORMAL);

    private final Shader shader;

    Shaders(int flags) {
        this.shader = new Shader(Client.NAMESPACE, this.name().toLowerCase(), flags);
    }

    public Shader getShader() {
        return shader;
    }
}
