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

    fragColor = vec4(color, 1.0f);
}