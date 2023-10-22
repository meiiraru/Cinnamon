#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;

out vec2 texCoords;
out vec3 pos;
out vec3 normal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;

void main() {
    vec4 posVec = vec4(aPosition, 1);
    gl_Position = projection * view * model * posVec;
    pos = (model * posVec).xyz;
    texCoords = aTexCoords;
    normal = aNormal * normalMat;
}

#type fragment
#version 330 core
#include shaders/libs/fog.glsl
#include shaders/libs/light.glsl

in vec2 texCoords;
in vec3 pos;
in vec3 normal;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec3 color;
uniform vec3 camPos;

void main() {
    //texture
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a <= 0.01f)
        discard;

    //color
    vec4 col = vec4(color, 1) * tex;

    //lighting
    col = calculateLighting(col);

    //fog
    col = calculateFog(pos, camPos, col);

    //final color
    fragColor = col;
}