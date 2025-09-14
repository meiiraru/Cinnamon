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

    vec3 col = sceneColor.rgb + bloomColor.rgb * bloomStrength;
    fragColor = vec4(col, sceneColor.a);
}