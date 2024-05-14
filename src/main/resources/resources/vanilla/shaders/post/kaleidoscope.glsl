#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform float segments;

void main() {
    vec2 normed = 2.0f * texCoords - 1.0f;
    float r = length(normed);
    float theta = atan(normed.y / abs(normed.x));
    theta *= segments;

    vec2 newUV = (vec2(r * cos(theta), r * sin(theta)) + 1.0f) / 2.0f;
    fragColor = texture(colorTex, newUV);
}
