#type vertex
#version 330 core
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;

out vec3 pos;
out float localHeight;

uniform mat4 projection;
uniform mat4 view;

uniform vec3 camPos;
uniform float cloudBase;

void main() {
    vec3 worldPos = aPosition + vec3(camPos.x, cloudBase, camPos.z);
    gl_Position = projection * view * vec4(worldPos, 1.0f);
    pos = worldPos;
    localHeight = aPosition.y;
}

#type fragment
#version 330 core

in vec3 pos;
in float localHeight;

out vec4 fragColor;

uniform vec3 camPos;
uniform float time;

uniform vec3 cloudsColor;
uniform vec3 wind = vec3(0.5f, 0.0f, 0.2f);

uniform float planeRadius = 512.0f;
uniform float coverage = 0.5f;
uniform float cloudScale = 0.025f;
uniform float cloudThickness = 16.0f;

float hash(float n) {
    return fract(sin(n) * 753.5453123f);
}

float noise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0f - 2.0f * f);
    float n = p.x + p.y * 157.0f + 113.0f * p.z;
    return mix(mix(mix(hash(n + 0.0f),   hash(n + 1.0f), f.x),
                   mix(hash(n + 157.0f), hash(n + 158.0f), f.x), f.y),
               mix(mix(hash(n + 113.0f), hash(n + 114.0f), f.x),
                   mix(hash(n + 270.0f), hash(n + 271.0f), f.x), f.y), f.z);
}

// Fractal Brownian Motion for fluffy detail
float fbm(vec3 p) {
    float f = 0.0f;
    f += 0.5f    * noise(p); p *= 2.01f;
    f += 0.25f   * noise(p); p *= 2.02f;
    f += 0.125f  * noise(p); p *= 2.03f;
    f += 0.0625f * noise(p);
    return f;
}

void main() {
    if (coverage <= 0.0f)
        discard;

    //circular edge fade
    float distFromCam = length(pos.xz - camPos.xz);
    float fadeStart = planeRadius * 0.6f;
    float fadeEnd = planeRadius * 0.95f;

    float edgeAlpha = 1.0f - smoothstep(fadeStart, fadeEnd, distFromCam);
    if (edgeAlpha <= 0.0f)
        discard;

    //vertical density fade with a bell-curve shape
    float t = clamp(localHeight / cloudThickness, 0.0f, 1.0f);
    float verticalDensity = smoothstep(0.0f, 0.3f, t) * (1.0f - smoothstep(0.7f, 1.0f, t));
    if (verticalDensity <= 0.0f)
        discard;

    //noise-based cloud density
    vec3 noiseCoord = (pos + wind * time) * cloudScale;
    float density = fbm(noiseCoord);

    //large-scale noise for big cloud shapes
    vec3 largeCoord = (pos + wind * time * 0.3f) * cloudScale * 0.3f;
    float largeDensity = fbm(largeCoord);
    density = density * 0.7f + largeDensity * 0.3f;

    //reduce density towards the cloud edges vertically
    density *= verticalDensity;

    //coverage mapping
    float minThreshold = mix(0.85f, -0.1f, coverage);
    float maxThreshold = minThreshold + 0.4f;

    float noiseAlpha = smoothstep(minThreshold, maxThreshold, density);
    if (noiseAlpha <= 0.0f)
        discard;

    //keep a low layer alpha to allow natual accumulation
    float layerAlpha = edgeAlpha * noiseAlpha * 0.3f;

    //self-shadowing
    vec3 finalColor = cloudsColor * mix(1.0f, 0.6f, density);

    //brighten edges of cloud puffs
    float edgeBrightness = smoothstep(0.0f, 0.3f, noiseAlpha) * (1.0f - smoothstep(0.3f, 1.0f, noiseAlpha));
    finalColor += cloudsColor * edgeBrightness * 0.15f;

    fragColor = vec4(finalColor, layerAlpha);
}