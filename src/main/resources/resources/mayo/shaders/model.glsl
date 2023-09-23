#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;

out vec2 texCoords;
//out vec3 pos;
out vec3 normal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main() {
    gl_Position = projection * view * model * vec4(aPosition, 1.0f);
    //pos = vec3(model * vec4(aPosition, 1.0f));
    texCoords = aTexCoords;
    normal = aNormal;
}

#type fragment
#version 330 core

in vec2 texCoords;
//in vec3 pos;
in vec3 normal;

out vec4 fragColor;

uniform sampler2D textureSampler;
//uniform vec3 ambientLight;
//uniform vec3 lightPos;

void main() {
    //if (true) {fragColor = vec4(normal, 1.0f); return;}

    //texture
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a <= 0.01f)
        discard;

    //out color
    fragColor = tex;

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
    fragColor = light * tex;
    */
}