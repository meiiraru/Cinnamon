#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec4 aColor;
layout (location = 4) in float aIndex;

flat out int texID;
out vec2 texCoords;
out vec4 color;

uniform mat4 projection;
uniform mat4 view;

const float Z_OFFSET = 0.001f;

void main() {
    float z = aPosition.z + Z_OFFSET * int(aIndex);
    gl_Position = projection * view * vec4(aPosition.xy, z, 1.0f);
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
    //if (true) {fragColor = vec4(1.0f, 1.0f, 1.0f, 1.0f); return;}

    //color
    vec4 col = color;

    if (texID > 0) {
        //texture
        vec4 tex = texture(textures[texID], texCoords);
        if (tex.r < 0.01f)
            discard;

        fragColor = vec4(color.rgb, color.a * tex.r);
    } else {
        fragColor = color;
    }
}