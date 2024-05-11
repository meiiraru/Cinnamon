#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D screenTexture;
uniform vec2 textelSize;
uniform vec2 dir;
uniform float radius;

void main() {
    vec4 blurred = vec4(0.0f);
    float totalAlpha = 0.0f;

    for (float r = -radius; r <= radius; r++) {
        vec4 sampleValue = texture(screenTexture, texCoords + textelSize * r * dir);
        totalAlpha += sampleValue.a;
        blurred += sampleValue;
    }

    fragColor = vec4(blurred.rgb / (radius * 2.0f + 1.0f), totalAlpha);
}