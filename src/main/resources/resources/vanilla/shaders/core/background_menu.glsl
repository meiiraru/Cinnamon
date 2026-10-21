#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform float time;
uniform vec2 resolution;
uniform vec2 framebufferOffset;
uniform float guiScale;

//background
uniform vec3 topColor    = vec3(0.09f, 0.07f, 0.21f);
uniform vec3 bottomColor = vec3(0.24f, 0.24f, 0.41f);

//stars
uniform vec4 star1Color = vec4(1.0f,  0.94f, 0.72f, 0.7f);
uniform vec4 star2Color = vec4(0.18f, 0.03f, 0.41f, 0.7f);
uniform vec4 star3Color = vec4(0.63f, 0.5f,  0.81f, 0.7f);
uniform float starGrid = 40.0f;
uniform float starSize = 0.15f;
uniform vec2 parallaxSpeed = vec2(0.0f, 3.0f);

//random 2D vector
vec2 randVector(vec2 v, float seed) {
    return vec2(
            fract(sin(v.x * 999.9f + v.y) * seed),
            fract(sin(v.y * 999.9f + v.x) * seed)
    );
}

//star layer
vec4 drawStars(vec4 color, vec4 starColor, vec2 uv, float grid, float size, vec2 speed, float seed) {
    uv += time * speed;

    //local coordinates within the grid cell
    vec2 local = mod(uv, grid) / grid;

    //random position for this cell
    vec2 randv = randVector(floor(uv / grid), seed) - 0.5f;
    float len = length(randv);

    //only draw stars whose randomized centers are inside the cell
    if (len < 0.5f) {
        float radius = 1.0f - distance(local, vec2(0.5f, 0.5f) + randv) / (size * (0.5f - len));
        if (radius > 0.0f)
            color += starColor * radius;
    }

    return color;
}

void main() {
    //background gradient
    vec4 col = vec4(mix(bottomColor, topColor, texCoords.y), 1.0f);

    //parallax star layers
    vec2 fragCoord = texCoords * resolution * guiScale + framebufferOffset;
    col = drawStars(col, star1Color, fragCoord, starGrid,               starSize,               parallaxSpeed,        123456.789f);
    col = drawStars(col, star2Color, fragCoord, starGrid * 2.0f / 3.0f, starSize,               parallaxSpeed / 1.2f, 345678.912f);
    col = drawStars(col, star3Color, fragCoord, starGrid / 2.0f,        starSize * 3.0f / 4.0f, parallaxSpeed / 1.6f, 567891.234f);

    fragColor = col;
}
