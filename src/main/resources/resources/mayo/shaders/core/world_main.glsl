#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec4 aColor;
layout (location = 4) in vec3 aNormal;

flat out int texID;
out vec2 texCoords;
out vec3 pos;
out vec4 color;
out vec3 normal;

uniform mat4 projection;
uniform mat4 view;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1);
    texID = int(aTexID);
    texCoords = aTexCoords;
    pos = aPosition;
    color = aColor;
    normal = aNormal;
}

#type fragment
#version 330 core
#include shaders/libs/light.glsl
#include shaders/libs/fog.glsl

flat in int texID;
in vec2 texCoords;
in vec3 pos;
in vec4 color;
in vec3 normal;

out vec4 fragColor;

uniform sampler2D textures[16];
uniform vec3 camPos;

void main() {
    //color
    vec4 col = color;

    if (texID >= 0) {
        //texture
        vec4 tex = texture(textures[texID], texCoords);
        if (tex.a < 0.01f)
            discard;

        col *= tex;
    }

    //ambient light
    col *= calculateLight(pos, normal);

    //fog
    col = calculateFog(pos, camPos, col);

    //final color
    fragColor = col;
}