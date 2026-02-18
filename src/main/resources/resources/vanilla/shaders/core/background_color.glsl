#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

out vec4 fragColor;

uniform vec4 color = vec4(vec3(0.125f), 1.0f);

void main() {
  fragColor = color;
}