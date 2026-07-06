
uniform int frameIndex;

//4x4 bayer matrix
const float ditherPattern[16] = float[16](
         0.0f / 16.0f,  8.0f / 16.0f,  2.0f / 16.0f, 10.0f / 16.0f,
        12.0f / 16.0f,  4.0f / 16.0f, 14.0f / 16.0f,  6.0f / 16.0f,
         3.0f / 16.0f, 11.0f / 16.0f,  1.0f / 16.0f,  9.0f / 16.0f,
        15.0f / 16.0f,  7.0f / 16.0f, 13.0f / 16.0f,  5.0f / 16.0f
);

//TAA dithered transparency
bool shouldDiscard(vec4 color, vec3 pos) {
    //not transparent
    if (color.a >= 0.99f)
        return false;

    //fully transparent
    if (color.a < 0.01f)
        return true;

    //base bayer threshold
    int x = int(gl_FragCoord.x) % 4;
    int y = int(gl_FragCoord.y) % 4;
    float baseThreshold = ditherPattern[y * 4 + x];

    //temporal shift based on frame index and primitive id
    float temporalShift = float((frameIndex % 4096) + (gl_PrimitiveID % 137)) * 0.61803398875f;
    float threshold = fract(baseThreshold + temporalShift);

    //discard only if below the threshold
    return color.a < threshold;
}