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

    vec2[] offsets = vec2[](
        //top
        vec2(-textelSize.x, textelSize.y),
        vec2(0,             textelSize.y),
        vec2(textelSize.x,  textelSize.y),

        //center
        vec2(-textelSize.x, 0),
        vec2(0,             0),
        vec2(textelSize.x,  0),

        //bottom
        vec2(-textelSize.x, -textelSize.y),
        vec2(0,             -textelSize.y),
        vec2(textelSize.x,  -textelSize.y)
    );

    vec4 sampleTex[9];
    for (int i = 0; i < 9; i++) {
        sampleTex[i] = texture(screenTexture, texCoords.xy + offsets[i]);
    }

    vec4 col = vec4(0, 0, 0, 1);
    for (int i = 0; i < 9; i++)
        col += sampleTex[i] * kernel[i];

    fragColor = col;
}