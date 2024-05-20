#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D lutTex;
uniform vec2 lutGrid = vec2(8.0f);

void main() {
    //original color
    vec4 color = texture(colorTex, texCoords);

    //check in which cell were currently in
    float cell = color.b * (lutGrid.x * lutGrid.y - 1.0f);

    //get the correct cell in the grid
    vec2 cellPos = vec2(floor(mod(cell, lutGrid.x)), floor(cell / lutGrid.y));

    //get the final position in the LUT
    vec2 lutPos = (cellPos + color.rg) / lutGrid;

    //get the color from the LUT
    vec3 lutColor = texture(lutTex, lutPos).rgb;

    //output the color
    fragColor = vec4(lutColor, color.a);
}