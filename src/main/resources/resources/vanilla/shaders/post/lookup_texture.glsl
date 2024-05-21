#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D lutTex;
uniform vec2 lutGrid = vec2(8.0f);

vec2 cellSize = textureSize(lutTex, 0) / lutGrid;

void main() {
    //original color
    vec4 color = texture(colorTex, texCoords);

    //get channels values
    vec3 channels = vec3(
        color.r * (cellSize.x - 1.0f),
        color.g * (cellSize.y - 1.0f),
        color.b * (lutGrid.x * lutGrid.y - 1.0f)
    );

    //get the floor color
    float blue2 = floor(channels.b);
    vec2 gridPos = vec2(mod(blue2, lutGrid.x), floor(blue2 / lutGrid.x));
    vec2 uv = (gridPos + floor(channels.rg) / cellSize) / lutGrid;
    vec3 lutColor1 = texture(lutTex, uv).rgb;

    //get the ceil color
    blue2 += 1.0f;
    gridPos = vec2(mod(blue2, lutGrid.x), floor(blue2 / lutGrid.x));
    uv = (gridPos + ceil(channels.rg) / cellSize) / lutGrid;
    vec3 lutColor2 = texture(lutTex, uv).rgb;

    //interpolate and return the color
    vec3 lutColor = mix(lutColor1, lutColor2, fract(channels));
    fragColor = vec4(lutColor, color.a);
}