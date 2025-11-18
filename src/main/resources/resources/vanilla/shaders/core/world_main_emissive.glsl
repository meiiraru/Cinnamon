#type vertex
#include shaders/core/world_main.vsh

#type fragment
#version 330 core

flat in int texID;
in vec2 texCoords;
in vec3 pos;
in vec4 color;
in vec3 normal;

layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gEmissive;

uniform sampler2D textures[16];

void main() {
    //color
    vec4 col = color;

    if (texID >= 0) {
        //texture
        vec4 tex = texture(textures[texID], texCoords);
        if (tex.a < 0.5f)
            discard;

        col *= tex;
    }

    //gBuffer outputs
    gAlbedo = vec4(0.0f, 0.0f, 0.0f, 1.0f);
    gNormal = vec4(normal, 1.0f);
    gORM = vec4(1.0f, 1.0f, 0.0f, 1.0f);
    gEmissive = col;
}