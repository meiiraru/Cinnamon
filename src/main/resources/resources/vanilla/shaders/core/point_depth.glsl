#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec3 worldPos;
out vec2 texCoords;

uniform mat4 lightSpaceMatrix;
uniform mat4 model;

void main() {
    vec4 pos = model * vec4(aPosition, 1.0f);
    gl_Position = lightSpaceMatrix * pos;
    worldPos = pos.xyz;
    texCoords = aTexCoords;
}

#type fragment
#version 330 core

in vec3 worldPos;
in vec2 texCoords;

out highp float gl_FragDepth;

uniform sampler2D textureSampler;
uniform vec3 lightPos;
uniform float farPlane;

void main() {
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a < 0.5f)
        discard;

    float lightDistance = length(worldPos - lightPos);
    lightDistance = lightDistance / farPlane;
    gl_FragDepth = lightDistance;
}