#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;
out highp float gl_FragDepth;

uniform sampler2D colorTex;
uniform sampler2D depthTex;

void main() {
    fragColor = texture(colorTex, texCoords);
    gl_FragDepth = texture(depthTex, texCoords).r;
}