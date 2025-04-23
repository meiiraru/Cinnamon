#type vertex
#include shaders/core/model.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec3 color = vec3(1.0f);

void main() {
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a <= 0.01f)
        discard;

    fragColor = vec4(color, 1.0f) * tex;
}