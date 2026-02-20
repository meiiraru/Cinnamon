#type vertex
#version 330 core

layout (location = 0) in vec2 aPosition;

out vec2 localPos;

uniform mat4 projection;
uniform mat4 view;

uniform vec3 camPos;

uniform vec3 lightPos;
uniform vec3 lightDir;
uniform float height;
uniform float radius;

void main() {
    vec3 up = lightDir;
    vec3 dirToCam = normalize(camPos - lightPos);
    vec3 forward = normalize(cross(dirToCam, up));

    float dist = (1.0f - aPosition.y) * 0.5f * height;
    float widthOffset = aPosition.x * radius;

    vec3 worldPos = lightPos + up * dist + forward * widthOffset;

    gl_Position = projection * view * vec4(worldPos, 1.0f);
    localPos = aPosition;
}

#type fragment
#version 330 core

in vec2 localPos;

out vec4 fragColor;

uniform float beamIntensity;
uniform vec2 fadeRange = vec2(0.9f, 0.6f);
uniform vec3 color;

void main() {
    float y = (1.0f - localPos.y) * 0.5f;
    float x = y - abs(localPos.x);

    float edgeFade = smoothstep(0.0f, 1.0f - fadeRange.x, x);
    float heightFade = 1.0f - smoothstep(0.0f, fadeRange.y, y);

    float intensity = edgeFade * heightFade * beamIntensity;
    intensity = clamp(intensity, 0.0f, 1.0f);

    fragColor = vec4(color * intensity, intensity);
}