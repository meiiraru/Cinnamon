#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec3 aColor;
layout (location = 4) in vec3 aNormal;

uniform mat4 view;
uniform mat4 projection;

out float texID;
out vec2 texCoords;
out vec3 color;
out vec3 normal;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1.0f);
    texID = aTexID;
    texCoords = aTexCoords;
    color = aColor;
    normal = aNormal;
}

#type fragment
#version 330 core

uniform sampler2D textures[16];
//uniform vec2 uvOffset;

in float texID;
in vec2 texCoords;
in vec3 color;

out vec4 fragColor;

void main() {
    //color
    vec4 col = vec4(color, 1.0f);

    if (texID > 0.0f) {
        //texture
        vec4 tex = texture(textures[int(texID)], texCoords);
        if (tex.a == 0.0f)
            discard;

        fragColor = tex * col;
    } else {
        fragColor = col;
    }
}