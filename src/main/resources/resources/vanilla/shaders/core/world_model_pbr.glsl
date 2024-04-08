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
    sampler2D albedoTex;
    sampler2D normalTex;
    sampler2D roughnessTex;
    sampler2D metallicTex;
    sampler2D aoTex;
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

const float PI = 3.14159265359f;

//simple normal mapping function, courtesy of learnopengl
vec3 getNormalFromMap() {
    vec3 tangentNormal = texture(material.normalTex, texCoords).rgb * 2 - 1;

    vec3 Q1 = dFdx(pos);
    vec3 Q2 = dFdy(pos);
    vec2 st1 = dFdx(texCoords);
    vec2 st2 = dFdy(texCoords);

    vec3 N = normalize(normal);
    vec3 T = normalize(Q1 * st2.t - Q2 * st1.t);
    vec3 B = -normalize(cross(N, T));
    mat3 TBN = mat3(T, B, N);

    return normalize(TBN * tangentNormal);
}

//Trowbridge-Reitz GGX
//NDF (Normal Distribution Function)
float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;

    float NdotH = max(dot(N, H), 0);
    float NdotH2 = NdotH * NdotH;
    float denom = (NdotH2 * (a2 - 1) + 1);

    return a2 / (PI * (denom * denom));
}

//geometry function
float geometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1);
    float k = (r * r) / 8;
    return NdotV / (NdotV * (1 - k) + k);
}

//G
float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0);
    float NdotL = max(dot(N, L), 0);
    float ggx2 = geometrySchlickGGX(NdotV, roughness);
    float ggx1 = geometrySchlickGGX(NdotL, roughness);

    return ggx1 * ggx2;
}

//F
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1 - F0) * pow(clamp(1 - cosTheta, 0, 1), 5);
}

vec4 applyLighting(vec4 albedoTex) {
    //grab textures
    vec3 albedo     = albedoTex.rgb;
    float metallic  = texture(material.metallicTex, texCoords).r;
    float roughness = texture(material.roughnessTex, texCoords).r;
    float ao        = texture(material.aoTex, texCoords).r;

    //get N (normal) and V (view direction)
    vec3 N = getNormalFromMap();
    vec3 V = normalize(camPos - pos);

    //grab shadow from shadow map
    float shadow = 1 - calculateShadow(N, normalize(shadowDir));

    //F0
    vec3 F0 = vec3(0.04f);
    F0 = mix(F0, albedo, metallic);

    //reflectance
    vec3 Lo = vec3(0);
    for (int i = 0; i < min(lightCount, 16); i++) {
        Light light = lights[i];

        //light radiance
        //L = light direction; H = half vector
        vec3 L = normalize(light.directional ? light.dir : (light.pos - pos));
        vec3 H = normalize(V + L);

        vec3 radiance;

        if (light.directional) {
            radiance = vec3(1);
        } else {
            float distance = length(light.pos - pos);
            float attenuation = 1 / (distance * distance);
            radiance = light.color * attenuation;
        }

        //cook torrance BRDF
        float NDF = distributionGGX(N, H, roughness);
        float G = geometrySmith(N, V, L, roughness);
        vec3 F = fresnelSchlick(max(dot(H, V), 0), F0);

        //calculate specular and diffuse
        vec3 kS = F;
        vec3 kD = vec3(1) - kS;
        kD *= 1 - metallic;

        float NdotL = max(dot(N, L), 0);
        vec3 specular = (NDF * G * F) / (4 * max(dot(N, V), 0) * NdotL + 0.0001f);

        //calculate radiance and add to Lo
        Lo += (kD * albedo / PI + specular) * radiance * NdotL * shadow;
    }

    //finish calculating light
    vec3 color = (ambient * albedo * ao) + Lo;
    vec4 emissive = texture(material.emissiveTex, texCoords);

    //output
    return vec4(color, albedoTex.a) + emissive;
}

void main() {
    //if (true) {fragColor = vec4(normal, 1); return;}

    //texture
    vec4 tex = texture(material.albedoTex, texCoords);
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