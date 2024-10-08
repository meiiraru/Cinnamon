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

ivec2 ditherTexSize = textureSize(ditherTex, 0);
float ditherSteps = ditherTexSize.x / ditherTexSize.y;

float great(vec3 t) {
    return max(t.x, max(t.y, t.z));
}

void main() {
    vec2 scrCoords = texCoords * resolution;
    vec2 uv = mod(scrCoords, vec2(16.0f, 16.0f)) / 16.0f;

    vec3 color = texture(colorTex, texCoords).rgb;
    float brightness = floor(rgb2hsv(color).b * ditherSteps) / ditherSteps;

    float offset = max(brightness - 1.0f / ditherSteps, 0.0f);

    vec2 f = vec2(uv.x / ditherSteps + offset, uv.y);
    vec3 d = texture(ditherTex, f).rgb;

    fragColor = vec4(d * color, 1.0f);
}