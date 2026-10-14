#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in int aTexID;
layout (location = 2) in vec2 aTexCoords;

flat out int texID;
out vec2 texCoords;

void main() {
    gl_Position = vec4(aPosition, 1.0f);
    texID = aTexID;
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
        vec4 tex = vec4(0.0f);
        switch(int(g_texID)) {
            case  0: tex = texture(textures[0],  g_texCoords); break;
            case  1: tex = texture(textures[1],  g_texCoords); break;
            case  2: tex = texture(textures[2],  g_texCoords); break;
            case  3: tex = texture(textures[3],  g_texCoords); break;
            case  4: tex = texture(textures[4],  g_texCoords); break;
            case  5: tex = texture(textures[5],  g_texCoords); break;
            case  6: tex = texture(textures[6],  g_texCoords); break;
            case  7: tex = texture(textures[7],  g_texCoords); break;
            case  8: tex = texture(textures[8],  g_texCoords); break;
            case  9: tex = texture(textures[9],  g_texCoords); break;
            case 10: tex = texture(textures[10], g_texCoords); break;
            case 11: tex = texture(textures[11], g_texCoords); break;
            case 12: tex = texture(textures[12], g_texCoords); break;
            case 13: tex = texture(textures[13], g_texCoords); break;
            case 14: tex = texture(textures[14], g_texCoords); break;
            case 15: tex = texture(textures[15], g_texCoords); break;
        }

        if (tex.a < 0.01f)
            discard;
    }
}
