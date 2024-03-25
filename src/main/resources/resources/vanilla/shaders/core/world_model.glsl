#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;

out vec2 texCoords;
out vec3 pos;
out vec3 normal;
out vec4 shadowPos;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;
uniform mat4 lightSpaceMatrix;

void main() {
    vec4 posVec = model * vec4(aPosition, 1);
    gl_Position = projection * view * posVec;
    pos = posVec.xyz;
    texCoords = aTexCoords;
    normal = aNormal * normalMat;
    shadowPos = lightSpaceMatrix * posVec;
}

#type fragment
#version 330 core
#include shaders/libs/fog.glsl
#include shaders/libs/shadow.glsl

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
    vec3 color;
    vec3 attenuation;

    bool directional;
    vec3 dir;

    bool spotlight;
    float cutOff;
    float outerCutOff;
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
uniform Light lights[16];

float calculateAttenuation(vec3 attenuation, float distance) {
    return 1 / (attenuation.x + attenuation.y * distance + attenuation.z * (distance * distance));
}

vec3 calculateDiffuse(Light light, vec3 normal, float attenuation, vec3 lightDir) {
    //minimum of 0 otherwise it will override the ambient lgiht
    float diff = max(dot(normal, lightDir), 0);
    return attenuation * light.color * material.diffuse * diff;
}

vec3 calculateSpecular(Light light, vec3 normal, vec3 viewDir, float attenuation, vec3 lightDir) {
    //blinn-phong
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0), material.shininess);
    return attenuation * light.color * material.specular * spec;
}

float spotlightIntensity(Light light, vec3 lightDir) {
    float theta = dot(lightDir, normalize(-light.dir));
    float epsilon = (light.cutOff - light.outerCutOff);
    return clamp((theta - light.outerCutOff) / epsilon, 0, 1);
}

vec4 applyLighting(vec4 diffTex) {
    vec3 norm = normalize(normal);
    vec3 viewDir = normalize(camPos - pos);

    // ambient //

    vec3 ambient = ambient * material.ambient * diffTex.rgb;

    if (dot(viewDir, norm) < 0.0f)
        return vec4(ambient, 1);

    // shadow //

    float shadow = 1 - calculateShadow(norm, normalize(shadowDir));

    // diffuse and specular //

    vec3 diffuse = vec3(0);
    vec3 specular = vec3(0);

    for (int i = 0; i < min(lightCount, 16); i++) {
        Light l = lights[i];

        vec3 diffDist = l.pos - pos;
        float attenuation = l.directional ? 1 : calculateAttenuation(l.attenuation, length(diffDist));
        if (attenuation <= 0.01f)
            continue;

        vec3 lightDir = l.directional ? normalize(l.dir) : normalize(diffDist);
        vec3 diff = calculateDiffuse(l, norm, attenuation, lightDir);
        vec3 spec = calculateSpecular(l, norm, viewDir, attenuation, lightDir);

        //spotlight
        if (l.spotlight) {
            float intensity = spotlightIntensity(l, lightDir);
            diff *= intensity;
            spec *= intensity;
        }

        //directional
        if (l.directional) {
            diff *= shadow;
            spec *= shadow;
        }

        diffuse += diff;
        specular += spec;
    }

    diffuse *= diffTex.rgb;
    specular *= texture(material.specularTex, texCoords).rgb;

    // emissive //

    vec3 emissive = texture(material.emissiveTex, texCoords).rgb;

    //return light
    return vec4(max(ambient + diffuse + specular, emissive), diffTex.a);
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