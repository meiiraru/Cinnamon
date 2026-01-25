#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec2 texCoords;

uniform mat4 lightSpaceMatrix;
uniform mat4 model;

void main() {
    gl_Position = lightSpaceMatrix * model * vec4(aPosition, 1.0f);
    texCoords = aTexCoords;
}

#type fragment
#version 330 core

in vec2 texCoords;

uniform sampler2D textureSampler;

void main() {
    //gl_FragDepth = gl_FragCoord.z;
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a < 0.01f)
        discard;
}