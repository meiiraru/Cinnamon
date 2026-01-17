#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main() {
    gl_Position = projection * view * model * vec4(aPosition, 1.0f);
}

#type fragment
#version 330 core

struct Light {
    vec3 pos, color, direction;
    float intensity, falloffStart, falloffEnd, innerAngle, outerAngle;
    int type; //1 = point, 2 = spot, 3 = directional, 4 = cookie
    mat4 lightSpaceMatrix;
    bool castsShadows;
};

out vec4 fragColor;

const float PI = 3.14159265359f;

//gBuffer inputs
uniform sampler2D gAlbedo;
uniform sampler2D gORM;
uniform sampler2D gNormal;
uniform sampler2D gDepth;

//camera inputs
uniform vec3 camPos;
uniform mat4 view;
uniform mat4 invView;
uniform mat4 invProjection;

//light
uniform Light light;
uniform sampler2D shadowMap;
uniform samplerCube shadowCubeMap;
uniform sampler2D cookieMap;

//cascaded shadow maps
uniform int cascadeCount;
uniform float cascadeDistances[16];
uniform mat4 cascadeMatrices[16];
uniform sampler2DArray shadowCascadeMap;

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

float geometrySchlickGGX(float angle, float roughness) {
    float r = (roughness + 1.0f);
    float k = (r * r) / 8.0f;
    return angle / (angle * (1.0f - k) + k);
}

//G (Geometry function)
float geometrySmith(float NdotV, float NdotL, float roughness) {
    float ggx2 = geometrySchlickGGX(NdotV, roughness);
    float ggx1 = geometrySchlickGGX(NdotL, roughness);
    return ggx1 * ggx2;
}

//F (Fresnel equation)
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0f - F0) * pow(clamp(1.0f - cosTheta, 0.0f, 1.0f), 5.0f);
}

float calculateSpotShadow(vec3 lightCoords, vec3 lightDir, vec3 normal) {
    //get the current fragment depth from the light perspective
    float currentDepth = lightCoords.z;

    //get the closest depth from the shadow map
    float closestDepth = texture(shadowMap, lightCoords.xy).r;

    //shadow acne prevention bias
    //the bias is larger for surfaces that are steeply angled to the light
    float bias = max(0.001f * (1.0f - dot(normal, lightDir)), 0.0001f);

    //check if the current fragment is behind the closest one recorded in the shadow map
    float shadow = currentDepth - bias > closestDepth ? 1.0f : 0.0f;
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
    float shadow = currentDepth - bias > closestDepth ? 1.0f : 0.0f;
    return shadow;
}

vec3 getCookieColor(vec3 lightCoords) {
    //transform to UV coordinates
    vec2 cookieCoords = lightCoords.xy;
    cookieCoords.y = 1.0f - cookieCoords.y;

    //sample the cookie texture if inside the projection area
    if (cookieCoords.x >= 0.0f && cookieCoords.x <= 1.0f && cookieCoords.y >= 0.0f && cookieCoords.y <= 1.0f)
        return texture(cookieMap, cookieCoords).rgb;
    else
        return vec3(0.0f);
}

float calculateDirectionalShadow(vec3 fragPosWorld, vec3 lightDir, vec3 normal) {
    //select cascade layer
    vec4 fragPosViewSpace = view * vec4(fragPosWorld, 1.0f);
    float depthValue = abs(fragPosViewSpace.z);

    int layer = cascadeCount - 1;
    for (int i = 0; i < cascadeCount; i++) {
        if (depthValue < cascadeDistances[i]) {
            layer = i;
            break;
        }
    }

    //transform fragment position to light clip space
    vec4 fragPosLightSpace = cascadeMatrices[layer] * vec4(fragPosWorld, 1.0f);
    //perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    //transform to [0,1] range
    projCoords = projCoords * 0.5f + 0.5f;

    //get current light depth
    float currentDepth = projCoords.z;

    //dont cast shadow outside the light frustum
    if (currentDepth > 1.0f)
        return 0.0f;

    //acne prevention bias
    float bias = max(0.001f * (1.0f - dot(normal, lightDir)), 0.0001f);
    //bias *= 1 / (cascadeDistances[layer] * 0.5f);

    //get depth of closest fragment from light perspective
    float closestDepth = texture(shadowCascadeMap, vec3(projCoords.xy, layer)).r;
    float shadow = currentDepth - bias > closestDepth ? 1.0f : 0.0f;

    //PCF
    //float shadow = 0.0f;
    //vec2 texelSize = 1.0f / vec2(textureSize(shadowCascadeMap, 0));
    //const int r = 1;
    //for(int x = -r; x <= r; x++) {
    //    for(int y = -r; y <= r; ++y) {
    //        float pcfDepth = texture(shadowCascadeMap, vec3(projCoords.xy + vec2(x, y) * texelSize, layer)).r;
    //        shadow += currentDepth - bias > pcfDepth ? 1.0f : 0.0f;
    //    }
    //}
    //shadow /= (float((r * 2 + 1) * (r * 2 + 1)));

    return shadow;
}

