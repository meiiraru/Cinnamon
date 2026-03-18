#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D lutTex;
uniform vec2 lutGrid = vec2(8.0f);

vec3 sampleSliceBilinear(ivec2 grid, vec2 tileSize, float sliceIndex, vec2 rg) {
    vec2 lutSize = vec2(textureSize(lutTex, 0));

    //slice tile in atlas (row-major)
    vec2 tile = vec2(mod(sliceIndex, float(grid.x)), floor(sliceIndex / float(grid.x)));

    //map rg to texel centers inside the tile to avoid crossing tile borders
    vec2 pix = tile * tileSize + (rg * (tileSize - 1.0f) + 0.5f);
    vec2 uv  = pix / lutSize;

    //return the bilinear sample from the slice
    return textureLod(lutTex, uv, 0.0f).rgb;
}

void main() {
    //original color
    vec4 color = clamp(texture(colorTex, texCoords), 0.0f, 1.0f);

    //grid and slice information
    ivec2 gridI = ivec2(lutGrid + 0.5f);
    int sliceCount = max(gridI.x * gridI.y, 1);
    float slicesMinus1 = float(sliceCount - 1);
    vec2 tileSize = vec2(textureSize(lutTex, 0)) / vec2(gridI);

    //get slices from blue
    float bScaled = color.b * slicesMinus1;
    float slice0 = floor(bScaled);
    float slice1 = min(slice0 + 1.0f, slicesMinus1);
    float sliceDelta = fract(bScaled);

    //tri-linear between the two slices
    vec3 lut0 = sampleSliceBilinear(gridI, tileSize, slice0, color.rg);
    vec3 lut1 = sampleSliceBilinear(gridI, tileSize, slice1, color.rg);
    vec3 lut = mix(lut0, lut1, sliceDelta);

    fragColor = vec4(lut, color.a);
}