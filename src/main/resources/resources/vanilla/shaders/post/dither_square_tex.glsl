#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core
#include shaders/libs/color_utils.glsl

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D ditherTex;
uniform vec2 resolution;

uniform float colorMask = 1.0f;
uniform vec3 dither = vec3(0.75f, 0.5f, 0.25f);

void main() {
    vec3 color = texture(colorTex, texCoords).rgb;
    float brightness = rgb2hsv(color).b;

    vec2 ditherSize = textureSize(ditherTex, 0);
    vec2 ditherTexCoords = texCoords * resolution / ditherSize;
    ditherTexCoords.y = 1.0f - ditherTexCoords.y;

    vec3 d = texture(ditherTex, ditherTexCoords).rgb;
    float f = 0.0f;

    if (brightness <= dither.x)
        f += d.r;
    if (brightness <= dither.y)
        f += d.g;
    if (brightness <= dither.z)
        f += d.b;

    f = 1.0f - min(f, 1.0f);
    fragColor = vec4(mix(vec3(f), color * f, colorMask), 1.0f);
}