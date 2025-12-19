#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D ssaoTex;
uniform vec2 texelSize;

void main() {
    float result = 0.0f;

    for (int x = -2; x < 2; x++) {
        for (int y = -2; y < 2; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            result += texture(ssaoTex, texCoords + offset).r;
        }
    }

    fragColor = vec4(vec3(result / (4.0f * 4.0f)), 1.0f);
}