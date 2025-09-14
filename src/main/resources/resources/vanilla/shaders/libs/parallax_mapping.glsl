
const float minParallax = 8;
const float maxParallax = 32;

vec2 parallaxMapping(vec2 texCoords, vec3 viewDir, sampler2D heightTex, float heightScale) {
    //number of depth layers
    float numLayers = mix(maxParallax, minParallax, abs(dot(vec3(0.0f, 0.0f, 1.0f), viewDir)));
    //depth of each layer
    float layerDepth = 1.0f / numLayers;

    //current depth
    float currentLayerDepth = 0.0f;

    //shift of texture coordinates for each layer (from vector P)
    vec2 deltaTexCoords = viewDir.xy / viewDir.z * heightScale / numLayers;

    //current texture coordinates
    vec2 currentTexCoords = texCoords;
    //get depth from height map
    float currentDepthMapValue = texture(heightTex, currentTexCoords).r;

    //test until we find the depth layer that is below the surface
    while (currentLayerDepth < currentDepthMapValue) {
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
    float weight = afterDepth / (afterDepth - beforeDepth);
    vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0f - weight);

    return finalTexCoords;
}