#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 texelSize;

//5x5 box kernel
uniform float kernel[] = float[](
    1.0f / 273.0f,  4.0f / 273.0f,  7.0f / 273.0f,  4.0f / 273.0f, 1.0f / 273.0f,
    4.0f / 273.0f, 16.0f / 273.0f, 26.0f / 273.0f, 16.0f / 273.0f, 4.0f / 273.0f,
    7.0f / 273.0f, 26.0f / 273.0f, 41.0f / 273.0f, 26.0f / 273.0f, 7.0f / 273.0f,
    4.0f / 273.0f, 16.0f / 273.0f, 26.0f / 273.0f, 16.0f / 273.0f, 4.0f / 273.0f,
    1.0f / 273.0f,  4.0f / 273.0f,  7.0f / 273.0f,  4.0f / 273.0f, 1.0f / 273.0f
);

uniform float focusCenter = 0.5f;
uniform float focusInner = 0.05f;
uniform float focusOuter = 0.25f;
uniform float blurRadius = 2.5f;

void main() {
    vec2 uv = texCoords;
    float distanceFromCenter = abs(uv.y - focusCenter);
    float blurAmount = smoothstep(focusInner, focusOuter, distanceFromCenter);

    if (blurAmount <= 0.001f) {
        fragColor = texture(colorTex, uv);
        return;
    }

    float radius = blurAmount * blurAmount * blurRadius;
    vec3 color = vec3(0.0f);

    for (int y = -2; y <= 2; y++) {
        for (int x = -2; x <= 2; x++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize * radius;
            color += texture(colorTex, uv + offset).rgb * kernel[(y + 2) * 5 + (x + 2)];
        }
    }

    fragColor = vec4(color, 1.0f);
}