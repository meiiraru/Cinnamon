#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;
uniform float radius = 2.0f;

void main() {
    vec4 result = vec4(0.0f);

    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            result += texture(colorTex, texCoords + offset);
        }
    }

    fragColor = result / float((radius * 2) * (radius * 2));
}