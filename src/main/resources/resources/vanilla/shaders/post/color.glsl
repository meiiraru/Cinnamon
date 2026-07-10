#type vertex
#version 330 core
layout (location = 0) in vec2 aPosition;

void main() {
    gl_Position = vec4(aPosition, 0.0f, 1.0f);
}

#type fragment
#version 330 core

out vec4 fragColor;

uniform vec4 color;

void main() {
    fragColor = color;
}