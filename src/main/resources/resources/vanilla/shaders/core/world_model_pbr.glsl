#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec3 aTangent;

out vec2 texCoords;
out vec3 pos;
out mat3 TBN;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;

void main() {
    vec4 worldPos = model * vec4(aPosition, 1.0f);
    gl_Position = projection * view * worldPos;
    pos = worldPos.xyz;
    texCoords = aTexCoords;

    vec3 T = normalize(normalMat * aTangent);
    vec3 N = normalize(normalMat * aNormal);
    T = normalize(T - dot(T, N) * N);
    TBN = mat3(T, cross(N, T), N);
}

#type fragment
#version 330 core
#include shaders/libs/fog.glsl
#include shaders/libs/parallax_mapping.glsl

struct Material {
    sampler2D albedoTex;
    sampler2D heightTex;
    sampler2D normalTex;
    sampler2D aoTex;
    sampler2D roughnessTex;
    sampler2D metallicTex;
    sampler2D emissiveTex;
    float heightScale;
};

struct Light {
    vec3 pos;
    vec3 color;
};

in vec2 texCoords;
in vec3 pos;
in mat3 TBN;

out vec4 fragColor;

uniform vec4 color = vec4(1.0f);
uniform vec3 camPos;
uniform Material material;

const int MAX_LIGHTS = 128;
uniform int lightCount;
uniform Light lights[MAX_LIGHTS];

const float PI = 3.14159265359f;

//IBL
const int MAX_REFLECTION_LOD = 7;
uniform mat3 cubemapRotation;
uniform samplerCube irradianceMap;
uniform samplerCube prefilterMap;
uniform sampler2D brdfLUT;

//Trowbridge-Reitz GGX
//D (NDF Normal Distribution Function)
float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;

    float NdotH = max(dot(N, H), 0.0f);
    float NdotH2 = NdotH * NdotH;
    float denom = (NdotH2 * (a2 - 1.0f) + 1.0f);

    return a2 / (PI * (denom * denom));
}

float geometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0f);
    float k = (r * r) / 8.0f;
    return NdotV / (NdotV * (1.0f - k) + k);
}

//G (Geometry function)
float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0f);
    float NdotL = max(dot(N, L), 0.0f);
    float ggx2 = geometrySchlickGGX(NdotV, roughness);
    float ggx1 = geometrySchlickGGX(NdotL, roughness);

    return ggx1 * ggx2;
}

//F (Fresnel equation)
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0f - F0) * pow(clamp(1.0f - cosTheta, 0.0f, 1.0f), 5.0f);
}

vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness) {
    return F0 + (max(vec3(1.0f - roughness), F0) - F0) * pow(clamp(1.0f - cosTheta, 0.0f, 1.0f), 5.0f);
}

vec4 applyLighting() {
    //parallax mapping
    vec3 viewDir = normalize(transpose(TBN) * (camPos - pos));
    viewDir.y = -viewDir.y; //flip y for opengl coordinate system
    vec2 texCoords = parallaxMapping(texCoords, viewDir, material.heightTex, material.heightScale);

    //if (texCoords.x > 1.0f || texCoords.y > 1.0f || texCoords.x < 0.0f || texCoords.y < 0.0f)
    //    discard;

    //grab textures
    vec4 albedo4 = texture(material.albedoTex, texCoords);
    if (albedo4.a <= 0.01f)
        discard;

    vec3 albedo     = albedo4.rgb;
    float ao        = texture(material.aoTex, texCoords).r;
    float roughness = texture(material.roughnessTex, texCoords).r;
    float metallic  = texture(material.metallicTex, texCoords).r;

    //sample normal
    vec3 N = texture(material.normalTex, texCoords).rgb;
    N = N * 2.0f - 1.0f;
    N = normalize(TBN * N);

    vec3 V = normalize(camPos - pos);
    vec3 R = reflect(-V, N);

    //F0
    vec3 F0 = vec3(0.04f);
    F0 = mix(F0, albedo, metallic);

    //reflectance
    vec3 Lo = vec3(0.0f);
    for (int i = 0; i < min(lightCount, MAX_LIGHTS); i++) {
        Light light = lights[i];

        //light radiance
        //L = light direction; H = half vector
        vec3 L = normalize(light.pos - pos);
        vec3 H = normalize(V + L);

        float distance = length(light.pos - pos);
        float attenuation = 1.0f / (distance * distance);
        vec3 radiance = light.color * attenuation;

        //cook torrance BRDF
        float D = distributionGGX(N, H, roughness);
        float G = geometrySmith(N, V, L, roughness);
        vec3 F = fresnelSchlick(max(dot(H, V), 0.0f), F0);

        //calculate specular and diffuse
        vec3 kS = F;
        vec3 kD = (vec3(1.0f) - kS) * (1.0f - metallic);

        float NdotL = max(dot(N, L), 0.0f);
        vec3 specular = (D * F * G) / (4.0f * max(dot(N, V), 0.0f) * NdotL + 0.0001f);

        //calculate radiance and add to Lo
        Lo += (kD * albedo / PI + specular) * radiance * NdotL;
    }

    //ambient lighting
    float NdotV = max(dot(N, V), 0.0f);
    vec3 F = fresnelSchlickRoughness(NdotV, F0, roughness);

    vec3 kS = F;
    vec3 kD = (1.0f - kS) * (1.0f - metallic);
    vec3 irradiance = texture(irradianceMap, N * cubemapRotation).rgb;
    vec3 diffuse = irradiance * albedo;

    //sample both the pre-filter map and the BRDF lut and combine them together as per the Split-Sum approximation to get the IBL specular part
    vec3 prefilteredColor = textureLod(prefilterMap, R * cubemapRotation, roughness * (MAX_REFLECTION_LOD - 1)).rgb;
    vec2 brdf = texture(brdfLUT, vec2(NdotV, roughness)).rg;
    vec3 specular = prefilteredColor * (F * brdf.x + brdf.y);

    vec3 color = (kD * diffuse + specular) * ao + Lo;

    vec3 emissive = texture(material.emissiveTex, texCoords).rgb;

    //output
    return vec4(color + emissive, albedo4.a);
}

void main() {
    //if (true) {fragColor = vec4(getNormalFromMap(material.normalTex, texCoords, TBN), 1); return;}

    //color
    vec4 col = color;

    //lighting
    col *= applyLighting();

    //fog
    col = calculateFog(pos, camPos, col);

    //final color
    col.a = clamp(col.a, 0.0f, 1.0f);
    fragColor = col;
}
