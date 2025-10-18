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
    //map r, g in [0,1] to pixel space inside the tile
    vec2 rgPix = rg * (tileSize - 1.0f);

    //tile coordinates for the slice (row-major)
    vec2 tileOrigin = tileSize * vec2(mod(sliceIndex, float(grid.x)), floor(sliceIndex / float(grid.x)));

    //integer texel coordinates for bilinear fetch, stay within tile bounds
    vec2 p = tileOrigin + rgPix;
    vec2 p0 = floor(p);
    vec2 p1 = min(p0 + 1.0f, tileOrigin + (tileSize - 1.0f));
    vec2 f = p - p0;

    //fetch the four texels
    vec3 t00 = texelFetch(lutTex, ivec2(p0),         0).rgb;
    vec3 t10 = texelFetch(lutTex, ivec2(p1.x, p0.y), 0).rgb;
    vec3 t01 = texelFetch(lutTex, ivec2(p0.x, p1.y), 0).rgb;
    vec3 t11 = texelFetch(lutTex, ivec2(p1),         0).rgb;

    //bilinear interpolation
    vec3 cx0 = mix(t00, t10, f.x);
    vec3 cx1 = mix(t01, t11, f.x);
    return mix(cx0, cx1, f.y);
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