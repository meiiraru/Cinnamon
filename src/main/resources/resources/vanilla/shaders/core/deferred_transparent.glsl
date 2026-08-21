#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core
#include shaders/libs/fog.glsl

in vec2 texCoords;

out vec4 fragColor;

//gBuffer
uniform sampler2D gAlbedo;
uniform sampler2D gNormal;
uniform sampler2D gORM;
uniform sampler2D gEmissive;
uniform sampler2D gDepth;

uniform sampler2D lightTex;
uniform float lightFactor = 1.0f;

uniform vec3 camPos;
uniform mat4 invView;
uniform mat4 invProjection;

//IBL
const int MAX_REFLECTION_LOD = 8;
uniform mat3 cubemapRotation;
uniform samplerCube prefilterMap;
uniform sampler2D brdfLUT;

vec3 getPosFromDepth(vec2 texCoords) {
    //normalized device coordinates
    vec2 ndc = texCoords * 2.0f - 1.0f;

    //clip space
    float depth = texture(gDepth, texCoords).r;
    vec4 clip = vec4(ndc, depth * 2.0f - 1.0f, 1.0f);

    //view space
    vec4 view = invProjection * clip;
    view /= view.w;

    //world space
    vec4 world = invView * view;
    return world.xyz;
}

vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness) {
    return F0 + (max(vec3(1.0f - roughness), F0) - F0) * pow(clamp(1.0f - cosTheta, 0.0f, 1.0f), 5.0f);
}

void main() {
    //fetch albedo
    vec4 albedo = texture(gAlbedo, texCoords);
    if (albedo.a <= 0.0f)
        discard; //no refraction

    //fetch surface
    vec3 pos = getPosFromDepth(texCoords);
    vec3 gORM = texture(gORM, texCoords).rgb;
    float roughness = gORM.g;
    float metallic  = gORM.b;

    //normal mapping
    vec3 N = texture(gNormal, texCoords).rgb;
    vec3 V = normalize(camPos - pos);
    vec3 R = reflect(-V, N);

    //F0
    vec3 F0 = vec3(0.04f);
    F0 = mix(F0, albedo.rgb, metallic);

    //lighting
    vec3 Lo = texture(lightTex, texCoords).rgb * lightFactor;

    //IBL specular
    float NdotV = max(dot(N, V), 0.0f);
    vec3 F = fresnelSchlickRoughness(NdotV, F0, roughness);
    vec3 prefilteredColor = textureLod(prefilterMap, R * cubemapRotation, roughness * (MAX_REFLECTION_LOD - 1)).rgb;
    vec2 brdf = texture(brdfLUT, vec2(NdotV, roughness)).rg;
    vec3 specular = prefilteredColor * (F * brdf.x + brdf.y);

    //the refracted scene
    vec3 emissive = texture(gEmissive, texCoords).rgb;

    //transmission
    vec3 transmission = emissive * (vec3(1.0f) - F) * (1.0f - metallic);

    //combine
    vec3 finalColor = Lo + specular + transmission;

    //fog
    fragColor = calculateFog(pos, camPos, vec4(finalColor, albedo.a));
}