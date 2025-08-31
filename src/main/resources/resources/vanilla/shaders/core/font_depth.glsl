#type vertex
#include shaders/core/main_depth.vsh

#type fragment
#version 330 core

flat in int texID;
in vec2 texCoords;

uniform sampler2D textures[16];

void main() {
    if (texID < 0)
        return;

    vec4 tex = texture(textures[texID], texCoords);
    if (tex.r < 0.01f)
        discard;
}