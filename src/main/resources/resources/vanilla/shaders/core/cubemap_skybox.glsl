#type vertex
#version 330 core
layout (location = 0) in vec3 aPos;

out vec3 pos;

uniform mat4 projection;
uniform mat4 view;

void main() {
    pos = aPos;
    gl_Position = projection * view * vec4(aPos, 1.0f);
}

#type fragment
#version 330 core

in vec3 pos;

out vec4 fragColor;

uniform vec3 skyColor;
uniform vec3 fogColor;
uniform vec3 sunColor;
uniform vec3 sunDirection;

uniform float fogIntensity = 1.0f;
uniform float sunIntensity = 1.0f;
uniform float starsIntensity = 1.0f;
uniform float starsCoverage = 0.01f;

float hash(vec3 p) {
    p = fract(p * vec3(127.1f, 311.7f, 74.7f));
    p += dot(p, p.yzx + 19.19f);
    return fract((p.x + p.y) * p.z);
}

float stars(vec3 dir) {
    //scale the direction to a grid cell
    const float gridSize = 10000.0f;
    vec3 grid = floor(dir * gridSize);
    float h = hash(grid);

    //only a fraction of cells become stars
    if (h > starsCoverage)
        return 0.0f;

    //sub-cell position for a soft point
    vec3 cell = fract(dir * gridSize) - 0.5f;
    float dist = dot(cell, cell);
    float brightness = smoothstep(0.25f, 0.0f, dist);
    return brightness * (h / starsCoverage);
}

void main() {
    vec3 dir = normalize(pos);

    //vertical fog gradient
    float horizon = 1.0f - max(dir.y, 0.0f);
    float horizonBlend = horizon * horizon * fogIntensity;
    vec3 color = mix(skyColor, fogColor, horizonBlend);

    //sun glow
    if (sunIntensity > 0.0f) {
        float sunDot = max(dot(dir, -sunDirection), 0.0f);
        float sunGlow = pow(sunDot, 8.0f) * 0.5f;
        color += sunColor * sunGlow * sunIntensity;
    }

    //stars
    if (starsIntensity > 0.0f) {
        //fade stars near horizon and toward sun
        float skyFade = max(dir.y, 0.0f);
        float sunMask = 1.0f - smoothstep(0.0f, 1.0f, max(dot(dir, -sunDirection), 0.0f));
        float starBrightness = stars(dir) * skyFade * sunMask * starsIntensity;
        color += starBrightness;
    }

    fragColor = vec4(color, 1.0f);
}