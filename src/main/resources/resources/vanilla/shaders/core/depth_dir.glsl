#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec2 texCoords;

uniform mat4 model;

void main() {
    gl_Position = model * vec4(aPosition, 1.0f);
    texCoords = aTexCoords;
}

#type geometry
#version 400 core

layout(triangles, invocations = 4) in;
layout(triangle_strip, max_vertices = 3) out;

in vec2 texCoords[];
out vec2 g_texCoords;

uniform mat4 cascadeMatrices[16];

void main() {
    for (int i = 0; i < 3; i++) {
        gl_Position = cascadeMatrices[gl_InvocationID] * gl_in[i].gl_Position;
        g_texCoords = texCoords[i];
        gl_Layer = gl_InvocationID;
        EmitVertex();
    }
    EndPrimitive();
}

#type fragment
#version 330 core

in vec2 g_texCoords;

uniform sampler2D textureSampler;

void main() {
    vec4 tex = texture(textureSampler, g_texCoords);
    if (tex.a < 0.01f)
        discard;
}
