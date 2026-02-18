#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform float time;
uniform vec2 resolution;
uniform vec2 framebufferOffset;

//warp fbm
//https://iquilezles.org/articles/warp/

const mat2 m = mat2(0.8f, 0.6f, -0.6f, 0.8f);

float noise(vec2 p) {
    return sin(p.x) * sin(p.y);
}

float fbm4(vec2 p) {
    float f = 0.0f;
    f += 0.5f    * noise(p); p = m * p * 2.02f;
    f += 0.25f   * noise(p); p = m * p * 2.03f;
    f += 0.125f  * noise(p); p = m * p * 2.01f;
    f += 0.0625f * noise(p);
    return f / 0.9375f;
}

float fbm6(vec2 p) {
    float f = 0.0f;
    f += 0.5f      * (0.5f + 0.5f * noise(p)); p = m * p * 2.02f;
    f += 0.25f     * (0.5f + 0.5f * noise(p)); p = m * p * 2.03f;
    f += 0.125f    * (0.5f + 0.5f * noise(p)); p = m * p * 2.01f;
    f += 0.0625f   * (0.5f + 0.5f * noise(p)); p = m * p * 2.04f;
    f += 0.03125f  * (0.5f + 0.5f * noise(p)); p = m * p * 2.01f;
    f += 0.015625f * (0.5f + 0.5f * noise(p));
    return f / 0.96875f;
}

vec2 fbm4_2(vec2 p) {
    return vec2(fbm4(p), fbm4(p + vec2(7.8f)));
}

vec2 fbm6_2(vec2 p) {
    return vec2(fbm6(p + vec2(16.8f)), fbm6(p + vec2(11.5f)));
}

float func(vec2 q, out vec4 ron) {
    q += 0.03f * sin(vec2(0.27f, 0.23f) * time + length(q) * vec2(4.1f, 4.3f));

    vec2 o = fbm4_2(0.9f * q);
    o += 0.04f * sin(vec2(0.12f, 0.14f) * time + length(o));

    vec2 n = fbm6_2(3.0f * o);
    ron = vec4(o, n);

    float f = 0.5f + 0.5f * fbm4(1.8f * q + 6.0f * n);
    return mix(f, f * f * f * 3.5f, f * abs(n.x));
}

vec3 warp(vec2 p, float e) {
    vec4 on = vec4(0.0f);
    float f = func(p, on);

    vec3 col = vec3(0.0f);
    col = mix(vec3(0.2f, 0.1f, 0.4f), vec3(0.3f, 0.05f, 0.05f), f);
    col = mix(col, vec3(0.9f, 0.9f, 0.9f), dot(on.zw, on.zw));
    col = mix(col, vec3(0.0f, 0.3f, 0.3f), 0.2f + 0.5f * on.y * on.y);
    col = mix(col, vec3(0.0f, 0.2f, 0.4f), 0.5 * smoothstep(1.2f, 1.3f, abs(on.z) + abs(on.w)));
    col = clamp(col * f * 2.0f, 0.0f, 1.0f);

    vec4 kk;
    vec3 nor = normalize(vec3(
        func(p + vec2(e, 0.0f), kk) - f,
        2.0f * e,
        func(p + vec2(0.0f, e), kk) - f
    ));

    vec3 lig = normalize(vec3(0.9f, 0.2f, -0.4f));
    float dif = clamp(0.3f + 0.7f * dot(nor, lig), 0.0f, 1.0f);
    vec3 lin = vec3(0.7f, 0.9f, 0.95f) * (nor.y * 0.5f + 0.5f) + vec3(0.15f, 0.1f, 0.05f) * dif;
    col *= 1.2f * lin;
    col = 1.0f - col;
    col = 1.1f * col * col * col * 0.6f;

    return col;
}

//ripples
const int MAX_RIPPLES = 16;
uniform int rippleCount;
uniform vec2 ripplePos[MAX_RIPPLES];
uniform float rippleStart[MAX_RIPPLES];

const float TWO_PI = 6.2831853f;
uniform float rippleStrength = 0.6f;
uniform float rippleSpeed    = 2.0f;
uniform float rippleDecay    = 1.0f;
uniform float rippleRadius   = 0.2f;

vec2 ripple(vec2 p) {
    vec2 rippleOffset = vec2(0.0f);
    int count = min(rippleCount, MAX_RIPPLES);

    for (int i = 0; i < count; ++i) {
        float startTime = rippleStart[i];
        float dt = time - startTime;
        if (dt <= 0.0f)
            continue;

        vec2 rippleCoord = ripplePos[i] + framebufferOffset;
        vec2 rp = (2.0f * rippleCoord - resolution.xy) / resolution.y;

        float dist = length(p - rp);

        float t = dt * rippleSpeed;
        float falloff = exp(-dist * rippleDecay) * exp(-t * 0.5f);

        if (falloff > 0.0f && dist <= rippleRadius) {
            float wave = sin(dist * 30.0f - t * TWO_PI) * rippleStrength * falloff;
            vec2 dir = normalize(p - rp + vec2(1e-6f));
            rippleOffset += dir * wave * (1.0f - dist / rippleRadius);
        }
    }

    return rippleOffset;
}

void main() {
    vec2 fragCoord = texCoords * resolution + framebufferOffset;

    vec2 p = (2.0f * fragCoord - resolution.xy) / resolution.y;
    float e = 2.0f / resolution.y;

    p += ripple(p);

    vec3 col = warp(p, e);

    fragColor = vec4(col, 1.0f);
}
