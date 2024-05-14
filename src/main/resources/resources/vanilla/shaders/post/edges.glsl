#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 textelSize;

void main() {
    vec4 center = texture(colorTex, texCoords);
    vec4 left   = texture(colorTex, texCoords - vec2(textelSize.x, 0.0f));
    vec4 right  = texture(colorTex, texCoords + vec2(textelSize.x, 0.0f));
    vec4 up     = texture(colorTex, texCoords - vec2(0.0f, textelSize.y));
    vec4 down   = texture(colorTex, texCoords + vec2(0.0f, textelSize.y));

    vec4 leftDiff  = center - left;
    vec4 rightDiff = center - right;
    vec4 upDiff    = center - up;
    vec4 downDiff  = center - down;

    vec4 total = clamp(leftDiff + rightDiff + upDiff + downDiff, 0.0f, 1.0f);
    fragColor = vec4(total.rgb, 1.0f);
}