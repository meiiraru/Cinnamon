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
uniform sampler2D depthTexture;
uniform vec2 textelSize;

const float kernel[] = float[](
    1,  1, 1,
    1, -8, 1,
    1,  1, 1
);

void main() {
    if (texture(depthTexture, texCoords).x == 1)
        discard;

    vec4 col = vec4(0, 0, 0, 1);

    for (int y = -1, i = 0; y <= 1; y++) {
        for (int x = -1; x <= 1; x++, i++) {
            vec4 tex = texture(screenTexture, texCoords.xy + vec2(textelSize.x * x, textelSize.y * y));
            col += tex * kernel[i];
        }
    }

    fragColor = col;
}