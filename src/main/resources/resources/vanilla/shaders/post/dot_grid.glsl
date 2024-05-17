#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 resolution;
uniform float cellSize;
uniform float fill;
uniform float opacity;

void main() {
    vec2 gridPos = mod(texCoords * resolution / cellSize, vec2(1.0f));
    float distance = length(gridPos - vec2(0.5f));
    float color = distance < fill * 0.5f ? opacity : 1.0f;
    fragColor = vec4(texture(colorTex, texCoords).rgb * color, 1.0f);
}