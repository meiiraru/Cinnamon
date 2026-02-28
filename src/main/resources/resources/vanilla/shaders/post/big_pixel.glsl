#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;
const float HALF_PI = 1.57079632679;

in vec2 texCoords;
out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;
uniform float factor;


void main() {
    vec2 screenSize = vec2(1.0) / texelSize;
    vec2 resolution = screenSize / factor;
    vec2 cell = floor(texCoords * resolution) / resolution;
    vec2 uv = mod(texCoords * resolution, 1.0);

    vec3 color = texture(colorTex, cell).rgb;
    float intensity = min(sin(uv.y * PI), 1.0);
    float r = 2.0 * cos(uv.x * TWO_PI - (TWO_PI / 6.0)) - 1.0;
    float g = 2.0 * cos(uv.x * TWO_PI - ((3.0 * TWO_PI) / 6.0)) - 1.0;
    float b = 2.0 * cos(uv.x * TWO_PI - ((5.0 * TWO_PI) / 6.0)) - 1.0;
    vec3 col = color * intensity * vec3(r, g, b);

    fragColor = vec4(col, 1.0);
}