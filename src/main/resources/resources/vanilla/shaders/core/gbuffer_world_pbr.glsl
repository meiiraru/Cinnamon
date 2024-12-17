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
    vec4 worldPos = model * vec4(aPosition, 1.0f);
    gl_Position = projection * view * worldPos;
    pos = worldPos.xyz;
    texCoords = aTexCoords;

    vec3 T = normalize(normalMat * aTangent);
    vec3 N = normalize(normalMat * aNormal);
    vec3 B = cross(T, N);
    TBN = mat3(T, B, N);
    pTBN = transpose(TBN);
}

#type fragment
#version 330 core

layout (location = 0) out vec4 gPosition;
layout (location = 1) out vec4 gAlbedo;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gNormal;
layout (location = 4) out vec4 gEmissive;

struct Material {
    sampler2D albedoTex;
    sampler2D heightTex;
    sampler2D normalTex;
    sampler2D roughnessTex;
    sampler2D metallicTex;
    sampler2D aoTex;
    sampler2D emissiveTex;
    float heightScale;
};

in vec2 texCoords;
in vec3 pos;
in mat3 TBN;
in mat3 pTBN;

uniform vec3 color;
uniform vec3 camPos;
uniform Material material;

const int minParallax = 16;
const int maxParallax = 64;

vec2 pallaxMapping(vec2 texCoords, vec3 viewDir, sampler2D depthMap, float heightScale) {
    //calculate number of layers
    float numLayers = mix(maxParallax, minParallax, max(dot(vec3(0.0f, 0.0f, 1.0f), viewDir), 0.0f));
    float layerDepth = 1.0f / numLayers;
    float currentLayerDepth = 0.0f;
    vec2 P = viewDir.xy / viewDir.z * heightScale;
    vec2 deltaTexCoords = P / numLayers;

    //initial values
    vec2 currentTexCoords = texCoords;
    float currentDepthMapValue = 1.0f - texture(depthMap, currentTexCoords).r;

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

void main() {
    //parallax mapping
    vec3 parallaxDir = normalize(pTBN * (camPos - pos));
    vec2 texCoords = pallaxMapping(texCoords, parallaxDir, material.heightTex, material.heightScale);

    //if (texCoords.x > 1.0f || texCoords.y > 1.0f || texCoords.x < 0.0f || texCoords.y < 0.0f)
    //    discard;

    //grab textures
    vec4 albedo = texture(material.albedoTex, texCoords);
    if (albedo.a <= 0.01f)
        discard;

    float metallic  = texture(material.metallicTex, texCoords).r;
    float roughness = texture(material.roughnessTex, texCoords).r;
    float ao        = texture(material.aoTex, texCoords).r;
    vec3 emissive   = texture(material.emissiveTex, texCoords).rgb;
    vec3 normal     = getNormalFromMap(material.normalTex, texCoords, TBN);

    //write to gBuffer
    gPosition = vec4(pos, 1.0f);
    gAlbedo = albedo * vec4(color, 1.0f);
    gORM = vec4(ao, roughness, metallic, 1.0f);
    gNormal = vec4(normal, 1.0f);
    gEmissive = vec4(emissive, 1.0f);
}