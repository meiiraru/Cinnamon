#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec3 aTangent;
layout (location = 4) in float aTexLayer;

out vec2 texCoords;
out vec3 pos;
out mat3 TBN;
out float texLayer;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;

void main() {
    vec4 worldPos = model * vec4(aPosition, 1.0f);
    gl_Position = projection * view * worldPos;
    pos = worldPos.xyz;
    texCoords = aTexCoords;
    texLayer = aTexLayer;

    // Build TBN matrix for normal mapping
    vec3 T = normalize(normalMat * aTangent);
    vec3 N = normalize(normalMat * aNormal);
    T = normalize(T - dot(T, N) * N); // re-orthogonalize
    vec3 B = cross(N, T);
    TBN = mat3(T, B, N);
}

#type fragment
#version 330 core

layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gEmissive;

in vec2 texCoords;
in vec3 pos;
in mat3 TBN;
in float texLayer;

uniform sampler2DArray blockTextures;
uniform sampler2DArray normalTextures;
uniform sampler2DArray roughnessTextures;
uniform sampler2DArray aoTextures;

void main() {
    // Sample albedo from the texture array
    vec4 albedo = texture(blockTextures, vec3(texCoords, texLayer));

    // Discard fully transparent pixels
    if (albedo.a < 0.01f)
        discard;

    // Sample PBR textures
    float ao        = texture(aoTextures, vec3(texCoords, texLayer)).r;
    float roughness = texture(roughnessTextures, vec3(texCoords, texLayer)).r;

    // Sample and apply tangent-space normal map
    vec3 normalMap = texture(normalTextures, vec3(texCoords, texLayer)).rgb;
    normalMap = normalMap * 2.0f - 1.0f;
    vec3 normal = normalize(TBN * normalMap);

    // Write to G-Buffer
    gAlbedo = albedo;
    gNormal = vec4(normal, 1.0f);
    gORM = vec4(ao, roughness, 0.0f, 1.0f); // metallic = 0 for dielectric blocks
    gEmissive = vec4(0.0f, 0.0f, 0.0f, 1.0f);
}
