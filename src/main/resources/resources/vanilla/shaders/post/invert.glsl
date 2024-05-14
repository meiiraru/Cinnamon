#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;

void main() {
    vec4 tex = texture(colorTex, texCoords);
    fragColor = vec4(vec3(1.0f - tex.rgb), tex.a);
}