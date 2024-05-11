#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D screenTexture;

void main() {
    const float gamma = 2.2f;
    float exposure = 1.0f;

    vec3 hdrColor = texture(screenTexture, texCoords).rgb;

    //exposure tone mapping
    vec3 mapped = vec3(1.0f) - exp(-hdrColor * exposure);
    //gamma correction
    mapped = pow(mapped, vec3(1.0f / gamma));

    fragColor = vec4(mapped, 1.0f);
}