#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;

uniform sampler2D colorTex;
uniform float exposure = 1.0f;
uniform float gamma = 2.2f;

//ACES (Academy Color Encoding System) is the industry standard for tone mapping
vec3 ACESFilm(vec3 x) {
    return clamp((x * (2.51f * x + 0.03f)) / (x * (2.43f * x + 0.59f) + 0.14f), 0.0f, 1.0f);
}

void main() {
    vec3 hdrColor = texture(colorTex, texCoords).rgb;
    vec3 exposedColor = hdrColor * exposure;

    //apply ACES tone mapping
    vec3 toneMappedColor = ACESFilm(exposedColor);

    //apply gamma correction
    vec3 gammaCorrectedColor = pow(toneMappedColor, vec3(1.0f / gamma));
    fragColor = vec4(gammaCorrectedColor, 1.0f);
}