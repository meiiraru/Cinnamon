#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec3 aTangent;

out vec2 texCoords;
out vec3 pos;
out vec4 shadowPos;
out mat3 TBN;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;
uniform mat4 lightSpaceMatrix;

void main() {
    vec4 posVec = model * vec4(aPosition, 1.0f);
    gl_Position = projection * view * posVec;
    pos = posVec.xyz;
    texCoords = aTexCoords;
    shadowPos = lightSpaceMatrix * posVec;

    vec3 T = normalize(vec3(mat3(model) * (aTangent * normalMat)));
    vec3 N = normalize(vec3(mat3(model) * (aNormal * normalMat)));
    vec3 B = cross(N, T);
    TBN = mat3(T, B, N);
}

#type fragment
#version 330 core
#include shaders/libs/fog.glsl
#include shaders/libs/shadow.glsl

struct Material {
    sampler2D albedoTex;
    sampler2D heightTex;
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
in mat3 TBN;

out vec4 fragColor;

uniform vec3 color;
uniform vec3 camPos;

uniform Material material;

uniform vec3 ambient;
uniform int lightCount;
uniform Light lights[16];

const float PI = 3.14159265359f;

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

vec4 applyLighting() {
    //grab textures
    vec4 albedo4 = texture(material.albedoTex, texCoords);
    if (albedo4.a <= 0.01f)
        discard;

    vec3 albedo     = albedo4.rgb;
    float metallic  = texture(material.metallicTex, texCoords).r;
    float roughness = texture(material.roughnessTex, texCoords).r;
    float ao        = texture(material.aoTex, texCoords).r;

    //normal mapping
    vec3 N = getNormalFromMap(material.normalTex, texCoords, TBN);
    vec3 V = normalize(camPos - pos);

    //grab shadow from shadow map
    float shadow = 1.0f - calculateShadow(N, normalize(shadowDir));

    //F0
    vec3 F0 = vec3(0.04f);
    F0 = mix(F0, albedo, metallic);

    //reflectance
    vec3 Lo = vec3(0.0f);
    for (int i = 0; i < min(lightCount, 16); i++) {
        Light light = lights[i];

        //light radiance
        //L = light direction; H = half vector
        vec3 L = normalize(light.directional ? light.dir : (light.pos - pos));
        vec3 H = normalize(V + L);

        vec3 radiance;

        if (light.directional) {
            radiance = vec3(1.0f);
        } else {
            float distance = length(light.pos - pos);
            float attenuation = 1.0f / (distance * distance);
            radiance = light.color * attenuation;
        }

        //cook torrance BRDF
        float D = distributionGGX(N, H, roughness);
        float G = geometrySmith(N, V, L, roughness);
        vec3 F = fresnelSchlick(max(dot(H, V), 0.0f), F0);

        //calculate specular and diffuse
        vec3 kS = F;
        vec3 kD = vec3(1.0f) - kS;
        kD *= 1 - metallic;

        float NdotL = max(dot(N, L), 0.0f);
        vec3 specular = (D * F * G) / (4.0f * max(dot(N, V), 0.0f) * NdotL + 0.0001f);

        //calculate radiance and add to Lo
        Lo += (kD * albedo / PI + specular) * radiance * NdotL * shadow;
    }

    //finish calculating light
    vec3 color = (ambient * albedo * ao) + Lo;
    vec4 emissive = texture(material.emissiveTex, texCoords);

    //output
    return vec4(color, albedo4.a) + emissive;
}

void main() {
    //if (true) {fragColor = vec4((getNormalFromMap(material.normalTex, texCoords, TBN) + 2) / 4, 1); return;}

    //color
    vec4 col = vec4(color, 1.0f);

    //lighting
    col *= applyLighting();

    //fog
    col = calculateFog(pos, camPos, col);

    //final color
    fragColor = col;
}