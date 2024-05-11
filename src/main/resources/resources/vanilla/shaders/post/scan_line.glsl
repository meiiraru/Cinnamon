#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D screenTexture;
uniform float time;
uniform float density;
uniform float opacity;

void main() {
    vec4 col4 = texture(screenTexture, texCoords);
    vec3 col = col4.rgb;

    float y = texCoords.y + time;
    vec2 sl = vec2(sin(y * density), cos(y * density));
    vec3 scanlines = vec3(sl.x, sl.y, sl.x);

    col += col * scanlines * opacity;
    fragColor = vec4(col, col4.a);
}