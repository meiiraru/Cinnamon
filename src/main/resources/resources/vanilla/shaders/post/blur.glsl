#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;
uniform vec2 dir = vec2(1.0f, 1.0f);
uniform int radius = 5;

void main() {
    vec3 accumRgb = vec3(0.0f);
    float accumA = 0.0f;
    float accumW = 0.0f;

    for (int r = -radius; r <= radius; r++) {
        vec4 s = texture(colorTex, texCoords + texelSize * float(r) * dir);
        accumRgb += s.rgb * s.a;
        accumA += s.a;
        accumW += 1.0f;
    }

    vec3 outRgb = accumA > 0.0f ? accumRgb / accumA : vec3(0.0f);
    float outA = accumW > 0.0f ? accumA / accumW : 0.0f;

    fragColor = vec4(outRgb, outA);
}