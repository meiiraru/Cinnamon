#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;

uniform sampler2D sceneTex;
uniform sampler2D bloomTex;
uniform float bloomStrength = 1.0f;

void main() {
    vec4 sceneColor = texture(sceneTex, texCoords);
    vec4 bloomColor = texture(bloomTex, texCoords);

    vec4 col = sceneColor + bloomColor * bloomStrength;
    col.a = clamp(col.a, 0.0f, 1.0f);
    fragColor = col;
}