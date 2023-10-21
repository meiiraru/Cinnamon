#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in float aTexID;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec4 aColor;
layout (location = 4) in vec3 aNormal;

flat out int texID;
out vec2 texCoords;
out vec3 pos;
out vec4 color;
out vec3 normal;

uniform mat4 projection;
uniform mat4 view;

void main() {
    gl_Position = projection * view * vec4(aPosition, 1.0f);
    pos = aPosition;
    texID = int(aTexID);
    texCoords = aTexCoords;
    color = aColor;
    normal = aNormal;
}

#type fragment
#version 330 core

flat in int texID;
in vec2 texCoords;
in vec4 color;
in vec3 pos;
in vec3 normal;

out vec4 fragColor;

uniform sampler2D textures[16];
uniform vec3 ambientLight;
uniform vec3 lightPos;

void main() {
    //if (true) {fragColor = vec4(normal, 1.0f); return;}

    //color
    vec4 col = color;

    if (texID >= 0) {
        //texture
        vec4 tex = texture(textures[texID], texCoords);
        if (tex.r < 0.01f)
        discard;

        col = vec4(col.rgb, col.a * tex.r);
    }

    //out color
    fragColor = col;

    /*
    //light

    //ambient
    vec3 ambient = ambientLight;

    //diffuse
    vec3 norm = normalize(normal);
    vec3 lightDir = normalize(lightPos - pos);

    float diffuse = max(dot(norm, lightDir), 0.0f);

    vec4 light = vec4(ambient + diffuse, 1.0f);

    //out color
    fragColor = light * col;
    */
}