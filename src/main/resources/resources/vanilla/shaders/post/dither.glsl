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

ivec2 ditherTexSize = textureSize(ditherTex, 0);
float ditherSteps = ditherTexSize.x / ditherTexSize.y;

void main() {
    vec2 scrCoords = texCoords * resolution;
    float cellSize = ditherTexSize.y;
    vec2 uv = mod(scrCoords, vec2(cellSize)) / cellSize;

    vec4 tex = clamp(texture(colorTex, texCoords), 0.0f, 1.0f);
    vec3 color = tex.rgb;
    float brightness = floor(rgb2hsv(color).b * ditherSteps) / ditherSteps;

    float offset = max(brightness - 1.0f / ditherSteps, 0.0f);

    vec2 f = vec2(uv.x / ditherSteps + offset, 1.0f - uv.y);
    vec3 d = texture(ditherTex, f).rgb;

    fragColor = vec4(mix(d, d * color, colorMask), tex.a);
}