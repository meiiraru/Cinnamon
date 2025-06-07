#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D outlineTex;
uniform vec2 textelSize;

uniform int numSteps = 12;
uniform float radius = 4.0f;

const float TAU = 6.28318530;

void main() {
    //if (true) {fragColor = vec4(texture(outlineTex, texCoords).rgb, 1.0f); return;}

    vec4 color = texture(outlineTex, texCoords);
    vec4 outlinemask = vec4(0.0f);

    for (float i = 0.0f; i < TAU; i += TAU / numSteps) {
        vec2 offset = vec2(sin(i), cos(i)) * textelSize * radius;
        vec4 col = texture(outlineTex, texCoords + offset);
        outlinemask = mix(outlinemask, vec4(col.rgb, 1.0f), col.a);
    }

    outlinemask = mix(outlinemask, vec4(color.rgb, 0.0f), color.a);
    fragColor = outlinemask;
}