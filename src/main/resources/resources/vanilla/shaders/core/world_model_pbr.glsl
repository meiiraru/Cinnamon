#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec3 aTangent;

out vec2 texCoords;
out vec3 pos;
out mat3 TBN;
out mat3 pTBN;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;

void main() {
    vec4 posVec = model * vec4(aPosition, 1.0f);
    gl_Position = projection * view * posVec;
    pos = posVec.xyz;
    texCoords = aTexCoords;

    vec3 T = normalize(normalMat * aTangent);
    vec3 N = normalize(normalMat * aNormal);
    vec3 B = cross(T, N);
    TBN = mat3(T, B, N);
    pTBN = transpose(TBN);
}

#type fragment
#version 330 core
#include shaders/libs/fog.glsl

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
in mat3 pTBN;

out vec4 fragColor;

uniform vec3 color;
uniform vec3 camPos;

uniform Material material;

const int MAX_LIGHTS = 128;
uniform int lightCount;
uniform Light lights[MAX_LIGHTS];

const float PI = 3.14159265359f;

const int minParallax = 16;
const int maxParallax = 64;

//IBL
const int MAX_REFLECTION_LOD = 4;
uniform mat3 cubemapRotation;
uniform samplerCube irradianceMap;
uniform samplerCube prefilterMap;
uniform sampler2D brdfLUT;

vec2 pallaxMapping(vec2 texCoords, vec3 viewDir, sampler2D depthMap, float heightScale) {
    float currentDepthMapValue = 1.0f - texture(depthMap, texCoords).r;
    if (currentDepthMapValue <= 0.01f)
        return texCoords;

    //calculate number of layers
    float numLayers = mix(maxParallax, minParallax, max(dot(vec3(0.0f, 0.0f, 1.0f), viewDir), 0.0f));
    float layerDepth = 1.0f / numLayers;
    float currentLayerDepth = 0.0f;
    vec2 textureSize = vec2(textureSize(depthMap, 0));
    vec2 P = viewDir.xy / viewDir.z * heightScale * normalize(textureSize).yx;
    vec2 deltaTexCoords = P / numLayers;

    vec2 currentTexCoords = texCoords;
    while(currentLayerDepth < currentDepthMapValue) {
        //shift texture coordinates along direction of view
        currentTexCoords -= deltaTexCoords;
        currentDepthMapValue = 1.0f - texture(depthMap, currentTexCoords).r;
        currentLayerDepth += layerDepth;
    }

    //get texture coordinates before collision
    vec2 prevTexCoords = currentTexCoords + deltaTexCoords;

    //get depth between collision
    float afterDepth  = currentDepthMapValue - currentLayerDepth;
    float beforeDepth = 1.0f - texture(depthMap, prevTexCoords).r - currentLayerDepth + layerDepth;

    //interpolate final texture coordinates
    float weight = afterDepth / (afterDepth - beforeDepth);
    vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0f - weight);

    return finalTexCoords;
}

//normal mapping function from tangent space to world space
vec3 getNormalFromMap(sampler2D normalTex, vec2 texCoords, mat3 TBN) {
    vec3 normal = texture(normalTex, texCoords).rgb;
    normal = normal * 2.0f - 1.0f;
    normal = normalize(TBN * normal);
    return normal;
}

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
    vec3 parallaxDir = normalize(pTBN * (camPos - pos));
    vec2 texCoords = pallaxMapping(texCoords, parallaxDir, material.heightTex, material.heightScale);

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

    //normal mapping
    vec3 N = getNormalFromMap(material.normalTex, texCoords, TBN);
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
    vec3 prefilteredColor = textureLod(prefilterMap, R * cubemapRotation, roughness * MAX_REFLECTION_LOD).rgb;
    vec2 brdf = texture(brdfLUT, vec2(NdotV, roughness)).rg;
    vec3 specular = prefilteredColor * (F * brdf.x + brdf.y);

    vec3 color = (kD * diffuse + specular) * ao + Lo;

    vec4 emissive = texture(material.emissiveTex, texCoords);

    //output
    return vec4(color, albedo4.a) + emissive;
}

void main() {
    //if (true) {fragColor = vec4(getNormalFromMap(material.normalTex, texCoords, TBN), 1); return;}

    //color
    vec4 col = vec4(color, 1.0f);

    //lighting
    col *= applyLighting();

    //fog
    col = calculateFog(pos, camPos, col);

    //final color
    fragColor = col;
}