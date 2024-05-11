#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D depthTexture;

void main() {
    float depthValue = texture(depthTexture, texCoords).r;
    fragColor = vec4(vec3(depthValue), 1.0f);
}