#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;
out highp float gl_FragDepth;

uniform vec2 resolution;
uniform vec3 camPos;
uniform vec3 sunColor;
uniform vec3 sunDir;
uniform vec3 cloudsColor;

uniform mat4 invView;
uniform mat4 invProjection;
uniform mat4 view;
uniform mat4 projection;

uniform sampler2D noiseTex;
uniform sampler2D blueNoiseTex;
uniform sampler2D gDepth;

uniform float time;

uniform vec3 cloudPos;
uniform float cloudScale = 32.0f;
uniform float cloudCoverage = 1.0f;
uniform float noiseScale = 0.1f;

uniform int MAX_STEPS = 60;
uniform int MAX_STEPS_LIGHTS = 5;
uniform float MARCH_SIZE = 0.3f;
uniform float ABSORPTION_COEFFICIENT = 0.9f;
uniform float SCATTERING_ANISO = 0.3f;

const float PI = 3.14159265359f;

vec3 getDirFromUV(vec2 ndc) {
    vec4 clipSpace = vec4(ndc, 1.0f, 1.0f);
    vec4 viewSpace = invProjection * clipSpace;
    viewSpace = vec4(viewSpace.xyz / viewSpace.w, 0.0f);
    return normalize((invView * viewSpace).xyz);
}

float getDistanceAtDepth(vec2 ndc, float depth) {
    //convert depth buffer value to world-space distance
    vec4 depthClip = vec4(ndc, depth * 2.0f - 1.0f, 1.0f);
    vec4 depthView = invProjection * depthClip;
    depthView /= depthView.w;
    vec3 worldP = (invView * depthView).xyz;

    //convert world space distance to local space distance
    return length(worldP - camPos);
}

float getCloudDepth(float hitDepthLocal, vec3 rayOrigin, vec3 rayDir) {
    //local cloud space
    vec3 hitPosLocal = rayOrigin + hitDepthLocal * rayDir;
    //world space position
    vec3 hitWorld = hitPosLocal * cloudScale + cloudPos;

    //project to clip space to get the depth value
    vec4 clip = projection * view * vec4(hitWorld, 1.0f);
    float ndcZ = clip.z / clip.w;
    return ndcZ * 0.5f + 0.5f;
}

float sdBox(vec3 p, float width, float height, float depth) {
    //absolute p sub half-extents
    float qx = abs(p.x) - width;
    float qy = abs(p.y) - height;
    float qz = abs(p.z) - depth;

    //distance to the faces, if outside
    float outsideDist = length(vec3(max(qx, 0.0f), max(qy, 0.0f), max(qz, 0.0f)));

    //negative distance if inside, based on furthest axis from center
    float insideDist = min(max(qx, max(qy, qz)), 0.0f);

    //final signed distance
    return outsideDist + insideDist;
}

bool intersectAABB(vec3 ro, vec3 rd, vec3 bmin, vec3 bmax, out float tEnter, out float tExit) {
    vec3 invDir = 1.0f / rd;
    vec3 t0 = (bmin - ro) * invDir;
    vec3 t1 = (bmax - ro) * invDir;

    vec3 tMin = min(t0, t1);
    vec3 tMax = max(t0, t1);

    tEnter = max(max(tMin.x, tMin.y), tMin.z);
    tExit = min(min(tMax.x, tMax.y), tMax.z);

    return tExit >= max(tEnter, 0.0f);
}

float beersLaw(float dist, float absorption) {
    return exp(-dist * absorption);
}

float henyeyGreenstein(float g, float mu) {
    float gg = g * g;
    return (1.0f / (4.0f * PI))  * ((1.0f - gg) / pow(1.0f + gg - 2.0f * g * mu, 1.5f));
}

float noise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0f - 2.0f * f); //smoothstep interpolation

    vec2 noiseSize = vec2(textureSize(noiseTex, 0));

    //hash the Z coordinate to a 2D offset for the current and next Z plane
    vec2 uvZ1 = vec2(37.0f, 239.0f) * p.z;
    vec2 uvZ2 = vec2(37.0f, 239.0f) * (p.z + 1.0f);

    //add the XY position + the smooth fraction
    vec2 uv1 = (p.xy + uvZ1) + f.xy;
    vec2 uv2 = (p.xy + uvZ2) + f.xy;

    //sample the texture twice
    float val1 = textureLod(noiseTex, (uv1 + 0.5f) / noiseSize, 0.0f).r;
    float val2 = textureLod(noiseTex, (uv2 + 0.5f) / noiseSize, 0.0f).r;

    //interpolate along Z
    return mix(val1, val2, f.z) * 2.0f - 1.0f;
}

float fbm(vec3 p, int octaves) {
    vec3 q = p + time * 0.5f * vec3(1.0f, -0.2f, -1.0f);
    float g = noise(q * 0.3f);

    float f = 0.0f;
    float scale = 0.45f;
    float factor = 2.02f;

    for (int i = 0; i < octaves; i++) {
        f += scale * noise(q);
        q *= factor;
        factor += 0.21f;
        scale *= 0.5f;
    }

    //use g to blend between sparse and full detail
    f = mix(f * 0.1f - 0.5f, f, (0.5f + 0.5f * g) * (0.5f + 0.5f * g));

    return f;
}

