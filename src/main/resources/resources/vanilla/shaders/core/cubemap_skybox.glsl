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

uniform vec3 color = vec3(1.0f);

uniform vec3 skyColor;
uniform vec3 fogColor;
uniform vec3 sunColor;
uniform vec3 starsColor;
uniform vec3 moonColor;
uniform vec3 sunDirection;
uniform vec3 moonDirection;

uniform float fogIntensity = 1.0f;
uniform float sunIntensity = 1.0f;
uniform float starsIntensity = 1.0f;
uniform float starsCoverage = 0.02f;
uniform float starsCutoff = 0.1f;

uniform float moonSize = 0.015f;
uniform float moonAmbient = 0.02f;

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
    float brightness = smoothstep(starsCutoff, 0.0f, dist);
    return brightness * (h / starsCoverage);
}

void main() {
    vec3 dir = normalize(pos);

    //vertical fog gradient (atmospheric scattering)
    float horizon = 1.0f - max(dir.y, 0.0f);
    float horizonBlend = horizon * horizon * fogIntensity;
    vec3 col = mix(skyColor, fogColor, horizonBlend);

    //moon
    float moonCos = cos(moonSize);
    float moonDist = dot(dir, -moonDirection);
    bool hitMoon = (moonDist > moonCos);

    vec3 moonCol = vec3(0.0f);
    if (hitMoon) {
        //3D normal of the moon sphere based on the view ray
        vec3 p = dir - moonDist * -moonDirection;
        float p2 = dot(p, p);
        float sin_r = sin(moonSize);
        float z = sqrt(max(sin_r * sin_r - p2, 0.0f));
        vec3 normal = normalize(p + z * moonDirection);

        //diffuse lighting
        float ndotl = max(dot(normal, -sunDirection), 0.0f);

        //earthshine
        float earthshine = max(dot(normal, moonDirection), 0.0f) * 0.02f;

        moonCol = moonColor * clamp(ndotl + earthshine, moonAmbient, 1.0f);
    }

    //stars
    if (starsIntensity > 0.0f && !hitMoon) {
        //fade stars near horizon and toward sun
        float skyFade = max(dir.y, 0.0f);
        float sunMask = 1.0f - smoothstep(0.0f, 1.0f, max(dot(dir, -sunDirection), 0.0f));
        float starBrightness = stars(dir) * skyFade * sunMask * starsIntensity;
        col = mix(col, starsColor, starBrightness);
    }

    //add moon
    if (hitMoon)
        col += moonCol;

    //sun glow
    if (sunIntensity > 0.0f && !hitMoon) {
        float sunDot = max(dot(dir, -sunDirection), 0.0f);
        float sunGlow = pow(sunDot, 8.0f) * 0.5f;
        col += sunColor * sunGlow * sunIntensity;
    }

    fragColor = vec4(col * color, 1.0f);
}