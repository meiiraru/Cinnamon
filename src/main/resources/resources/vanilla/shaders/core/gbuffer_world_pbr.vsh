#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec3 aTangent;

out vec2 texCoords;
out vec3 pos;
out mat3 TBN;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;

void main() {
    vec4 worldPos = model * vec4(aPosition, 1.0f);
    gl_Position = projection * view * worldPos;
    pos = worldPos.xyz;
    texCoords = aTexCoords;

    vec3 T = normalize(normalMat * aTangent);
    vec3 N = normalize(normalMat * aNormal);
    T = normalize(T - dot(T, N) * N);
    TBN = mat3(T, cross(N, T), N);
}