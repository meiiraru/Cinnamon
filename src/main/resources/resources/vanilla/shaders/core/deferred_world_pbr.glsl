#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core
#include shaders/libs/fog.glsl

struct Light {
    vec3 pos;
    vec3 color;
};

in vec2 texCoords;

out vec4 fragColor;

//gBuffer
uniform sampler2D gPosition;
uniform sampler2D gAlbedo;
uniform sampler2D gORM;
uniform sampler2D gNormal;
uniform sampler2D gEmissive;

uniform vec3 camPos;

const int MAX_LIGHTS = 128;
uniform int lightCount;
uniform Light lights[MAX_LIGHTS];

const float PI = 3.14159265359f;

//IBL
const int MAX_REFLECTION_LOD = 4;
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

vec4 applyLighting(vec3 pos) {
    //grab textures
    vec4 albedo4 = texture(gAlbedo, texCoords);
    vec3 albedo = albedo4.rgb;

    float ao        = texture(gORM, texCoords).r;
    float roughness = texture(gORM, texCoords).g;
    float metallic  = texture(gORM, texCoords).b;

    //normal mapping
    vec3 N = texture(gNormal, texCoords).rgb;
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

    vec4 emissive = texture(gEmissive, texCoords);

    //output
    return vec4(color, albedo4.a) + emissive;
}

void main() {
    //if (true) {fragColor = vec4(texture(gAlbedo, texCoords); return;}

    //position
    vec3 pos = texture(gPosition, texCoords).rgb;

    //lighting
    vec4 col = applyLighting(pos);

    //fog
    col = calculateFog(pos, camPos, col);

    //final color
    fragColor = col;
}