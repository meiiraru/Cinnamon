#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec2 texCoords;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 uvMatrix;

void main() {
    gl_Position = projection * view * model * vec4(aPosition, 1.0f);
    texCoords = (vec3(aTexCoords, 1.0f) * uvMatrix).xy;
}

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