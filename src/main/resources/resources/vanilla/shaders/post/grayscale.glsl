#type vertex
#version 330 core

layout (location = 0) in vec2 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec2 texCoords;

void main() {
    gl_Position = vec4(aPosition, 0.0f, 1.0f);
    texCoords = aTexCoords;
}

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D screenTexture;

void main() {
    float brightness = dot(texture(screenTexture, texCoords).rgb, vec3(0.2126f, 0.7152f, 0.0722f));
    brightness = pow(brightness, 1.0f / 2.2f);
    fragColor = vec4(vec3(brightness), 1.0f);
}