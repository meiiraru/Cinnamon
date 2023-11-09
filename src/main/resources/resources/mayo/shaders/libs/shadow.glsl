
in vec4 shadowPos;

uniform sampler2D shadowMap;
uniform vec3 shadowDir;

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

    int sampleRadius = 1;
    vec2 pixelSize = 1.0f / textureSize(shadowMap, 0);
    for (int y = -sampleRadius; y <= sampleRadius; y++) {
        for (int x = -sampleRadius; x <= sampleRadius; x++) {
            float closestDepth = texture(shadowMap, lightCoords.xy + vec2(x, y) * pixelSize).r;
            if (currentDepth > closestDepth)
                shadow += 1 * dir;
        }
    }

    return shadow /= pow(sampleRadius * 2 + 1, 2);
}