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

const float intensity = 5.0f;
const float kernel[] = float[](
    1.0f / 16, 2.0f / 16, 1.0f / 16,
    2.0f / 16, 4.0f / 16, 2.0f / 16,
    1.0f / 16, 2.0f / 16, 1.0f / 16
);

void main() {
    vec2 offset = textelSize * intensity;
    vec4 col = vec4(0.0f);

    for (int y = -1, i = 0; y <= 1; y++) {
        for (int x = -1; x <= 1; x++, i++) {
            vec4 tex = texture(screenTexture, texCoords.xy + vec2(offset.x * x, offset.y * y));
            col += tex * kernel[i];
        }
    }

    fragColor = col;
}