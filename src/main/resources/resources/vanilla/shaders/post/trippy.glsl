#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core
#include shaders/libs/color_utils.glsl

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 resolution;
uniform float count;
uniform float time;
uniform float waveSpeed;
uniform float waveStrength;
uniform float waveFrequency;

void main() {
    vec2 uv = texCoords;
    uv.x += sin(uv.y * resolution.y / waveFrequency + time * waveSpeed) * waveStrength;

    vec4 color = texture(colorTex, uv);
    vec2 aspect = vec2(resolution.x / resolution.y, 1.0f);

    vec2 center = vec2(0.5f, 0.5f);
    float dist = length(uv * aspect - center * aspect);

    vec3 hsv = rgb2hsv(color.rgb);
    hsv.x += dist * count + time;
    color.rgb = hsv2rgb(hsv);

    fragColor = color;
}
