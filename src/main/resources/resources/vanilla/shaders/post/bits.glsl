#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform int bits;

void main() {
    float bit = pow(2.0f, bits);
    vec4 color = texture(colorTex, texCoords);
    color = floor(color * bit) / bit;
    fragColor = color;
}