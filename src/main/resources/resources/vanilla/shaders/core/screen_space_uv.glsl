#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec4 aColor;

flat out int texID;
out vec3 pos;
out vec4 color;
out vec4 texProj;

uniform mat4 projection;
uniform mat4 view;
uniform vec3 camPos;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1.0f);
    texID = int(aTexID);
    pos = aPosition;
    color = aColor;

    texProj = gl_Position * 0.5f;
    texProj.xy = vec2(texProj.x + texProj.w, -(texProj.y + texProj.w));
    texProj.zw = gl_Position.zw;
}

#type fragment
#version 330 core

flat in int texID;
in vec3 pos;
in vec4 color;
in vec4 texProj;

out vec4 fragColor;

uniform sampler2D textures[16];

void main() {
    if (texID < 0) {
        fragColor = color;
        return;
    }

    //texture
    vec4 tex = textureProj(textures[texID], texProj);
    if (tex.a < 0.01f)
        discard;

    fragColor = tex * color;
}