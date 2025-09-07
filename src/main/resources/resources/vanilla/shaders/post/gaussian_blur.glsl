#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;
uniform vec2 dir;

float weight[5] = float[] (0.227027f, 0.1945946f, 0.1216216f, 0.054054f, 0.016216f);

void main() {
    vec3 result = texture(colorTex, texCoords).rgb * weight[0];

    for (int i = 1; i < 5; i++) {
        vec2 offset = texelSize * dir * i;
        result += texture(colorTex, texCoords + offset).rgb * weight[i];
        result += texture(colorTex, texCoords - offset).rgb * weight[i];
    }

    fragColor = vec4(result, 1.0f);
}