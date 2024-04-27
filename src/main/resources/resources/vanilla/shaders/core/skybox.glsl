#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;

out vec3 texCoords;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main() {
    texCoords = aPosition;
    vec4 pos = projection * view * model * vec4(aPosition, 1.0f);
    gl_Position = pos.xyww;
}

#type fragment
#version 330 core

in vec3 texCoords;

out vec4 fragColor;

uniform samplerCube skybox;

void main() {
    fragColor = texture(skybox, texCoords);
}