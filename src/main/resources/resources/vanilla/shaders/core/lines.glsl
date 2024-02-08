#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec4 aColor;

out vec4 color;

uniform mat4 projection;
uniform mat4 view;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1.0f);
    color = aColor;
}

#type fragment
#version 330 core

in vec4 color;

out vec4 fragColor;

void main() {
    fragColor = color;
}