#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D prevColorTex;
uniform float phosphor;

void main() {
    vec4 currTex = texture(colorTex, texCoords);
    vec4 prevTex = texture(prevColorTex, texCoords);
    fragColor = max(prevTex * phosphor, currTex);
}