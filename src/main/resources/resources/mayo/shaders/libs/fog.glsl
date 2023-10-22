
uniform vec3 fogColor;
uniform float fogStart;
uniform float fogEnd;

vec4 calculateFog(vec3 pos, vec3 camPos, vec4 color) {
    float fogDistance = length(pos - camPos);
    float fogDelta = smoothstep(fogStart, fogEnd, fogDistance);

    return vec4(mix(color.rgb, fogColor, fogDelta), color.a);
}