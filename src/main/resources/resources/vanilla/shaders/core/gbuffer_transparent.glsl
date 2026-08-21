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

layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gEmissive;

struct Material {
    sampler2D albedoTex;
    sampler2D normalTex;
    sampler2D roughnessTex;
    sampler2D metallicTex;
    sampler2D emissiveTex;
};

in vec2 texCoords;
in vec3 pos;
in mat3 TBN;

uniform vec4 color = vec4(1.0f);
uniform vec3 camPos;
uniform Material material;

uniform mat4 projection;
uniform mat4 view;
uniform sampler2D opaqueSceneTex;
uniform float strength = 1.0f;

uniform vec3 ior = vec3(0.0f);

vec3 sampleIOR(vec3 viewDir, vec3 normal, float ior) {
    //refraction
    vec3 refraction = refract(viewDir, normal, 1.0f / ior);
    refraction = normalize(refraction);

    //position
    vec3 position = pos + refraction * strength;

    //convert position to screen space
    vec4 clipSpacePos = projection * view * vec4(position, 1.0f);
    vec2 screenUV = (clipSpacePos.xy / clipSpacePos.w) * 0.5f + 0.5f;

    //sample the opaque scene
    return texture(opaqueSceneTex, screenUV).rgb;
}

void main() {
    //the view direction
    vec3 viewDir = normalize(pos - camPos);

    //calculate world normal
    vec3 normalMap = texture(material.normalTex, texCoords).rgb;
    normalMap = normalMap * 2.0f - 1.0f;
    vec3 normal = normalize(TBN * normalMap);

    //fetch material properties
    vec4 baseColor  = texture(material.albedoTex, texCoords) * color;
    float roughness = texture(material.roughnessTex, texCoords).r;
    float metallic  = texture(material.metallicTex, texCoords).r;

    //IOR mapping [0..1] -> [1.0..5.0] air = 1.0, diamond = 2.5
    vec3 rawIor = texture(material.emissiveTex, texCoords).rgb;
    vec3 finalIor = 1.0f + (rawIor * 4.0f) + ior;

    vec3 backgroundLight = vec3(
            sampleIOR(viewDir, normal, finalIor.r).r,
            sampleIOR(viewDir, normal, finalIor.g).g,
            sampleIOR(viewDir, normal, finalIor.b).b
    );
    vec3 finalTint = backgroundLight * baseColor.rgb;

    //gBuffer output

    //albedo is used as a mask for the transparent object
    gAlbedo = vec4(0.0f, 0.0f, 0.0f, baseColor.a);
    gNormal = vec4(normal, 1.0f);
    gORM = vec4(1.0f, roughness, metallic, 1.0f);
    //emissive is used to store the final refraction color
    gEmissive = vec4(finalTint, 1.0f);
}