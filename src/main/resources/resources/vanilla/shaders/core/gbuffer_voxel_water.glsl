#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec3 aTangent;
layout (location = 4) in float aTexLayer;

out vec3 pos;
out vec3 normal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;

void main() {
    vec4 worldPos = model * vec4(aPosition, 1.0f);
    gl_Position = projection * view * worldPos;
    pos = worldPos.xyz;
    normal = normalize(normalMat * aNormal);
}

#type fragment
#version 330 core

layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gEmissive;

in vec3 pos;
in vec3 normal;

uniform sampler2D noiseTex;

uniform float time;

uniform vec4 color = vec4(0.3f, 0.45f, 0.6f, 0.95f);
uniform vec2 roughMetal = vec2(0.1f, 0.0f);

uniform vec2 waveDir1 = vec2(1.0f, 0.3f);
uniform vec2 waveDir2 = vec2(-0.28f, 0.7f);
uniform float waveAmplitude = 0.1f;
uniform float waveFrequency = 0.01f;

float gradientNoise(vec2 p) {
    return texture(noiseTex, p).r * 2.0f - 1.0f;
}

// Fractal brownian motion
float fbm(vec2 p) {
    float value = 0.0f;
    float amplitude = 0.5f;
    float frequency = 1.0f;

    for (int i = 0; i < 5; i++) {
        value += amplitude * gradientNoise(p * frequency);
        frequency *= 2.0f;
        amplitude *= 0.5f;
    }
    return value;
}

float waterHeight(vec2 p, float t) {
    vec2 move1 = waveDir1 * t;
    vec2 move2 = waveDir2 * t;

    float height = 0.0f;
    height += fbm(p * waveFrequency + move1) * 0.6f;
    height += fbm(p * waveFrequency * 2.0f + move2) * 0.3f;
    height += fbm(p * waveFrequency * 4.0f - move1 * 0.5f) * 0.1f;

    return height * waveAmplitude;
}

vec3 calculateWaveNormal(vec3 worldPos) {
    float t = time;
    vec2 p = worldPos.xz;

    float eps = 0.05f;
    float hL = waterHeight(p - vec2(eps, 0.0f), t);
    float hR = waterHeight(p + vec2(eps, 0.0f), t);
    float hD = waterHeight(p - vec2(0.0f, eps), t);
    float hU = waterHeight(p + vec2(0.0f, eps), t);

    return normalize(vec3(hL - hR, 2.0f * eps, hD - hU));
}

void main() {
    // Use the geometric face normal to determine if this is a top/bottom face or side face
    vec3 faceNormal = normalize(normal);
    vec3 waveNormal;

    // For top faces (Y-facing), apply animated wave normals
    if (abs(faceNormal.y) > 0.5f) {
        waveNormal = calculateWaveNormal(pos);
        // Blend geometric normal with wave normal
        waveNormal = normalize(mix(faceNormal, waveNormal, 0.8f));
    } else {
        // For side faces, keep geometric normal (slight wave perturbation)
        vec3 sideWave = calculateWaveNormal(pos);
        waveNormal = normalize(mix(faceNormal, sideWave, 0.15f));
    }

    gAlbedo = color;
    gNormal = vec4(waveNormal, 1.0f);
    gORM = vec4(1.0f, roughMetal, 1.0f);
    gEmissive = vec4(0.0f, 0.0f, 0.0f, 1.0f);
}
