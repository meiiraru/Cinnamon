#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec4 aColor;
layout (location = 3) in vec3 aNormal;

flat out int texID;
out vec3 pos;
out vec4 color;
out vec3 normal;
out vec4 texProj;

uniform mat4 projection;
uniform mat4 view;
uniform vec3 camPos;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1.0f);
    texID = int(aTexID);
    pos = aPosition;
    color = aColor;
    normal = aNormal;

    texProj = gl_Position * 0.5f;
    texProj.xy = vec2(texProj.x + texProj.w, -(texProj.y + texProj.w));
    texProj.zw = gl_Position.zw;
}

#type fragment
#version 330 core

flat in int texID;
in vec3 pos;
in vec4 color;
in vec3 normal;
in vec4 texProj;

layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gEmissive;

uniform sampler2D textures[16];

void main() {
    //color
    vec4 col = color;
    
    if (texID >= 0) {
        vec4 tex = textureProj(textures[texID], texProj);
        if (tex.a < 0.01f)
            discard;

        col *= tex;
    }

    //gBuffer outputs
    gAlbedo = col;
    gNormal = vec4(normal, 1.0f);
    gORM = vec4(1.0f, 1.0f, 0.0f, 1.0f);
    gEmissive = vec4(0.0f, 0.0f, 0.0f, 1.0f);
}
