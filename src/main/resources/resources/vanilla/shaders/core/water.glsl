#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec3 pos;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main() {
    vec4 worldPos = model * vec4(aPosition, 1.0f);
    pos = worldPos.xyz;
    gl_Position = projection * view * worldPos;
}

#type fragment
#version 330 core

in vec3 pos;

layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gEmissive;

uniform vec4 color = vec4(0.4f, 0.6f, 0.8f, 0.8f);
uniform float time;

uniform vec2 waveDir1 = vec2(1.0f, 0.3f);
uniform vec2 waveDir2 = vec2(-0.28f, 0.7f);
uniform float waveAmplitude = 0.15f;
uniform float waveFrequency = 0.5f;

vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1f, 311.7f)), dot(p, vec2(269.5f, 183.3f)));
    return fract(sin(p) * 43758.5453123f) * 2.0f - 1.0f;
}

//perlin-like smooth gradient noise
float gradientNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    //quintic interpolation for smoothness
    vec2 u = f * f * f * (f * (f * 6.0f - 15.0f) + 10.0f);

    return mix(mix(dot(hash2(i + vec2(0.0f, 0.0f)), f - vec2(0.0f, 0.0f)),
                   dot(hash2(i + vec2(1.0f, 0.0f)), f - vec2(1.0f, 0.0f)), u.x),
               mix(dot(hash2(i + vec2(0.0f, 1.0f)), f - vec2(0.0f, 1.0f)),
                   dot(hash2(i + vec2(1.0f, 1.0f)), f - vec2(1.0f, 1.0f)), u.x), u.y);
}

//fractal brownian motion
float fbm(vec2 p) {
    float value = 0.0f;
    float amplitude = 0.5f;
    float frequency = 1.0f;

    //multiple octaves for detail at different scales
    for (int i = 0; i < 5; i++) {
        value += amplitude * gradientNoise(p * frequency);
        frequency *= 2.0f;
        amplitude *= 0.5f;
    }
    return value;
}

float waterHeight(vec2 p, float t) {
    //two wave directions
    vec2 move1 = waveDir1 * t;
    vec2 move2 = waveDir2 * t;

    //layer multiple noise patterns
    float height = 0.0f;
    height += fbm(p * waveFrequency + move1) * 0.6f;
    height += fbm(p * waveFrequency * 2.0f + move2) * 0.3f;
    height += fbm(p * waveFrequency * 4.0f - move1 * 0.5f) * 0.1f;

    return height * waveAmplitude;
}

vec3 calculateWaveNormal(vec3 pos) {
    float t = time;
    vec2 p = pos.xz;

    //calculate normal using central differences for better accuracy
    float eps = 0.05f;
    float hL = waterHeight(p - vec2(eps, 0.0f), t);
    float hR = waterHeight(p + vec2(eps, 0.0f), t);
    float hD = waterHeight(p - vec2(0.0f, eps), t);
    float hU = waterHeight(p + vec2(0.0f, eps), t);

    //construct normal from height gradient
    return normalize(vec3(hL - hR, 2.0f * eps, hD - hU));
}

void main() {
    vec3 normal = calculateWaveNormal(pos);

    gAlbedo = color;
    gNormal = vec4(normal, 1.0f);
    gORM = vec4(1.0f, 0.1f, 0.0f, 1.0f);
    gEmissive = vec4(0.0f, 0.0f, 0.0f, 1.0f);
}