void main() {
    //if (true) { fragColor = vec4(light.color, 1.0f); return; }

    //pos
    vec2 texCoords = gl_FragCoord.xy / vec2(textureSize(gAlbedo, 0));
    vec3 pos = getPosFromDepth(texCoords);
    vec3 lightCoords = vec3(0.0f);

    if (light.type != 1 && light.type != 3) {
        //transform fragment position to light clip space
        vec4 fragPosLightSpace = light.lightSpaceMatrix * vec4(pos, 1.0f);

        //normalize to [0,1] range
        lightCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
        lightCoords = lightCoords * 0.5f + 0.5f;

        //early exit if outside the light frustum
        if (lightCoords.z > 1.0f)
            discard;
    }

    //light radiance
    //L = light direction
    vec3 L;
    float attenuation = 1.0f;

    if (light.type == 3) {
        L = -light.direction;
    } else {
        L = light.pos - pos;
        float distance = length(L);
        L /= distance;

        //calculate distance-based attenuation
        float distanceAttenuation = smoothstep(light.falloffEnd, light.falloffStart, distance);

        //spotlight
        float spotEffect = 1.0f;
        if (light.type == 2 || light.type == 4) {
            //dot product between light-to-fragment vector and the light's forward direction
            //L points TOWARDS the light, so we use its inverse, light.direction points AWAY
            float theta = dot(-L, light.direction);

            //use smoothstep again for a soft cone edge
            spotEffect = smoothstep(light.outerAngle, light.innerAngle, theta);
        }

        attenuation = distanceAttenuation * spotEffect;
        if (attenuation < 0.001f)
            discard;
    }

    //final radiance
    vec3 radiance = light.color * light.intensity * attenuation;

    //cookie
    if (light.type == 4) {
        vec3 cookieColor = getCookieColor(lightCoords);
        if (cookieColor.r + cookieColor.g + cookieColor.b < 0.1f)
            discard;
        radiance *= cookieColor;
    }

    //normal
    vec3 N = texture(gNormal, texCoords).rgb;
    float NdotL = max(dot(N, L), 0.0f);
    if (NdotL <= 0.0f)
        discard;

    //shadow
    if (light.castsShadows) {
        float shadow = 0.0f;

        if (light.type == 1)
            shadow = calculatePointShadow(pos);
        else if (light.type == 3)
            shadow = calculateDirectionalShadow(pos, L, N);
        else
            shadow = calculateSpotShadow(lightCoords, L, N);

        radiance *= 1.0f - shadow;
    }

    //if light has no effect, skip the expensive PBR calculations
    if (dot(radiance, radiance) < 0.001f)
        discard;

    //color
    vec3 albedo = texture(gAlbedo, texCoords).rgb;

    //BRDF evaluation
    vec3 V = normalize(camPos - pos);
    vec3 H = normalize(V + L);

    //ao, roughness, metallic
    vec4 gORM = texture(gORM, texCoords);
    float roughness = gORM.g;
    float metallic  = gORM.b;

    vec3 F0 = mix(vec3(0.04f), albedo, metallic);

    //cook torrance BRDF
    float NdotV = max(dot(N, V), 0.0f);

    float D = distributionGGX(N, H, roughness);
    float G = geometrySmith(NdotV, NdotL, roughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0f), F0);

    //calculate specular and diffuse
    vec3 kS = F;
    vec3 kD = (vec3(1.0f) - kS) * (1.0f - metallic);
    vec3 specular = (D * F * G) / (4.0f * NdotV * NdotL + 0.0001f);

    //radiance
    vec3 Lo = (kD * albedo / PI + specular) * radiance * NdotL;
    fragColor = vec4(Lo, 1.0f);
}
