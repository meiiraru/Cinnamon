#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 uvOffset;

void main() {
    fragColor = texture(colorTex, texCoords + uvOffset);
}