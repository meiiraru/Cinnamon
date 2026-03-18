#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;

void main() {
    vec4 colorTex = texture(colorTex, texCoords);
    float brightness = dot(colorTex.rgb, vec3(0.2126f, 0.7152f, 0.0722f));
    brightness = pow(brightness, 1.0f / 2.2f);
    fragColor = vec4(vec3(brightness), colorTex.a);
}