#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

struct Light {
    vec3 pos;
    vec3 color;

    float intensity;
    float falloffStart;
    float falloffEnd;

    //1 = point, 2 = spot, 3 = directional
    int type;

    //spotlights
    vec3 direction;
    float innerCutOff;
    float outerCutOff;

    //shadow
    mat4 lightSpaceMatrix;
    bool castsShadows;
};

in vec2 texCoords;

out vec4 fragColor;

const float PI = 3.14159265359f;

//gBuffer inputs
uniform sampler2D gPosition;
uniform sampler2D gAlbedo;
uniform sampler2D gORM;
uniform sampler2D gNormal;

//light and camera inputs
uniform vec3 camPos;
uniform Light light;
uniform sampler2D shadowMap;
uniform samplerCube shadowCubeMap;

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

float calculateDirectionalShadow(mat4 lightMatrix, vec3 lightDir, vec3 fragPosWorld, vec3 normal) {
    //transform fragment position to light clip space
    vec4 fragPosLightSpace = lightMatrix * vec4(fragPosWorld, 1.0f);

    //normalize to [0,1] range
    vec3 lightCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    lightCoords = lightCoords * 0.5f + 0.5f;

    //get the current fragment depth from the light perspective
    float currentDepth = lightCoords.z;

    //early exit if outside the light frustum
    if (currentDepth > 1.0f)
        return 1.0f;

    //get the closest depth from the light perspective
    float closestDepth = texture(shadowMap, lightCoords.xy).r;

    //shadow acne prevention bias
    //the bias is larger for surfaces that are steeply angled to the light
    float bias = max(0.01f * (1.0f - dot(normal, lightDir)), 0.001f);

    //check if the current fragment is behind the closest one recorded in the shadow map
    float shadow = currentDepth - bias > closestDepth ? 0.0f : 1.0f;
    return shadow;
}

float calculatePointShadow(vec3 fragPosWorld) {
    //direction from light to fragment
    vec3 lightDir = fragPosWorld - light.pos;

    //current distance from light to fragment
    float currentDepth = length(lightDir);

    //sample closest depth from cubemap
    float closestDepth = texture(shadowCubeMap, lightDir).r;
    closestDepth *= light.falloffEnd; // stored depth was normalized

    //bias to prevent shadow acne
    const float bias = 0.05f;

    //check if the current fragment is in shadow
    float shadow = currentDepth - bias > closestDepth ? 0.0f : 1.0f;
    return shadow;
}

void main() {
    //pos
    vec3 pos = texture(gPosition, texCoords).rgb;

    //discard fragments outside the light volume
    //TODO change to a mesh-based light volume
    if (light.type == 1 || light.type == 2) {
        float distanceToLight = length(light.pos - pos);
        if (distanceToLight > light.falloffEnd)
            discard;
    }

    //color
    vec3 albedo = texture(gAlbedo, texCoords).rgb;

    //normal map
    vec3 N = texture(gNormal, texCoords).rgb;
    vec3 V = normalize(camPos - pos);

    //light radiance
    //L = light direction
    vec3 L;
    float attenuation = 1.0f;

    if (light.type == 3) {
        L = -light.direction;
    } else {
        L = light.pos - pos;
        float distance = length(L);
        L = normalize(L);

        //calculate distance-based attenuation
        float distanceAttenuation = smoothstep(light.falloffEnd, light.falloffStart, distance);

        //spotlight
        float spotEffect = 1.0f;
        if (light.type == 2) {
            //dot product between light-to-fragment vector and the light's forward direction
            //L points TOWARDS the light, so we use its inverse, light.direction points AWAY
            float theta = dot(-L, light.direction);

            //use smoothstep again for a soft cone edge
            spotEffect = smoothstep(light.outerCutOff, light.innerCutOff, theta);
        }

        attenuation = distanceAttenuation * spotEffect;
    }

    //final radiance
    vec3 radiance = light.color * light.intensity * attenuation;

    //shadow
    if (light.castsShadows) {
        float shadow = light.type == 1 ? calculatePointShadow(pos) : calculateDirectionalShadow(light.lightSpaceMatrix, L, pos, N);
        radiance *= shadow;
    }

    //if light has no effect, skip the expensive PBR calculations
    if (dot(radiance, radiance) < 0.00001f)
        discard;

    //reflectance
    vec3 Lo = vec3(0.0f);

    //H = half vector
    vec3 H = normalize(V + L);

    //ao, roughness, metallic
    vec4 gORM = texture(gORM, texCoords);
    float roughness = gORM.g;
    float metallic  = gORM.b;

    //F0
    vec3 F0 = vec3(0.04f);
    F0 = mix(F0, albedo, metallic);

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

    fragColor = vec4(Lo, 1.0f);
}
