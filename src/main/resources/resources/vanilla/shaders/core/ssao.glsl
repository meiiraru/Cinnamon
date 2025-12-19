#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D gNormal;
uniform sampler2D gDepth;
uniform sampler2D texNoise;

uniform mat4 view;
uniform mat4 projection;
uniform mat4 invProjection;

const int MAX_SAMPLES = 64;
uniform int sampleCount;
uniform vec3 samples[MAX_SAMPLES];
uniform vec2 noiseScale;

uniform float radius = 0.5f;
uniform float bias = 0.025f;

vec3 getPosFromDepth(vec2 uv) {
    //normalized device coordinates
    vec2 ndc = uv * 2.0f - 1.0f;

    //clip space
    float depth = texture(gDepth, uv).r;
    vec4 clip = vec4(ndc, depth * 2.0f - 1.0f, 1.0f);

    //view space
    vec4 view = invProjection * clip;
    view /= view.w;

    ////world space
    //vec4 world = invView * view;
    //return world.xyz;

    return view.xyz;
}

void main() {
    //sample textures
    vec3 fragPos = getPosFromDepth(texCoords);
    vec3 normal = normalize(mat3(view) * texture(gNormal, texCoords).rgb);
    vec3 randomVec = texture(texNoise, texCoords * noiseScale).xyz;

    //create TBN matrix
    vec3 tangent = normalize(randomVec - normal * dot(randomVec, normal));
    vec3 bitangent = cross(normal, tangent);
    mat3 TBN = mat3(tangent, bitangent, normal);

    //find occlusion
    float occlusion = 0.0f;
    int samplesToUse = min(sampleCount, MAX_SAMPLES);

    for (int i = 0; i < samplesToUse; i++) {
        //get sample position
        vec3 samplePos = TBN * samples[i]; //from tangent to view-space
        samplePos = fragPos + samplePos * radius;

        //project sample to clip space
        vec4 offset = projection * vec4(samplePos, 1.0f);
        offset.xyz /= offset.w;
        offset.xyz = offset.xyz * 0.5f + 0.5f;

        //find sample depth
        float sampleDepth = getPosFromDepth(offset.xy).z;

        //accumulate occlusion
        float rangeCheck = smoothstep(0.0f, 1.0f, radius / abs(fragPos.z - sampleDepth));
        occlusion += (sampleDepth >= samplePos.z + bias ? 1.0f : 0.0f) * rangeCheck;
    }

    //average occlusion
    occlusion = (occlusion / samplesToUse);
    fragColor = vec4(vec3(occlusion), 1.0f);
}