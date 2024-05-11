#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D screenTexture;
uniform sampler2D prevTexture;
uniform vec3 phosphor;

void main() {
    vec4 currTex = texture(screenTexture, texCoords);
    vec4 prevTex = texture(prevTexture, texCoords);
    fragColor = vec4(max(prevTex.rgb * phosphor, currTex.rgb), 1.0f);
}