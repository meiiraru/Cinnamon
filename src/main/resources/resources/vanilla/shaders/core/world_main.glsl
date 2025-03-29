#type vertex
#include shaders/core/world_main.vsh

#type fragment
#version 330 core
#include shaders/libs/light.glsl
#include shaders/libs/fog.glsl

flat in int texID;
in vec2 texCoords;
in vec3 pos;
in vec4 color;
in vec3 normal;

out vec4 fragColor;

uniform sampler2D textures[16];
uniform vec3 camPos;

void main() {
    //color
    vec4 col = color;

    if (texID >= 0) {
        //texture
        vec4 tex = texture(textures[texID], texCoords);
        if (tex.a < 0.01f)
            discard;

        col *= tex;
    }

    //ambient light
    col *= calculateLight(pos, normal);

    //fog
    col = calculateFog(pos, camPos, col);

    //final color
    fragColor = col;
}