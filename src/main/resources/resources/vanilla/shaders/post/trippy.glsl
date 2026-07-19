#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core
#include shaders/libs/color_utils.glsl

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 resolution;
uniform float count;
uniform float time;
uniform float scale;
uniform float distortion;

const float PI = 3.14159265359f;

//kaleidoscope mirror effect
vec2 kaleidoscope(vec2 uv, vec2 offset, float sides) {
    float angle = atan(uv.y, uv.x);
    angle = ((angle / PI) + 1.0f) * 0.5f;
    angle = mod(angle, 1.0f / sides) * sides;
    angle = -abs(2.0f * angle - 1.0f) + 1.0f;
    float y = length(uv);
    angle = angle * y;
    return vec2(angle, y) - offset;
}

//glowing orbs
vec4 orb(vec2 uv, float size, vec2 position, vec3 color, float contrast) {
    float dist = max(length(uv + position), 0.0001f);
    vec4 col = vec4((size / dist) * color, 1.0f);
    return pow(col, vec4(contrast));
}

//2D rotation matrix
mat2 rotate(float angle) {
    return mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
}

void main() {
    vec2 fragCoord = texCoords * resolution;
    vec2 flowerUV = scale * (2.0f * fragCoord - resolution.xy) / resolution.y;

    //initial kaleidoscope folding
    flowerUV *= rotate(time / 20.0f);
    flowerUV = kaleidoscope(flowerUV, vec2(6.97f), count);
    flowerUV *= rotate(time / 5.0f);

    //flower generation
    vec4 flowerColor = vec4(0.0f);
    float orbsCount = 20.0f;

    for (float i = 0.0f; i < 20.0f; i++) {
        //domain warping, swirls and pulls the UV based on sin/cos waves
        flowerUV.x += 0.57f * sin(0.3f  * flowerUV.y + time);
        flowerUV.y -= 0.63f * cos(0.53f * flowerUV.x + time);

        //orb movement paths
        float angleT = i * PI / orbsCount * 2.0f;
        float x = 4.02f * tan(angleT + time / 10.0f);
        float y = 4.02f * cos(angleT - time / 30.0f);
        vec2 position = vec2(x, y);

        //cosine color palette math to generate vivid rainbow colors
        vec3 color = cos(vec3(-2.0f, 0.0f, -1.0f) * PI * 2.0f / 3.0f + PI * (i / 5.37f)) * 0.5f + 0.5f;

        //accumulate additive colors
        flowerColor += orb(flowerUV, 1.39f, position, color, 1.37f);
    }

    //distort the original texture
    vec2 screenDistortion = (flowerUV * 0.005f) * distortion;
    vec2 distortedUV = texCoords + screenDistortion;

    //sample the original texture
    vec4 baseScene = texture(colorTex, distortedUV);

    //composite everything together
    vec3 finalColor = baseScene.rgb + (flowerColor.rgb * distortion);
    fragColor = vec4(finalColor, baseScene.a);
}