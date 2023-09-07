#type vertex
#version 330 core

layout (location = 0) in vec4 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec3 aColor;

out float texID;
out vec2 texCoords;
out vec3 color;

void main() {
    gl_Position = aPosition;
    texID = aTexID;
    texCoords = aTexCoords;
    color = aColor;
}

#type fragment
#version 330 core

uniform sampler2D textures[16];

in float texID;
in vec2 texCoords;
in vec3 color;

out vec4 fragColor;

void main() {
    //texture
    vec4 tex = texture(textures[int(texID)], texCoords);
    if (tex.r < 0.01f)
        discard;

    fragColor = vec4(color.r, color.g, color.b, tex.r);
}