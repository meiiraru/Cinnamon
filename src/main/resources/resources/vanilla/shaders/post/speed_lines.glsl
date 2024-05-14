#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform float time;
uniform float speed;
uniform float lines;
uniform float rotationSpeed;
uniform float intensity;
uniform float maskSize;
uniform float maskStrength;
uniform vec3 color;

const float TWO_PI = 6.28318548202515f;

vec3 permute(vec3 x) {
    return mod(((x * 34.0f) + 1.0f) * x, 289.0f);
}

float snoise(vec2 v) {
    const vec4 C = vec4(0.211324865405187f, 0.366025403784439f, -0.577350269189626f, 0.024390243902439f);
    vec2 i = floor(v + dot(v, C.yy));
    vec2 x0 = v - i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0f, 0.0f) : vec2(0.0f, 1.0f);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod(i, 289.0f);
    vec3 p = permute(permute(i.y + vec3(0.0f, i1.y, 1.0f)) + i.x + vec3(0.0f, i1.x, 1.0f));
    vec3 m = max(0.5f - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0f);
    m = m * m;
    m = m * m;
    vec3 x = 2.0f * fract(p * C.www) - 1.0f;
    vec3 h = abs(x) - 0.5f;
    vec3 ox = floor(x + 0.5f);
    vec3 a0 = x - ox;
    m *= 1.79284291400159f - 0.85373472095314f * (a0 * a0 + h * h);
    vec3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;

    return 130.0f * dot(m, g);
}

void main() {
    vec2 uv = texCoords * 2.0f - 1.0f;

    vec2 v = vec2(length(uv) / speed, atan(uv.x, uv.y) * (1.0f / TWO_PI) * lines) + vec2(time * -rotationSpeed, 0.0f);
    float noise = snoise(v) * 0.5f + 0.5f;
    float speedLines = clamp((noise - (1.0f - intensity)) / intensity, 0.0f, 1.0f);

    float mask = smoothstep(maskSize, maskSize + maskStrength, length(uv));

    fragColor = mix(texture(colorTex, texCoords), vec4(color, 1.0f), speedLines * mask);
} 