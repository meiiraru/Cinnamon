#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform float gamma = 2.2f;

void main() {
    vec4 col = texture(colorTex, texCoords);
    col.rgb = pow(col.rgb, vec3(1.0f / gamma));
    fragColor = col;
}