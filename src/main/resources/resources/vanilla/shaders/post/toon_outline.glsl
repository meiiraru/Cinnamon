#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D depthTex;
uniform sampler2D normalTex;

uniform vec2 textelSize;
uniform vec3 outlineColor = vec3(1.0f);
uniform vec2 depthBias = vec2(1.0f);
uniform vec2 normalBias = vec2(1.0f);

void main() {
    float depth = texture(depthTex, texCoords).r;
    float depthDiff = 0.0f;

    vec3 normal = texture(normalTex, texCoords).xyz;
    float normalDiff = 0.0f;

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float neighborDepth = texture(depthTex, texCoords + vec2(x, y) * textelSize).r;
            depthDiff = abs(depth - neighborDepth);

            vec3 neighborNormal = texture(normalTex, texCoords + vec2(x, y) * textelSize).xyz;
            normalDiff = distance(normal, neighborNormal);
        }
    }

    depthDiff = depthDiff * depthBias.x;
    depthDiff = pow(depthDiff, depthBias.y);

    normalDiff = normalDiff * normalBias.x;
    normalDiff = pow(normalDiff, normalBias.y);

    float outline = normalDiff + depthDiff;
    //fragColor = vec4(vec3(outline), 1.0f);
    fragColor = vec4(mix(texture(colorTex, texCoords), vec4(outlineColor, 1.0f), outline));
}