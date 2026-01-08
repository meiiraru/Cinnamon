#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

//textures
uniform sampler2D previousTex;
uniform sampler2D gNormal;
uniform sampler2D gORM;
uniform sampler2D gDepth;

//camera
uniform mat4 view;
uniform mat4 projection;
uniform mat4 invView;
uniform mat4 invProjection;

uniform float nearPlane;
uniform float farPlane;

//parameters
uniform int maxSteps = 50;
uniform int maxBinarySearchSteps = 5;
uniform float maxDepthDiff = 10.0f;
uniform float rayStep = 1.0f;
uniform float thickness = 10.0f;

uniform float edgeFade = 0.1f;
uniform float jitterStrength = 0.5f;
uniform float roughnessThreshold = 0.85f;

vec3 noise(vec2 uv) {
    return fract(sin(vec3(
        dot(uv, vec2(12.9898f, 78.233f)),
        dot(uv, vec2(45.164f, 92.478f)),
        dot(uv, vec2(37.462f, 19.837f))
    )) * 43758.5453f);
}

float linearizeDepth(float depth) {
    float z = depth * 2.0f - 1.0f;
    return (2.0f * nearPlane * farPlane) / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

vec3 getViewPosFromDepth(vec2 uv) {
    vec2 ndc = uv * 2.0f - 1.0f;
    float depth = texture(gDepth, uv).r;
    vec4 clip = vec4(ndc, depth * 2.0f - 1.0f, 1.0f);
    vec4 viewPos = invProjection * clip;
    viewPos /= viewPos.w;
    return viewPos.xyz;
}

vec2 projectToScreen(vec3 viewPos) {
    vec4 clip = projection * vec4(viewPos, 1.0f);
    clip.xyz /= clip.w;
    return clip.xy * 0.5f + 0.5f;
}

bool rayMarch(vec3 rayOrigin, vec3 rayDir, out vec2 hitUV) {
    vec3 rayPos = rayOrigin;
    float stepSize = rayStep;

    for (int i = 0; i < maxSteps; i++) {
        rayPos += rayDir * stepSize;

        //project to screen space
        vec2 uv = projectToScreen(rayPos);

        //check screen bounds
        if (uv.x < 0.0f || uv.x > 1.0f || uv.y < 0.0f || uv.y > 1.0f)
            return false;

        //sample depth at this position
        float sampledDepth = texture(gDepth, uv).r;
        if (sampledDepth >= 0.99999f)
            continue;

        //calculate depth difference
        float depthDiff = -rayPos.z - linearizeDepth(sampledDepth);

        //check for intersection
        if (depthDiff > 0.0f && depthDiff < thickness) {
            //binary search refinement
            vec3 hitPos = rayPos;
            vec3 lastGoodPos = rayPos - rayDir * stepSize;

            for (int j = 0; j < maxBinarySearchSteps; j++) {
                vec3 midPos = (hitPos + lastGoodPos) * 0.5f;
                vec2 midUV = projectToScreen(midPos);

                float midDepth = texture(gDepth, midUV).r;
                float midSampledZ = linearizeDepth(midDepth);
                float midDepthDiff = -midPos.z - midSampledZ;

                if (midDepthDiff > 0.0f && midDepthDiff < thickness) {
                    hitPos = midPos;
                } else {
                    lastGoodPos = midPos;
                }
            }

            hitUV = projectToScreen(hitPos);

            //check if depth difference is within acceptable range
            if (depthDiff < maxDepthDiff)
                return true;
        }

        //increase step size as we get further
        stepSize *= 1.05f;
    }

    return false;
}

void main() {
    //early exit for sky/far plane
    float depth = texture(gDepth, texCoords).r;
    if (depth >= 0.99999f) {
        fragColor = vec4(0.0f);
        return;
    }

    //sample material properties
    float roughness = texture(gORM, texCoords).g;

    //skip highly rough surfaces (no clear reflections)
    if (roughness > roughnessThreshold) {
        fragColor = vec4(0.0f);
        return;
    }

    //get view-space position and normal
    vec3 viewPos = getViewPosFromDepth(texCoords);
    vec3 normal = texture(gNormal, texCoords).rgb;
    vec3 viewNormal = normalize(mat3(view) * normal);

    //calculate reflection direction
    vec3 reflectDir = normalize(reflect(normalize(viewPos), viewNormal));

    //jitter reflection direction based on roughness
    vec3 jitter = (noise(texCoords) * 2.0f - 1.0f) * jitterStrength * roughness;
    reflectDir = normalize(reflectDir + jitter);

    //ray march
    vec2 hitUV;
    if (!rayMarch(viewPos, reflectDir, hitUV)) {
        fragColor = vec4(0.0f);
        return;
    }

    //sample the reflected color
    vec3 reflectedColor = texture(previousTex, hitUV).rgb;

    //edge fade
    vec2 edgeDist = min(hitUV, 1.0f - hitUV);
    float fade = smoothstep(0.0f, edgeFade, edgeDist.x) * smoothstep(0.0f, edgeFade, edgeDist.y);

    //reduce reflection intensity for grazing angles
    float angleFade = dot(-normalize(viewPos), viewNormal);
    fade *= clamp(angleFade * 2.0f, 0.0f, 1.0f);

    fragColor = vec4(reflectedColor, fade);
}
