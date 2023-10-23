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
#include shaders/libs/fog.glsl

struct Material {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
    sampler2D diffuseTex;
    sampler2D specularTex;
    sampler2D emissiveTex;
};

struct Light {
    vec3 pos;
    vec3 diffuse;
    vec3 specular;
};

in vec2 texCoords;
in vec3 pos;
in vec3 normal;

out vec4 fragColor;

uniform vec3 color;
uniform vec3 camPos;

uniform Material material;

uniform vec3 ambient;
uniform int lightCount;
uniform Light lights[4];

vec3 calculateDiffuse(Light light, vec3 normal, float attenuation, vec3 lightDir) {
    //minimum of 0 otherwise it will override the ambient lgiht
    float diff = max(dot(normal, lightDir), 0);
    return attenuation * light.diffuse * material.diffuse * diff;
}

vec3 calculateSpecular(Light light, vec3 normal, vec3 viewDir, float attenuation, vec3 lightDir) {
    vec3 reflectDir = reflect(-lightDir, normal);
    float spec = pow(max(dot(viewDir, reflectDir), 0), material.shininess);
    return attenuation * light.specular * material.specular * spec;
}

vec4 applyLighting(vec4 diffTex) {
    // ambient //

    vec3 ambient = ambient * material.ambient * diffTex.rgb;

    // diffuse and specular //

    vec3 norm = normalize(normal);
    vec3 viewDir = normalize(camPos - pos);

    vec3 diffuse = vec3(0);
    vec3 specular = vec3(0);

    for (int i = 0; i < lightCount; i++) {
        Light l = lights[i];

        vec3 diffDist = l.pos - pos;
        float attenuation = 1 / length(diffDist);
        vec3 lightDir = normalize(diffDist);

        diffuse += calculateDiffuse(l, norm, attenuation, lightDir);
        specular += calculateSpecular(l, norm, viewDir, attenuation, lightDir);
    }

    diffuse *= diffTex.rgb;
    specular *= texture(material.specularTex, texCoords).rgb;

    // emissive //

    vec3 emissive = texture(material.emissiveTex, texCoords).rgb;

    //return light
    return vec4(ambient + diffuse + specular + emissive, diffTex.a);
}

void main() {
    //if (true) {fragColor = vec4(normal, 1); return;}

    //texture
    vec4 tex = texture(material.diffuseTex, texCoords);
    if (tex.a <= 0.01f)
        discard;

    //color
    vec4 col = vec4(color, 1);

    //lighting
    col *= applyLighting(tex);

    //fog
    col = calculateFog(pos, camPos, col);

    //final color
    fragColor = col;
}