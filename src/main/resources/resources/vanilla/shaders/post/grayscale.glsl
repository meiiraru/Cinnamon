#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;

void main() {
    vec4 colorTex = texture(colorTex, texCoords);
    float brightness = dot(colorTex.rgb, vec3(0.2125f, 0.7154f, 0.0721f));
    fragColor = vec4(vec3(brightness), colorTex.a);
}