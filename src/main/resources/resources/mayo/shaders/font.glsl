#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec3 aColor;

uniform mat4 projection;
uniform mat4 view;

flat out int texID;
out vec2 texCoords;
out vec3 color;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1.0f);
    texID = int(aTexID);
    texCoords = aTexCoords;
    color = aColor;
}

#type fragment
#version 330 core

uniform sampler2D textures[16];

flat in int texID;
in vec2 texCoords;
in vec3 color;

out vec4 fragColor;

void main() {
    //if (true) {fragColor = vec4(1.0f, 1.0f, 1.0f, 1.0f); return;}

    //color
    vec4 col = vec4(color, 1.0f);

    if (texID > 0) {
        //texture
        vec4 tex = texture(textures[texID], texCoords);
        if (tex.r < 0.01f)
            discard;

        fragColor = vec4(color.rgb, tex.r);
    } else {
        fragColor = col;
    }
}