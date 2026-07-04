
uniform int frameIndex;

//4x4 bayer matrix
const float ditherPattern[16] = float[16](
         0.0f / 16.0f,  8.0f / 16.0f,  2.0f / 16.0f, 10.0f / 16.0f,
        12.0f / 16.0f,  4.0f / 16.0f, 14.0f / 16.0f,  6.0f / 16.0f,
         3.0f / 16.0f, 11.0f / 16.0f,  1.0f / 16.0f,  9.0f / 16.0f,
        15.0f / 16.0f,  7.0f / 16.0f, 13.0f / 16.0f,  5.0f / 16.0f
);

//random offset based on world position
float random(vec3 pos) {
    return fract(sin(dot(pos, vec3(12.9898f, 78.233f, 45.164f))) * 43758.5453f);
}

//TAA dithered transparency
bool shouldDiscard(vec4 color, vec3 pos) {
    //not transparent
    if (color.a >= 0.99f)
        return false;

    //fully transparent
    if (color.a < 0.01f)
        return true;

    //generate a spatial offset so overlapping objects do not share the same grid
    int spatialOffset = int(random(pos) * 16.0f);

    //add the frameIndex so the pattern shifts every frame for TAA
    int offset = spatialOffset + frameIndex;

    //calculate screen-space coordinates with the temporal offset
    int x = (int(gl_FragCoord.x) + offset) % 4;
    int y = (int(gl_FragCoord.y) + (offset / 4)) % 4;

    float threshold = ditherPattern[y * 4 + x];

    //discard only if below the threshold
    return color.a < threshold;
}