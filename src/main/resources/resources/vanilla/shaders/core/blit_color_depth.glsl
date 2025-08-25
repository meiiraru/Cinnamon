#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;
out highp float gl_FragDepth;

uniform sampler2D colorTexA;
uniform sampler2D depthTexA;
uniform sampler2D colorTexB;
uniform sampler2D depthTexB;

void main() {
    float depthA = texture(depthTexA, texCoords).r;
    float depthB = texture(depthTexB, texCoords).r;

    if (depthA < depthB) {
        fragColor = texture(colorTexA, texCoords);
        gl_FragDepth = depthA;
    } else {
        fragColor = texture(colorTexB, texCoords);
        gl_FragDepth = depthB;
    }
}