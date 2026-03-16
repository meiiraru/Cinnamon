#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;
uniform vec2 dir;

const float weight[5] = float[] (0.227027f, 0.1945946f, 0.1216216f, 0.054054f, 0.016216f);

void main() {
    vec3 accumRgb = vec3(0.0f);
    float accumA = 0.0f;
    float accumW = 0.0f;

    vec4 center = texture(colorTex, texCoords);
    accumRgb += center.rgb * center.a * weight[0];
    accumA += center.a * weight[0];
    accumW += weight[0];

    for (int i = 1; i < 5; i++) {
        vec2 offset = texelSize * dir * float(i);
        vec4 s1 = texture(colorTex, texCoords + offset);
        vec4 s2 = texture(colorTex, texCoords - offset);

        accumRgb += s1.rgb * s1.a * weight[i];
        accumRgb += s2.rgb * s2.a * weight[i];
        accumA += s1.a * weight[i];
        accumA += s2.a * weight[i];
        accumW += weight[i] * 2.0f;
    }

    vec3 outRgb = accumA > 0.0f ? accumRgb / accumA : vec3(0.0f);
    float outA = accumW > 0.0f ? accumA / accumW : 0.0f;

    fragColor = vec4(outRgb, outA);
}