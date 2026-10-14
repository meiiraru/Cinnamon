#type vertex
#include shaders/core/world_main.vsh

#type fragment
#version 330 core
//#include shaders/libs/transparent_dither.glsl

flat in int texID;
in vec2 texCoords;
in vec3 pos;
in vec4 color;
in vec3 normal;

layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gEmissive;

uniform sampler2D textures[16];

void main() {
    //color
    vec4 col = color;

    if (texID >= 0) {
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

        col *= tex;
    }

    //if (shouldDiscard(col, pos)) {
    //    discard;
    //} else {
    //    col.a = 1.0f;
    //}

    if (col.a < 0.01f)
        discard;

    //gBuffer outputs
    gAlbedo = col;
    gNormal = vec4(normal, 1.0f);
    gORM = vec4(1.0f, 1.0f, 0.0f, 1.0f);
    gEmissive = vec4(0.0f, 0.0f, 0.0f, 1.0f);
}