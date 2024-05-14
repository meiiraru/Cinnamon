#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 resolution;
uniform float factor;

void main() {
    vec2 screenSize = resolution / factor;
    vec2 coords = floor(texCoords * screenSize) / screenSize;
    fragColor = texture(colorTex, coords);
}