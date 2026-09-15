#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex; //low res
uniform sampler2D depthTex; //low res
uniform sampler2D fullResDepth;

uniform vec2 lowResTexelSize;
uniform float nearPlane;
uniform float farPlane;

float linearizeDepth(float depth) {
    float z = depth * 2.0f - 1.0f; //ndc
    return (2.0f * nearPlane * farPlane) / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

void main() {
    //get the exact depth of the current high-res pixel
    float rawHighDepth  = texture(fullResDepth, texCoords).r;
    float highDepth = linearizeDepth(rawHighDepth);

    //sample the 4 closest pixels from the low-res texture
    vec2 uv = texCoords - lowResTexelSize * 0.5f;

    vec2 uv00 = uv;
    vec2 uv10 = uv + vec2(lowResTexelSize.x, 0.0f);
    vec2 uv01 = uv + vec2(0.0f, lowResTexelSize.y);
    vec2 uv11 = uv + lowResTexelSize;

    //fetch the low-res depths for those 4 pixels
    float d00 = linearizeDepth(texture(depthTex, uv00).r);
    float d10 = linearizeDepth(texture(depthTex, uv10).r);
    float d01 = linearizeDepth(texture(depthTex, uv01).r);
    float d11 = linearizeDepth(texture(depthTex, uv11).r);

    //fetch the low-res colors for those 4 pixels
    vec4 c00 = texture(colorTex, uv00);
    vec4 c10 = texture(colorTex, uv10);
    vec4 c01 = texture(colorTex, uv01);
    vec4 c11 = texture(colorTex, uv11);

    //how quickly the blur falls off at an edge
    float sensitivity = 2.0f;

    //calculate weights based on world depth differences
    vec4 weights = vec4(
            1.0f / (1.0f + abs(highDepth - d00) * sensitivity) + 0.001f,
            1.0f / (1.0f + abs(highDepth - d10) * sensitivity) + 0.001f,
            1.0f / (1.0f + abs(highDepth - d01) * sensitivity) + 0.001f,
            1.0f / (1.0f + abs(highDepth - d11) * sensitivity) + 0.001f
    );

    //blend the colors using the weights
    float totalWeight = weights.x + weights.y + weights.z + weights.w;
    vec4 finalColor = (c00 * weights.x + c10 * weights.y + c01 * weights.z + c11 * weights.w) / totalWeight;

    //output the final color
    fragColor = finalColor;
}