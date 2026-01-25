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
#include shaders/libs/parallax_mapping.glsl

layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gEmissive;

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

in vec2 texCoords;
in vec3 pos;
in mat3 TBN;

uniform vec4 color = vec4(1.0f);
uniform vec3 camPos;
uniform Material material;

void main() {
    //parallax mapping
    vec3 viewDir = normalize(transpose(TBN) * (camPos - pos));
    viewDir.y = -viewDir.y; //flip y for opengl coordinate system
    vec2 texCoords = parallaxMapping(texCoords, viewDir, material.heightTex, material.heightScale);

    //if (texCoords.x > 1.0f || texCoords.y > 1.0f || texCoords.x < 0.0f || texCoords.y < 0.0f)
    //    discard;

    //sample textures
    vec4 albedo = texture(material.albedoTex, texCoords);
    if (albedo.a < 0.01f)
        discard;

    float ao        = texture(material.aoTex, texCoords).r;
    float roughness = texture(material.roughnessTex, texCoords).r;
    float metallic  = texture(material.metallicTex, texCoords).r;
    vec3 emissive   = texture(material.emissiveTex, texCoords).rgb;

    //sample normal
    vec3 normal = texture(material.normalTex, texCoords).rgb;
    normal = normal * 2.0f - 1.0f;
    normal = normalize(TBN * normal);

    //write to gBuffer
    gAlbedo = albedo * color;
    gNormal = vec4(normal, 1.0f);
    gORM = vec4(ao, roughness, metallic, 1.0f);
    gEmissive = vec4(emissive, 1.0f);
}