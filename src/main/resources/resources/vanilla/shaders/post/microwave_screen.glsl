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
uniform vec2 borders;

float transform(float x) {
    return mod(x, cellSize) / cellSize;
}

void main() {
    vec2 fragCoord = texCoords * resolution;
    bool odd = mod(floor(fragCoord.x / cellSize), 2.0f) == 1.0f;
    float y_offset = odd ? cellSize * 0.5f : 0.0f;

    float u = 2.0f * transform(fragCoord.x) - 1.0f;
    float v = 2.0f * transform(fragCoord.y - y_offset) - 1.0f;
    float d = sqrt(u * u + v * v) - fill;

    vec2 screenCells = resolution / cellSize;
    vec2 cellsBorder = floor(screenCells);
    vec2 pos = texCoords * screenCells;
    float border = borders.y + (odd ? 1.5f : 0.0f);

    float color = 1.0f;
    if (d > 0.0f ||
        pos.x < borders.x || pos.x > (cellsBorder.x - borders.x) ||
        pos.y < border || pos.y > (cellsBorder.y - border)
    )
        color = opacity;

    fragColor = vec4(texture(colorTex, texCoords).rgb * color, 1.0f);
}