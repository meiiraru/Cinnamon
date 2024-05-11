#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D screenTexture;

void main() {
    vec2 uv = vec2(texCoords.x, 1.0f - texCoords.y);
    fragColor = texture(screenTexture, uv);
}