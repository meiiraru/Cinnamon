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

uniform sampler2D noiseTex;
uniform sampler2D blueNoiseTex;
uniform sampler2D gDepth;

uniform float time;
uniform int frame;

uniform vec3 cloudPos;       // world-space position of the cloud volume center
uniform float cloudScale;    // world-space scale (e.g. 50.0 = 50x bigger)
uniform float cloudCoverage; // 0.0 = clear sky, 1.0 = overcast (shifts density threshold)
uniform float noiseScale;    // noise frequency multiplier (higher = smaller blobs, lower = bigger blobs)

uniform int MAX_STEPS = 60;
uniform int MAX_STEPS_LIGHTS = 6;
uniform float MARCH_SIZE = 0.3f;
uniform float ABSORPTION_COEFFICIENT = 0.9f;
uniform float SCATTERING_ANISO = 0.3f;
const float PI = 3.14159265359f;

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
    f = f * f * (3.0f - 2.0f * f); // Smoothstep interpolation

    vec2 noiseSize = vec2(textureSize(noiseTex, 0));

    // Magic numbers to "hash" the Z coordinate to a 2D offset
    // We calculate offsets for the current Z plane and the Next Z plane
    vec2 uvZ1 = vec2(37.0f, 239.0f) * p.z;
    vec2 uvZ2 = vec2(37.0f, 239.0f) * (p.z + 1.0f);

    // Add the XY position + the smooth fraction
    vec2 uv1 = (p.xy + uvZ1) + f.xy;
    vec2 uv2 = (p.xy + uvZ2) + f.xy;

    // Sample the texture twice
    // We only need the Red channel (.x) since we are treating it as a value map
    float val1 = textureLod(noiseTex, (uv1 + 0.5f) / noiseSize, 0.0f).x;
    float val2 = textureLod(noiseTex, (uv2 + 0.5f) / noiseSize, 0.0f).x;

    // Linearly interpolate along Z
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

    // Use g to blend between sparse and full detail (from original shader)
    f = mix(f * 0.1f - 0.5f, f, (0.5f + 0.5f * g) * (0.5f + 0.5f * g));

    return f;
}

float scene(vec3 p, int octaves) {
    float distance = sdBox(p, 9.0f, 0.5f, 9.0f);
    // Sample fbm at world-space position, scaled by noiseScale for blob size
    vec3 worldP = (p * cloudScale + cloudPos) * noiseScale;
    float f = fbm(worldP, octaves);
    // cloudCoverage shifts the density: 0 = clear, 1 = overcast
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

float raymarch(vec3 rayOrigin, vec3 rayDirection, float offset, float marchSize, float maxDepth) {
    // Start marching from the near intersection, with blue noise dither
    float depth = marchSize * offset;
    vec3 p = rayOrigin + depth * rayDirection;

    float totalTransmittance = 1.0f;
    float lightEnergy = 0.0f;

    float phase = henyeyGreenstein(SCATTERING_ANISO, dot(rayDirection, -sunDir));

    for (int i = 0; i < MAX_STEPS; i++) {
        // Stop if we've passed the bounding box or scene geometry
        if (depth >= maxDepth)
            break;

        // Adaptive step size: take larger steps when further from the camera
        float dt = max(marchSize, 0.02f * depth);

        // Distance-based LOD: fewer noise octaves for distant samples
        int oct = clamp(5 - int(log2(1.0f + depth * 0.5f)), 2, 5);

        float density = scene(p, oct);

        // Density only if above threshold
        if (density > 0.01f) {
            float lightTransmittance = lightmarch(p, rayDirection, dt * 0.2f);
            float luminance = 0.025f + density * phase;

            totalTransmittance *= lightTransmittance;
            lightEnergy += totalTransmittance * luminance;

            // Early opacity termination - stop if nearly fully opaque
            if (totalTransmittance < 0.01f)
                break;
        }

        depth += dt;
        p = rayOrigin + depth * rayDirection;
    }

    return clamp(lightEnergy, 0.0f, 1.0f);
}

void main() {
    vec2 uv = texCoords;

    // Reconstruct per-pixel ray direction from screen UV
    vec2 ndc = uv * 2.0f - 1.0f;
    vec4 clipSpace = vec4(ndc, 1.0f, 1.0f);
    vec4 viewSpace = invProjection * clipSpace;
    viewSpace = vec4(viewSpace.xyz / viewSpace.w, 0.0f);
    vec3 rd = normalize((invView * viewSpace).xyz);

    // Ray Origin - transform camera into cloud-local space
    vec3 ro = (camPos - cloudPos) / cloudScale;

    // Scene depth - reconstruct world-space position from depth buffer
    float sceneDepth = texture(gDepth, uv).r;
    float maxDepth = MAX_STEPS * MARCH_SIZE; // default: no geometry blocking
    if (sceneDepth < 0.99999f) {
        vec4 depthClip = vec4(ndc, sceneDepth * 2.0f - 1.0f, 1.0f);
        vec4 depthView = invProjection * depthClip;
        depthView /= depthView.w;
        vec3 worldP = (invView * depthView).xyz;
        // Convert world-space distance to local-space distance
        float worldDist = length(worldP - camPos);
        maxDepth = worldDist / cloudScale;
    }

    vec2 blueNoiseSize = vec2(textureSize(blueNoiseTex, 0));
    float blueNoise = texture(blueNoiseTex, uv * resolution / blueNoiseSize).r;
    float offset = fract(blueNoise);

    // Raymarch in cloud-local space
    float res = raymarch(ro, rd, offset, MARCH_SIZE, maxDepth);
    if (res <= 0.0f)
        discard;

    fragColor = vec4(cloudsColor + sunColor * res, 1.0f);
}