#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;

out vec3 texCoords;

uniform mat4 projection;
uniform mat4 view;

void main() {
    texCoords = aPosition;
    vec4 pos = projection * mat4(mat3(view)) * vec4(aPosition, 1.0f);
    gl_Position = pos.xyww;
}

#type fragment
#version 330 core

in vec3 texCoords;

out vec4 fragColor;

uniform samplerCube skybox;
uniform mat3 rotation;

void main() {
    fragColor = texture(skybox, texCoords * rotation);
}