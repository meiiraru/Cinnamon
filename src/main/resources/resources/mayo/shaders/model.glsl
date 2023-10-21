#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;

out vec2 texCoords;
out vec3 pos;
out vec3 normal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;

void main() {
    vec4 posVec = vec4(aPosition, 1);
    gl_Position = projection * view * model * posVec;
    pos = (model * posVec).xyz;
    texCoords = aTexCoords;
    normal = aNormal * normalMat;
}

#type fragment
#version 330 core

in vec2 texCoords;
in vec3 pos;
in vec3 normal;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec3 color;
uniform vec3 camPos;

uniform vec3 fogColor;
uniform float fogStart;
uniform float fogEnd;

uniform vec3 ambientLight;
uniform vec3 lightPos;

void main() {
    //texture
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a <= 0.01f)
        discard;

    //color
    vec4 col = vec4(color, 1) * tex;

    // >> lighting here <<

    //fog
    float fogDistance = length(pos - camPos);
    float fogDelta = smoothstep(fogStart, fogEnd, fogDistance);

    //final color
    fragColor = vec4(mix(col.rgb, fogColor, fogDelta), col.a);

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
    fragColor = light * tex * col;
    */
}