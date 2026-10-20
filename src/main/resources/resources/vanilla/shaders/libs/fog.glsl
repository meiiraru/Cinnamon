
uniform vec3 fogColor;
uniform float fogStart;
uniform float fogEnd;

uniform vec3 sunColor;
uniform vec3 sunDir;
uniform float sunIntensity;

float getFogFactor(vec3 pos, vec3 camPos) {
    float fogDistance = length(pos - camPos);
    return smoothstep(fogStart, fogEnd, fogDistance);
}

vec4 calculateFog(vec3 pos, vec3 camPos, vec4 color) {
    float fogDelta = getFogFactor(pos, camPos);

    if (fogDelta <= 0.001f)
        return color;

    vec3 finalFogColor = fogColor;

    if (sunIntensity > 0.0f) {
        vec3 viewDir = normalize(pos - camPos);
        float sunDot = dot(viewDir, -sunDir);

        if (sunDot > 0.2f) {
            float sunGlow = pow(sunDot, 8.0f) * 0.5f;
            finalFogColor += sunColor * sunGlow * sunIntensity;
        }
    }

    return vec4(mix(color.rgb, finalFogColor, fogDelta), color.a);
}