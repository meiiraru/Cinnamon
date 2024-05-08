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
uniform vec2 textelSize;

const float intensity = 2.5f;

void main() {
    vec4 tex = texture(screenTexture, texCoords);

    vec2 offset = vec2(textelSize.x, textelSize.y) * intensity;
    tex.r = texture(screenTexture, texCoords + offset).r;
    tex.gb = texture(screenTexture, texCoords - offset).gb;

    fragColor = tex;
}