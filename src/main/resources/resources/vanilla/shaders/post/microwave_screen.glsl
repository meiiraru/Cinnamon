#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D screenTexture;
uniform vec2 resolution;
uniform float cellSize;
uniform float fill;
uniform float opacity;

float transform(float x) {
    return mod(x, cellSize) / cellSize;
}

void main() {
    vec2 fragCoord = texCoords * resolution;
    float y_offset = mod(floor(fragCoord.x / cellSize), 2.0f) * cellSize * 0.5f;

    float u = 2.0f * transform(fragCoord.x) - 1.0f;
    float v = 2.0f * transform(fragCoord.y - y_offset) - 1.0f;
    float d = sqrt(u * u + v * v) - fill;

    float color = d > 0.0f ? opacity : 1.0f;
    fragColor = vec4(texture(screenTexture, texCoords).rgb * color, 1.0f);
}