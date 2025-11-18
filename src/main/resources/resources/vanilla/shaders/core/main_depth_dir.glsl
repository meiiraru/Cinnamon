#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;

flat out int texID;
out vec2 texCoords;

void main() {
    gl_Position = vec4(aPosition, 1.0f);
    texID = int(aTexID);
    texCoords = aTexCoords;
}

#type geometry
#version 400 core

layout(triangles, invocations = 4) in;
layout(triangle_strip, max_vertices = 3) out;

flat in int texID[];
in vec2 texCoords[];

flat out int g_texID;
out vec2 g_texCoords;

uniform mat4 cascadeMatrices[16];

void main() {
    for (int i = 0; i < 3; i++) {
        gl_Position = cascadeMatrices[gl_InvocationID] * gl_in[i].gl_Position;
        gl_Layer = gl_InvocationID;
        g_texID = texID[i];
        g_texCoords = texCoords[i];
        EmitVertex();
    }
    EndPrimitive();
}

#type fragment
#version 330 core

flat in int g_texID;
in vec2 g_texCoords;

uniform sampler2D textures[16];

void main() {
    if (g_texID >= 0) {
        vec4 tex = texture(textures[g_texID], g_texCoords);
        if (tex.a < 0.5f)
            discard;
    }
}
