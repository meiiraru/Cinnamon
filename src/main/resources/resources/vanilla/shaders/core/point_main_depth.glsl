#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;

flat out int texID;
out vec3 worldPos;
out vec2 texCoords;

uniform mat4 lightSpaceMatrix;

void main() {
    vec4 pos = vec4(aPosition, 1.0f);
    gl_Position = lightSpaceMatrix * pos;
    texID = int(aTexID);
    worldPos = pos.xyz;
    texCoords = aTexCoords;
}

#type fragment
#version 330 core

flat in int texID;
in vec3 worldPos;
in vec2 texCoords;

out highp float gl_FragDepth;

uniform sampler2D textures[16];
uniform vec3 lightPos;
uniform float farPlane;

void main() {
    if (texID < 0)
        return;

    vec4 tex = texture(textures[texID], texCoords);
    if (tex.a < 0.01f)
        discard;

    float lightDistance = length(worldPos - lightPos);
    lightDistance = lightDistance / farPlane;
    gl_FragDepth = lightDistance;
}