float scene(vec3 p, int octaves) {
    float r = MAX_STEPS * MARCH_SIZE;
    float distance = sdBox(p, r, 0.5f, r);

    //sample fbm at world-space position, scaled by noiseScale for blob size
    vec3 worldP = (p * cloudScale + cloudPos) * noiseScale;
    float f = fbm(worldP, octaves);

    //cloudCoverage shifts the density: 0 = clear, 1 = overcast
    return -distance + f - (1.0f - cloudCoverage);
}

float lightmarch(vec3 position, vec3 rayDirection, float marchSize) {
    float totalDensity = 0.0f;

    for (int step = 0; step < MAX_STEPS_LIGHTS; step++) {
        position += -sunDir * marchSize * float(step);

        float lightSample = scene(position, 2);
        totalDensity += lightSample;
    }

    float transmittance = beersLaw(totalDensity, ABSORPTION_COEFFICIENT);
    return transmittance;
}

float raymarch(vec3 rayOrigin, vec3 rayDirection, float offset, float marchSize, float startDepth, float endDepth, out float outHitDepth) {
    //start marching from the near intersection, with blue noise dither
    float depth = startDepth + marchSize * offset;
    if (depth > endDepth)
        depth = startDepth;
    vec3 p = rayOrigin + depth * rayDirection;

    float totalTransmittance = 1.0f;
    float lightEnergy = 0.0f;

    float phase = henyeyGreenstein(SCATTERING_ANISO, dot(rayDirection, -sunDir));

    outHitDepth = -1.0f;

    for (int i = 0; i < MAX_STEPS; i++) {
        //stop if we have passed the bounding box or scene geometry
        if (depth >= endDepth)
            break;

        //adaptive step size
        float dt = max(marchSize, 0.02f * depth);
        dt = min(dt, endDepth - depth);
        if (dt <= 0.0f)
            break;

        //sample density with a distance-based LOD
        int oct = clamp(5 - int(log2(1.0f + depth * 0.5f)), 2, 5);
        float density = scene(p, oct);

        //density only if above threshold
        if (density > 0.01f) {
            //record first hit depth if not already set
            if (outHitDepth < 0.0f)
                outHitDepth = depth;

            float lightTransmittance = lightmarch(p, rayDirection, dt * 0.2f);
            float luminance = 0.025f + density * phase;

            totalTransmittance *= lightTransmittance;
            lightEnergy += totalTransmittance * luminance;

            //early opacity termination if nearly fully opaque
            if (totalTransmittance < 0.01f)
                break;
        }

        depth += dt;
        p = rayOrigin + depth * rayDirection;
    }

    return clamp(lightEnergy, 0.0f, 1.0f);
}

void main() {
    //reconstruct pixel ray direction from screen uv
    vec2 ndc = texCoords * 2.0f - 1.0f;
    vec3 rayDir = getDirFromUV(ndc);
    //ray origin from camera into cloud space
    vec3 rayOrigin = (camPos - cloudPos) / cloudScale;

    //restrict marching to the cloud volume bounds
    float r = float(MAX_STEPS) * MARCH_SIZE;
    float tEnter, tExit;
    if (!intersectAABB(rayOrigin, rayDir, vec3(-r, -0.5f, -r), vec3(r, 0.5f, r), tEnter, tExit))
        discard;

    float marchStart = max(tEnter, 0.0f);
    float marchEnd = tExit;
    if (marchEnd <= marchStart)
        discard;

    //reconstruct world space position from depth buffer
    float sceneDepth = texture(gDepth, texCoords).r;
    float maxDepth = sceneDepth < 0.99999f ? getDistanceAtDepth(ndc, sceneDepth) / cloudScale : MAX_STEPS * MARCH_SIZE;

    //skip if opaque scene geometry is closer than cloud entry
    if (maxDepth <= marchStart)
        discard;

    marchEnd = min(marchEnd, maxDepth);
    if (marchEnd <= marchStart)
        discard;

    //blue noise to dither the raymarch start position
    vec2 blueNoiseSize = vec2(textureSize(blueNoiseTex, 0));
    float blueNoise = texture(blueNoiseTex, texCoords * resolution / blueNoiseSize).r;
    float offset = fract(blueNoise);

    //raymarch in cloud space, and capture the hit depth
    float hitDepthLocal = -1.0f;
    float res = raymarch(rayOrigin, rayDir, offset, MARCH_SIZE, marchStart, marchEnd, hitDepthLocal);
    if (res <= 0.0f)
        discard;

    //if we got a hit, convert the hit position to world space and write to depth buffer
    if (hitDepthLocal >= 0.0f)
        gl_FragDepth = getCloudDepth(hitDepthLocal, rayOrigin, rayDir);

    fragColor = vec4(cloudsColor + sunColor * res, 1.0f);
}