#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;

uniform vec3 color;
uniform vec3 direction;
uniform float intensity;

uniform mat4 view;
uniform mat4 projection;
uniform vec3 camPos;
uniform float aspectRatio;
uniform vec2 sampleRadius;

//for occlusion testing
uniform sampler2D gDepth;

//projects a 3D world point to 2D screen UVs [0, 1]
vec3 projectToScreen(vec3 worldPos) {
    vec4 clipSpace = projection * view * vec4(worldPos, 1.0f);
    if (clipSpace.w <= 0.0f)
        return vec3(-1.0f); //behind camera, invalid

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
    vec3 lightWorldPos = camPos - direction * 1000.0f; //distant directional light

    //project a light 3D world position to 2D screen space
    vec3 lightScreen = projectToScreen(lightWorldPos);
    if (lightScreen.z < 0.0f || lightScreen.z > 1.0f)
        discard; //clipped by near/far plane

    //light pos occlusion test
    float lightDepth = lightScreen.z;
    float visibility = 0.0f;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x), float(y)) * sampleRadius;
            vec2 samplePos = lightScreen.xy + offset;
            if (samplePos.x < 0.0f || samplePos.y < 0.0f || samplePos.x > 1.0f || samplePos.y > 1.0f)
            continue; //skip samples outside the screen

            if (texture(gDepth, samplePos).r >= lightDepth)
            visibility += 1.0f;
        }
    }
    visibility /= 9.0f;

    //pixel occlusion test
    float pixelDepth = texture(gDepth, texCoords).r;
    bool pixelOccluded = pixelDepth < lightScreen.z;
    //if pixel is not occluded, glare is fully visible
    float glareVisibility = pixelOccluded ? visibility : 1.0f;

    vec3 glareColor = color * glareVisibility * intensity;
    //if the glare is completely out, we can skip all calculations
    if (dot(glareColor, glareColor) < 0.0001f)
        discard;

    vec3 finalFlare = vec3(0.0f);

    //glare and starburst
    //vector from current pixel to the light screen position
    vec2 uv = texCoords - lightScreen.xy;
    uv.x *= aspectRatio;

    //main glare - a soft glow right at the light position
    float glare = max(0.0f, pow(max(0.0f, 1.0f - length(uv)), 15.0f));
    finalFlare += glare * glareColor * 2.0f;

    //starburst - anamorphic streaks
    float stars = starburst(uv, 8.0f, 10.0f);//8 rays, sharp falloff
    finalFlare += stars * glare * glareColor;

    //if the light is occluded, skip the rest
    vec3 flareColor = color * visibility * intensity;
    if (visibility < 0.01f || dot(flareColor, flareColor) < 0.0001f) {
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
        ghostColor.r = pow(max(0.0f, 0.05f - length(uvGhost - vec2(0.003f, 0.0f) * aspectRatio)), 1.0f) * color.r;
        ghostColor.g = pow(max(0.0f, 0.05f - length(uvGhost)), 1.0f) * color.g;
        ghostColor.b = pow(max(0.0f, 0.05f - length(uvGhost + vec2(0.003f, 0.0f) * aspectRatio)), 1.0f) * color.b;

        finalFlare += ghostColor * 2.0f;
    }

    fragColor = vec4(finalFlare, 1.0f);
}