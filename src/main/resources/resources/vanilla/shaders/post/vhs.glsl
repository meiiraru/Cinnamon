#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform float time;
uniform vec2 resolution;

uniform float damagedHeight = 0.05f;
uniform float trackingIntensity = 0.001f;
uniform float bottomTrackingIntensity = 0.03f;
uniform float colorBleed = 0.002f;
uniform float dropoutIntensity = 0.8f;
uniform float dropoutThreshold = 0.998f;
uniform float bottomDropoutThreshold = 0.9f;
uniform float scanlineFrequency = 2.0f;
uniform float scanlineAlpha = 0.1f;
uniform float noiseIntensity = 0.2f;
uniform float vignetteStrength = 0.25f;
uniform float desaturation = -0.5f;
uniform vec3 tintColor = vec3(0.95f, 1.0f, 0.95f);

//pseudo-random number generator
float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898f, 78.233f))) * 43758.5453f);
}

void main() {
    vec2 uv = texCoords;

    float trackIntensity = uv.y < damagedHeight ? bottomTrackingIntensity : trackingIntensity;

    //horizontal tracking distortion
    float trackingNoise = rand(vec2(uv.y, time)) * trackIntensity;
    float trackingWave = sin(uv.y * 100.0f + time * 15.0f) * (trackIntensity * 0.3f);
    uv.x += trackingNoise + trackingWave;

    //chromatic aberration
    float shiftAmount = colorBleed + sin(time * 3.0f) * (colorBleed * 0.25f);
    vec2 rg = texture(colorTex, vec2(uv.x + shiftAmount, uv.y)).rg;
    float b = texture(colorTex, vec2(uv.x - shiftAmount, uv.y)).b;

    vec3 color = vec3(rg, b);

    //tape dropouts
    float yChunk = floor(uv.y * resolution.y);
    float tChunk = floor(time * 24.0f); //24hz

    float dropThreshold = uv.y < damagedHeight ? bottomDropoutThreshold : dropoutThreshold;
    float isDropout = step(dropThreshold, rand(vec2(yChunk, tChunk)));

    float xChunk = floor(uv.x * 50.0f);
    float isStreak = step(0.8f, rand(vec2(xChunk, tChunk)));

    color += isDropout * isStreak * dropoutIntensity;

    //scanlines
    float scanline = sin(uv.y * resolution.y * scanlineFrequency) * scanlineAlpha;
    color -= scanline;

    //VHS noise
    float noise = (rand(uv + mod(time, 10.0f)) - 0.5f) * noiseIntensity;
    color += noise;

    //vignette
    float vignette = uv.x * uv.y * (1.0f - uv.x) * (1.0f - uv.y);
    vignette = clamp(pow(16.0f * vignette, vignetteStrength), 0.0f, 1.0f);
    color *= vignette;

    //color grading
    vec3 grayscale = vec3(dot(color, vec3(0.299f, 0.587f, 0.114f)));
    color = mix(color, grayscale, desaturation);
    color *= tintColor;

    fragColor = vec4(color, 1.0f);
}