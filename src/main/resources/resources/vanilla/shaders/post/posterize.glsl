#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core
#include shaders/libs/color_utils.glsl

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform int colorCount;

void main() {
    vec4 tex = texture(colorTex, texCoords);
    vec3 color = tex.rgb;
    color = rgb2hsv(color);
    color.gb = floor(color.gb * colorCount) / colorCount;
    color = hsv2rgb(color);
    fragColor = vec4(color, tex.a);
}