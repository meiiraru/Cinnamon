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

uniform sampler2D screenTexture;
uniform vec2 textelSize;

const float factor = 8;

void main() {
    vec2 screenSize = 1 / textelSize / factor;
    vec2 coords = floor(texCoords * screenSize) / screenSize;
    fragColor = texture(screenTexture, coords);
}