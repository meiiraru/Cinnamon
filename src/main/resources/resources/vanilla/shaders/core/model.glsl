#type vertex
#include shaders/core/model.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec4 color = vec4(1.0f);

void main() {
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a < 0.5f)
        discard;

    fragColor = tex * color;
}