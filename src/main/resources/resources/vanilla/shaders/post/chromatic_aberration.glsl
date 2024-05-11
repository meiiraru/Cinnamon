#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D screenTexture;
uniform vec2 textelSize;
uniform float intensity;

void main() {
    vec4 tex = texture(screenTexture, texCoords);

    vec2 offset = vec2(textelSize.x, textelSize.y) * intensity;
    tex.r = texture(screenTexture, texCoords + offset).r;
    tex.gb = texture(screenTexture, texCoords - offset).gb;

    fragColor = tex;
}