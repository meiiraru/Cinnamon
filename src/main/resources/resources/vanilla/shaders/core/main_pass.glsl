#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in int aTexID;
layout (location = 2) in vec2 aTexCoords;

flat out int texID;
out vec2 texCoords;

uniform mat4 projection;
uniform mat4 view;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1.0f);
    texID = aTexID;
    texCoords = aTexCoords;
}

#type fragment
#version 330 core

flat in int texID;
in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D textures[16];
uniform vec4 color = vec4(1.0f);

void main() {
    if (texID < 0) {
        fragColor = color;
        return;
    }

    //texture
    vec4 tex = vec4(0.0f);
    switch(int(texID)) {
        case  0: tex = texture(textures[0],  texCoords); break;
        case  1: tex = texture(textures[1],  texCoords); break;
        case  2: tex = texture(textures[2],  texCoords); break;
        case  3: tex = texture(textures[3],  texCoords); break;
        case  4: tex = texture(textures[4],  texCoords); break;
        case  5: tex = texture(textures[5],  texCoords); break;
        case  6: tex = texture(textures[6],  texCoords); break;
        case  7: tex = texture(textures[7],  texCoords); break;
        case  8: tex = texture(textures[8],  texCoords); break;
        case  9: tex = texture(textures[9],  texCoords); break;
        case 10: tex = texture(textures[10], texCoords); break;
        case 11: tex = texture(textures[11], texCoords); break;
        case 12: tex = texture(textures[12], texCoords); break;
        case 13: tex = texture(textures[13], texCoords); break;
        case 14: tex = texture(textures[14], texCoords); break;
        case 15: tex = texture(textures[15], texCoords); break;
    }

    if (tex.a < 0.01f)
        discard;

    fragColor = color;
}