#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D gNormal;
uniform sampler2D gDepth;
uniform sampler2D texKernel;
uniform sampler2D texNoise;

uniform mat4 view;
uniform mat4 projection;
uniform mat4 invProjection;

uniform float nearPlane;
uniform float farPlane;

const int MAX_SAMPLES = 64;
uniform int sampleCount;
uniform vec2 noiseScale;

uniform float radius = 0.5f;
uniform float bias = 0.025f;
uniform float biasFactor = 0.1f;

float linearizeDepth(float depth) {
    float z = depth * 2.0f - 1.0f; //back to [-1..1] range
    return (2.0f * nearPlane * farPlane) / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

vec3 getPosFromDepth(vec2 uv, float depth) {
    //normalized device coordinates
    vec2 ndc = uv * 2.0f - 1.0f;

    //clip space
    vec4 clip = vec4(ndc, depth * 2.0f - 1.0f, 1.0f);

    //view space
    vec4 viewPos = invProjection * clip;
    return viewPos.xyz / viewPos.w;
}

void main() {
    //avoid sampling depth at the far plane (no geometry)
    float depth = texture(gDepth, texCoords).r;
    if (depth >= 0.99999f) {
        fragColor = vec4(0.0f, 0.0f, 0.0f, 1.0f);
        return;
    }

    //sample textures
    vec3 fragPos = getPosFromDepth(texCoords, depth);
    vec3 normal = normalize(mat3(view) * texture(gNormal, texCoords).rgb);
    vec3 randomVec = texture(texNoise, texCoords * noiseScale).xyz;

    //create TBN matrix
    vec3 tangent = normalize(randomVec - normal * dot(randomVec, normal));
    vec3 bitangent = cross(normal, tangent);
    mat3 TBN = mat3(tangent, bitangent, normal);

    //get fragment view-space z value
    float fragViewZ = -linearizeDepth(depth);
    float baseBias = bias * (1.0f + abs(fragViewZ) * biasFactor);

    //find occlusion
    float occlusion = 0.0f;
    int samplesToUse = min(sampleCount, MAX_SAMPLES);
    float kernelScale = 1.0f / float(MAX_SAMPLES);

    for (int i = 0; i < samplesToUse; i++) {
        //get sample position
        vec3 samplePos = TBN * texture(texKernel, vec2(float(i) * kernelScale, 0.0f)).rgb;
        samplePos = fragPos + samplePos * radius;

        //project sample to clip space
        vec4 offset = projection * vec4(samplePos, 1.0f);
        vec2 sampleUV = (offset.xy / offset.w) * 0.5f + 0.5f;

        //skip samples outside the screen
        if (sampleUV.x < 0.0f || sampleUV.x > 1.0f || sampleUV.y < 0.0f || sampleUV.y > 1.0f)
            continue;

        //get sample depth
        float sampleDepth = texture(gDepth, sampleUV).r;
        if (sampleDepth >= 0.99999f)
            continue;

        float sampleViewZ = -linearizeDepth(sampleDepth);

        //accumulate occlusion
        if (sampleViewZ >= samplePos.z + baseBias)
            occlusion += smoothstep(0.0f, 1.0f, radius / abs(fragViewZ - sampleViewZ));
    }

    //average occlusion
    occlusion = occlusion / float(samplesToUse);
    fragColor = vec4(occlusion, occlusion, occlusion, 1.0f);
}