#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 textelSize;
uniform float intensity;

void main() {
    vec2 offset = vec2(textelSize.x, textelSize.y) * intensity;
    fragColor = vec4(
        texture(colorTex, texCoords + offset).r,
        texture(colorTex, texCoords - offset).gb,
        1.0f
    );
}