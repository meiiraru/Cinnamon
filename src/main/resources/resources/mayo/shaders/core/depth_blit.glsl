#type vertex
#version 330 core

layout (location = 0) in vec2 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec2 texCoords;

void main() {
    gl_Position = vec4(aPosition, 0, 1);
    texCoords = aTexCoords;
}

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D depthTexture;

void main() {
    float depthValue = texture(depthTexture, texCoords).r;
    fragColor = vec4(vec3(depthValue), 1);
}