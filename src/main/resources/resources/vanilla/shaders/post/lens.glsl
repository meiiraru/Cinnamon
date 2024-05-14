#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 distortion;
uniform float focus;

void main() {
    vec2 xn = 2.0f * (texCoords.st - 0.5f);
    vec3 xDistorted = vec3((1.0f + distortion * dot(xn, xn)) * xn, 1.0f);
    vec2 uv = xDistorted.xy * focus * 0.5f + 0.5f;
    fragColor = texture(colorTex, uv);
}