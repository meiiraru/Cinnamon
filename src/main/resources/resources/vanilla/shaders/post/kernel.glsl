#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;
uniform mat3 kernel;

void main() {
    vec3 col = vec3(0.0f);

    for (int y = -1, i = 0; y <= 1; y++, i++) {
        for (int x = -1, j = 0; x <= 1; x++, j++) {
            vec3 tex = texture(colorTex, texCoords.xy + vec2(texelSize.x * x, texelSize.y * y)).rgb;
            col += tex * kernel[i][j];
        }
    }

    fragColor = vec4(col, 1.0f);
}