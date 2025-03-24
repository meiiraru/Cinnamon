#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec4 aColor;

flat out int texID;
out vec2 texCoords;
out vec4 color;

uniform mat4 projection;
uniform mat4 view;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1.0f);
    texID = int(aTexID);
    texCoords = aTexCoords;
    color = aColor;
}

#type fragment
#version 330 core

flat in int texID;
in vec2 texCoords;
in vec4 color;

out vec4 fragColor;

uniform sampler2D textures[16];

void main() {
    if (texID < 0) {
        fragColor = color;
        return;
    }

    //texture
    vec4 tex = texture(textures[texID], texCoords);
    if (tex.a < 0.01f)
        discard;

    fragColor = tex * color;
}