#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;
uniform int radius = 2;

void main() {
    vec3 accumRgb = vec3(0.0f);
    float accumA = 0.0f;
    float accumW = 0.0f;

    for (int x = -radius; x <= radius; x++) {
        for (int y = -radius; y <= radius; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            vec4 s = texture(colorTex, texCoords + offset);

            accumRgb += s.rgb * s.a;
            accumA += s.a;
            accumW += 1.0f;
        }
    }

    vec3 outRgb = accumA > 0.0f ? accumRgb / accumA : vec3(0.0f);
    float outA = accumW > 0.0f ? accumA / accumW : 0.0f;

    fragColor = vec4(outRgb, outA);
}