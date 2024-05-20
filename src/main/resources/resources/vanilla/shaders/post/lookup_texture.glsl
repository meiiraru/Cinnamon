#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D lutTex;
uniform vec2 lutGrid = vec2(8.0f);

vec2 lutSize = textureSize(lutTex, 0) / lutGrid;

void main() {
    //original color
    vec4 color = texture(colorTex, texCoords);

    //check which cell were currently in based on the blue channel
    float blue = color.b * (lutGrid.x * lutGrid.y - 1.0f);
    float red = color.r * (lutSize.x - 1.0f);
    float green = color.g * (lutSize.y - 1.0f);

    //grab floor grid position
    vec2 cell = vec2(floor(mod(blue, lutGrid.x)), floor(blue / lutGrid.x)) * lutSize;
    vec2 uv = (cell + floor(vec2(red, green))) / (lutGrid * lutSize);
    vec3 lutColor1 = texture(lutTex, uv).rgb;

    //grab ceil grid position
    cell = vec2(floor(mod(blue + 1.0f, lutGrid.x)), floor((blue + 1.0f) / lutGrid.x)) * lutSize;
    uv = (cell + ceil(vec2(red, green))) / (lutGrid * lutSize);
    vec3 lutColor2 = texture(lutTex, uv).rgb;

    //get the color from the LUT
    vec3 lutColor = mix(lutColor1, lutColor2, vec3(fract(red), fract(green), fract(blue)));

    //output the color
    fragColor = vec4(lutColor, color.a);
}