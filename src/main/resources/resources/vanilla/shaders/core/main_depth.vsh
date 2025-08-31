#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;

flat out int texID;
out vec2 texCoords;

uniform mat4 lightSpaceMatrix;

void main() {
    gl_Position = lightSpaceMatrix * vec4(aPosition, 1.0f);
    texID = int(aTexID);
    texCoords = aTexCoords;
}