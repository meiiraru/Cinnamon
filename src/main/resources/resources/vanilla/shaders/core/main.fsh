#version 330 core

flat in int texID;
in vec2 texCoords;
in vec4 color;

out vec4 fragColor;

uniform sampler2D textures[16];

void main() {
    if (texID < 0) {
        fragColor = color;
        return;
    }

    //texture
    vec4 tex = texture(textures[texID], texCoords);
    if (tex.a < 0.01f)
        discard;

    fragColor = tex * color;
}