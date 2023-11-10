
in vec4 shadowPos;

uniform sampler2D shadowMap;
uniform vec3 shadowDir;

float shadowMapSample(vec2 coords, float compareDepth) {
    return 1 - step(compareDepth, texture2D(shadowMap, coords.xy).r);
}

float shadowMaplinearSample(vec2 coords, float compareDepth, vec2 texelSize) {
    vec2 pixelPos = coords / texelSize + vec2(0.5);
    vec2 fracPart = fract(pixelPos);
    vec2 startTexel = (pixelPos - fracPart) * texelSize;

    float blTexel = shadowMapSample(startTexel, compareDepth);
    float brTexel = shadowMapSample(startTexel + vec2(texelSize.x, 0), compareDepth);
    float tlTexel = shadowMapSample(startTexel + vec2(0, texelSize.y), compareDepth);
    float trTexel = shadowMapSample(startTexel + texelSize, compareDepth);

    float mixA = mix(blTexel, tlTexel, fracPart.y);
    float mixB = mix(brTexel, trTexel, fracPart.y);

    return mix(mixA, mixB, fracPart.x);
}

float calculateShadow(vec3 normal, vec3 shadowDir) {
    //convert to range [-1,1]
    vec3 lightCoords = shadowPos.xyz / shadowPos.w;

    //outside shadowmap range
    if (lightCoords.z > 1)
        return 0;

    //range [0,1]
    lightCoords = lightCoords * 0.5f + 0.5f;

    float shadow = 0;

    float currentDepth = lightCoords.z;
    float dir = max(dot(normal, shadowDir), 0);

    const int sampleRadius = 1;
    vec2 pixelSize = 1.0f / textureSize(shadowMap, 0);
    float bias = pixelSize.x;
    currentDepth -= bias * 3;

    for (int y = -sampleRadius; y <= sampleRadius; y++) {
        for (int x = -sampleRadius; x <= sampleRadius; x++) {
            vec2 offset = vec2(x, y) * pixelSize;
            shadow += shadowMaplinearSample(lightCoords.xy + offset, currentDepth, pixelSize) * dir;
        }
    }

    return shadow /= pow(sampleRadius * 2 + 1, 2);
}