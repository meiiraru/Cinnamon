#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;

uniform sampler2D sceneTex; //rgba
uniform sampler2D bloomTex; //rgb
uniform float bloomStrength = 1.0f;

void main() {
    vec4 sceneColor = texture(sceneTex, texCoords);
    vec3 bloomColor = texture(bloomTex, texCoords).rgb;

    vec3 col = sceneColor.rgb + bloomColor * bloomStrength;
    fragColor = vec4(col, sceneColor.a);
}