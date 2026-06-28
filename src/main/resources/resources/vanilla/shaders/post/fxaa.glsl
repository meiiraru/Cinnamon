#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;

const float FXAA_SPAN_MAX = 8.0f;
const float FXAA_REDUCE_MUL = 1.0f / 8.0f;
const float FXAA_REDUCE_MIN = 1.0f / 128.0;

const vec3 LUMA_WEIGHT = vec3(0.299f, 0.587f, 0.114f);

void main() {
    //sample the center and the diagonal neighbors
    vec3 rgbNW = texture(colorTex, texCoords + vec2(-1.0f, -1.0f) * texelSize).rgb;
    vec3 rgbNE = texture(colorTex, texCoords + vec2( 1.0f, -1.0f) * texelSize).rgb;
    vec3 rgbSW = texture(colorTex, texCoords + vec2(-1.0f,  1.0f) * texelSize).rgb;
    vec3 rgbSE = texture(colorTex, texCoords + vec2( 1.0f,  1.0f) * texelSize).rgb;
    vec4 texM  = texture(colorTex, texCoords); //keep center pixel alpha
    vec3 rgbM  = texM.rgb;

    //calculate the luma (brightness) of each pixel
    float lumaNW = dot(rgbNW, LUMA_WEIGHT);
    float lumaNE = dot(rgbNE, LUMA_WEIGHT);
    float lumaSW = dot(rgbSW, LUMA_WEIGHT);
    float lumaSE = dot(rgbSE, LUMA_WEIGHT);
    float lumaM  = dot(rgbM,  LUMA_WEIGHT);

    //find contrast from min and max luma
    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

    //calculate the direction of the edge
    vec2 dir;
    dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
    dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));

    float dirReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * (0.25f * FXAA_REDUCE_MUL), FXAA_REDUCE_MIN);
    float rcpDirMin = 1.0f / (min(abs(dir.x), abs(dir.y)) + dirReduce);

    //clamp the direction vector
    dir = min(vec2(FXAA_SPAN_MAX,  FXAA_SPAN_MAX), max(vec2(-FXAA_SPAN_MAX, -FXAA_SPAN_MAX), dir * rcpDirMin)) * texelSize;

    //sample along the edge
    //first blur sample (closer to center)
    vec3 rgbA = (1.0f / 2.0f) * (
        texture(colorTex, texCoords + dir * (1.0f / 3.0f - 0.5f)).rgb +
        texture(colorTex, texCoords + dir * (2.0f / 3.0f - 0.5f)).rgb
    );

    //second blur sample (away from center)
    vec3 rgbB = rgbA * (1.0f / 2.0f) + (1.0f / 4.0f) * (
        texture(colorTex, texCoords + dir * (0.0f / 3.0f - 0.5f)).rgb +
        texture(colorTex, texCoords + dir * (3.0f / 3.0f - 0.5f)).rgb
    );

    //check if the second blur is out of bounds with the local contrast
    float lumaB = dot(rgbB, LUMA_WEIGHT);

    if (lumaB < lumaMin || lumaB > lumaMax) {
        //out of bounds, use the closer blend
        fragColor = vec4(rgbA, texM.a);
    } else {
        //in bounds, use the further blend
        fragColor = vec4(rgbB, texM.a);
    }
}