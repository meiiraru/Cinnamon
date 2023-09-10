#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec2 texCoords;
out vec3 normal;

void main() {
    gl_Position = projection * view * model * vec4(aPosition, 1.0f);
    texCoords = aTexCoords;
    normal = aNormal;
}

#type fragment
#version 330 core

uniform sampler2D textureSampler;

in vec2 texCoords;
in vec3 normal;

out vec4 fragColor;

void main() {
    fragColor = vec4(normal, 1.0f);
//    //texture
//    vec4 tex = texture(textureSampler, texCoords);
//    if (tex.a <= 0.01f)
//        discard;
//
//    //out color
//    fragColor = tex;
}