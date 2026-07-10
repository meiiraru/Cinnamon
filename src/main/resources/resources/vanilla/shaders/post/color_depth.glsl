#type vertex
#version 330 core
layout (location = 0) in vec2 aPosition;

void main() {
    gl_Position = vec4(aPosition, 0.0f, 1.0f);
}

#type fragment
#version 330 core

out highp float gl_FragDepth;

uniform float depth;

void main() {
    gl_FragDepth = depth;
}