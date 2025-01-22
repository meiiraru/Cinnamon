#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main() {
    gl_Position = projection * view * model * vec4(aPosition, 1.0f);
}

#type fragment
#version 330 core

out vec4 fragColor;

uniform vec3 color = vec3(1.0f);

void main() {
    fragColor = vec4(color, 1.0f);
}