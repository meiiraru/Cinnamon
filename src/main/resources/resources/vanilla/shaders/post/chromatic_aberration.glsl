#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;
uniform float intensity;

void main() {
    vec2 offset = vec2(texelSize.x, texelSize.y) * intensity;
    vec4 red = texture(colorTex, texCoords + offset);
    vec4 greenBlue = texture(colorTex, texCoords - offset);

    fragColor = vec4(red.r, greenBlue.gb, (red.a + greenBlue.a) * 0.5);
}