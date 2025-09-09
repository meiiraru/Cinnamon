#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

//adapted for lens flare only
struct Light {
    vec3 pos, color, direction;
    float falloffStart, falloffEnd, innerAngle, outerAngle;
    int type; //1 = point, 2 = spot, 3 = directional, 4 = cookie

    float flareIntensity, flareFalloff; 
};

in vec2 texCoords;
out vec4 fragColor;

uniform Light light;

uniform vec3 camPos;
uniform mat4 view;
uniform mat4 projection;
uniform float aspectRatio; //screen aspect ratio

//for occlusion testing
uniform sampler2D gDepth;

uniform bool hasFlares = true;

//projects a 3D world point to 2D screen UVs [0, 1]
vec3 projectToScreen(vec3 worldPos) {
    vec4 clipSpace = projection * view * vec4(worldPos, 1.0f);
    if (clipSpace.w <= 0.0f)
        return vec3(-1.0f); // Behind camera, invalid

    vec3 ndc = clipSpace.xyz / clipSpace.w; //perspective divide
    ndc = ndc * 0.5f + 0.5f; //transform from [-1, 1] to [0, 1]
    return ndc;
}

//generates a starburst pattern (anamorphic streaks)
float starburst(vec2 uv, float rays, float falloff) {
    float angle = atan(uv.y, uv.x);
    float d = length(uv);
    float noise = sin(angle * rays) * 0.5f + 0.5f;
    noise = pow(noise, 20.f); //sharpen the rays
    return max(0.0f, noise * pow(1.0f - d, falloff));
}

void main() {
    vec3 finalFlare = vec3(0.0f);

    //project a light 3D world position to 2D screen space
    vec3 lightScreen = projectToScreen(light.pos);
    if (lightScreen.x < -0.25f || lightScreen.x > 1.25f ||
        lightScreen.y < -0.25f || lightScreen.y > 1.25f ||
        lightScreen.z < 0.0f || lightScreen.z > 1.0f)
        discard; //clipped by near/far plane

    //occlusion test
    float pixelDepth = texture(gDepth, texCoords).r;
    bool pixelOccluded = pixelDepth < lightScreen.z - 0.0001f;

    float sceneDepth = texture(gDepth, lightScreen.xy).r;
    bool lightOccluded = sceneDepth < lightScreen.z - 0.0001f;

    //directional fade for spotlights and cookies
    float directionalFade = 1.0f;
    if (light.type == 2 || light.type == 4) {
        //get the direction from the light to the camera
        vec3 lightToCamDir = normalize(camPos - light.pos);

        //dot > 0 means camera is in front of the light
        float alignment = dot(lightToCamDir, light.direction);

        //soft fade out at the edges
        directionalFade = smoothstep(0.0f, light.innerAngle, alignment);
    }

    //distance attenuation
    float distanceAttenuation = 1.0f;
    if (light.type != 3) {
        float distToLight = distance(light.pos, camPos);
        float falloffFade = smoothstep(light.falloffEnd, light.falloffStart, distToLight);
        float cameraDistFade = 1.0f / (1.0f + distToLight * distToLight * 0.1f);
        distanceAttenuation = falloffFade * cameraDistFade;
    }

    //base brightness
    vec3 flareColor = light.color * light.flareIntensity * distanceAttenuation * directionalFade;

    //if the flare is completely out, we can skip the rest of the expensive math
    if (dot(flareColor, flareColor) < 0.0001f)
        discard;

    //glare and starburst only if not occluded
    if (!pixelOccluded) {
        //vector from current pixel to the light screen position
        vec2 uv = texCoords - lightScreen.xy;
        uv.x *= aspectRatio;

        //main glare - a soft glow right at the light position
        float glare = max(0.0f, pow(max(0.0f, 1.0f - length(uv)), light.flareFalloff));
        finalFlare += glare * flareColor * 2.0f;

        //starburst - anamorphic streaks
        float stars = starburst(uv, 8.0f, light.flareFalloff);//8 rays, sharp falloff
        finalFlare += stars * glare * flareColor;
    }

    //if the light is occluded, skip the rest
    if (lightOccluded || !hasFlares) {
        fragColor = vec4(finalFlare, 1.0f);
        return;
    }

    //screen center
    vec2 screenCenter = vec2(0.5f);
    vec2 axisVec = lightScreen.xy - screenCenter;

    //halos - reflections inside the lens
    //they appear on the opposite side of the screen from the light
    vec2 haloScreenPos = screenCenter - axisVec * (0.2f - 3.0f * 0.3f);
    vec2 uvHalo = texCoords - haloScreenPos;
    uvHalo.x *= aspectRatio;

    //treat halo as a ghost
    vec3 haloColor;
    haloColor.r = smoothstep(0.1f, 0.11f, length(uvHalo - 0.005f)) - smoothstep(0.12f, 0.13f, length(uvHalo - 0.005f));
    haloColor.g = smoothstep(0.1f, 0.11f, length(uvHalo)) - smoothstep(0.12f, 0.13f, length(uvHalo));
    haloColor.b = smoothstep(0.1f, 0.11f, length(uvHalo + 0.005f)) - smoothstep(0.12f, 0.13f, length(uvHalo + 0.005f));

    finalFlare += haloColor * flareColor * 0.05f; //use the base flareColor for brightness

    //add a few ghosts
    for (int j = 0; j < 5; j++) {
        vec2 ghostScreenPos = screenCenter - axisVec * (0.2f + (j - 2.0f) * 0.3f);
        vec2 uvGhost = texCoords - ghostScreenPos;
        uvGhost.x *= aspectRatio;

        //chromatic aberration - offset colors slightly
        vec3 ghostColor;
        ghostColor.r = pow(max(0.0f, 0.05f - length(uvGhost - vec2(0.003f, 0.0f) * aspectRatio)), 1.0f) * light.color.r;
        ghostColor.g = pow(max(0.0f, 0.05f - length(uvGhost)), 1.0f) * light.color.g;
        ghostColor.b = pow(max(0.0f, 0.05f - length(uvGhost + vec2(0.003f, 0.0f) * aspectRatio)), 1.0f) * light.color.b;

        finalFlare += ghostColor * light.flareIntensity * 2.0f * distanceAttenuation;
    }

    fragColor = vec4(finalFlare, 1.0f);
}