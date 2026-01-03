
const int minParallax = 8;
const int maxParallax = 32;

vec2 parallaxMapping(vec2 texCoords, vec3 viewDir, sampler2D heightTex, float heightScale) {
    //early skip for no scale or perpendicular view
    if (heightScale <= 0.0f || abs(viewDir.z) < 1e-4f)
        return texCoords;

    //number of depth layers
    float ndotl = abs(dot(vec3(0.0f, 0.0f, 1.0f), viewDir));
    int numLayers = clamp(int(mix(maxParallax, minParallax, ndotl)), minParallax, maxParallax);
    float layerDepth = 1.0f / numLayers;

    //current depth
    float currentLayerDepth = 0.0f;

    //shift of texture coordinates for each layer (from vector P)
    vec2 deltaTexCoords = (viewDir.xy / viewDir.z) * (heightScale / numLayers);

    //current texture coordinates
    vec2 currentTexCoords = texCoords;
    //get depth from height map
    float currentDepthMapValue = texture(heightTex, currentTexCoords).r;

    //test until we find the depth layer that is below the surface
    for (int i = 0; i < numLayers && currentLayerDepth < currentDepthMapValue; i++) {
        //shift texture coordinates along vector P
        currentTexCoords -= deltaTexCoords;
        //get depth from height map
        currentDepthMapValue = texture(heightTex, currentTexCoords).r;
        //go to next depth layer
        currentLayerDepth += layerDepth;
    }

    //get texture coordinates before and after intersection
    vec2 prevTexCoords = currentTexCoords + deltaTexCoords;
    float afterDepth = currentDepthMapValue - currentLayerDepth;
    float beforeDepth = texture(heightTex, prevTexCoords).r - currentLayerDepth + layerDepth;

    //linear interpolation of texture coordinates
    float delta = afterDepth - beforeDepth;
    if (abs(delta) < 1e-6f)
        return currentTexCoords;

    float weight = afterDepth / delta;
    vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0f - weight);

    return finalTexCoords;
}