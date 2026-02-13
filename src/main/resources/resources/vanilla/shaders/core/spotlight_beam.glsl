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
uniform vec2 fadeRange = vec2(0.25f, 0.35f);
uniform vec3 color;

void main() {
    float fadeWidth = max(fadeRange.x - abs(localPos.x), 0.0f);
    float fadeHeight = max(fadeRange.y * 2.0f - (1.0f - localPos.y), 0.0f);
    float intensity = min(fadeWidth * fadeHeight * beamIntensity, 1.0f);

    fragColor = vec4(color, 1.0f) * intensity;
}