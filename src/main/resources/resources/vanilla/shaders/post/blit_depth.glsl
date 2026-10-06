#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

uniform sampler2D depthTex;

void main() {
    gl_FragDepth = texture(depthTex, texCoords).r;
}