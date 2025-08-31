#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec4 aColor;
layout (location = 4) in vec3 aNormal;

flat out int texID;
out vec2 texCoords;
out vec3 pos;
out vec4 color;
out vec3 normal;

uniform mat4 projection;
uniform mat4 view;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1.0f);
    texID = int(aTexID);
    texCoords = aTexCoords;
    pos = aPosition;
    color = aColor;
    normal = normalize(aNormal);
}