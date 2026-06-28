#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D depthTex;
uniform vec2 texelSize;

uniform float near;
uniform float far;

uniform vec2 focusRange = vec2(5.0f, 10.0f);
uniform float maxBlurRadius = 5.0f;
uniform float bokehIntensity = 5.0f;

const vec3 LUMA_WEIGHT = vec3(0.299f, 0.587f, 0.114f);

float linearizeDepth(float depth) {
    //convert back to NDC [-1, 1]
    float z = depth * 2.0f - 1.0f;

    //calculate the actual distance from the camera (view space)
    float linearDepth = (2.0f * near * far) / (far + near - z * (far - near));

    //return [0, 1] range
    return linearDepth / far;
}

void main() {
    //autofocus from the center of the screen
    float rawTargetDepth = texture(depthTex, vec2(0.5f, 0.5f)).r;
    float targetDepth = linearizeDepth(rawTargetDepth);

    //get the current pixel depth
    float rawDepth = texture(depthTex, texCoords).r;
    float depth = linearizeDepth(rawDepth);

    //find the distance from the focal point
    float depthDiff = abs(depth - targetDepth);

    //circle of confusion (CoC)
    float coc = smoothstep(focusRange.x / far, focusRange.y / far, depthDiff);

    //calculate this specific pixel blob radius
    float currentRadius = coc * maxBlurRadius;

    //if perfectly in focus, return the original color
    if (currentRadius < 1.0f) {
        fragColor = texture(colorTex, texCoords);
        return;
    }

    vec4 colorAcc = vec4(0.0f);
    float weightAcc = 0.0f;
    float cocSquared = currentRadius * currentRadius;

    //calculate a limit based on the pixel radius
    float limit = ceil(currentRadius);

    //circular sampling
    for (float u = -limit; u <= limit; u++) {
        for (float v = -limit; v <= limit; v++) {
            float dist = u * u + v * v;

            //only sample if within the circular radius
            if (dist > cocSquared)
                continue;

            vec2 offset = vec2(u, v) * texelSize;
            vec4 sampleCol = texture(colorTex, texCoords + offset);

            //give higher weight to bright pixels making highlights bloom more than dark areas
            float luma = dot(sampleCol.rgb, LUMA_WEIGHT);
            float weight = 1.0f + (pow(luma, 4.0f) * bokehIntensity);

            colorAcc += sampleCol * weight;
            weightAcc += weight;
        }
    }

    //average color
    fragColor = vec4(colorAcc.rgb / weightAcc, texture(colorTex, texCoords).a);
}