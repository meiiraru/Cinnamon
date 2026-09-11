
uniform vec3 fogColor;
uniform float fogStart;
uniform float fogEnd;

float getFogFactor(vec3 pos, vec3 camPos) {
    float fogDistance = length(pos - camPos);
    return smoothstep(fogStart, fogEnd, fogDistance);
}

vec4 calculateFog(vec3 pos, vec3 camPos, vec4 color) {
    float fogDelta = getFogFactor(pos, camPos);
    return vec4(mix(color.rgb, fogColor, fogDelta), color.a);
}