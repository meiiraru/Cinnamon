#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;
uniform vec2 textelSize;
uniform float radius;

void main() {
    vec4 c = texture(colorTex, texCoords);
    vec4 maxVal = c;

    for (float u = 0.0f; u <= radius; u ++) {
        for (float v = 0.0f; v <= radius; v++) {
            float weight = (((sqrt(u * u + v * v) / radius) > 1.0f) ? 0.0f : 1.0f);

            vec4 s0 = texture(colorTex, texCoords + vec2(-u * textelSize.x, -v * textelSize.y));
            vec4 s1 = texture(colorTex, texCoords + vec2( u * textelSize.x,  v * textelSize.y));
            vec4 s2 = texture(colorTex, texCoords + vec2(-u * textelSize.x,  v * textelSize.y));
            vec4 s3 = texture(colorTex, texCoords + vec2( u * textelSize.x, -v * textelSize.y));

            vec4 o0 = max(s0, s1);
            vec4 o1 = max(s2, s3);
            vec4 tempMax = max(o0, o1);
            maxVal = mix(maxVal, max(maxVal, tempMax), weight);
        }
    }

    fragColor = vec4(maxVal.rgb, 1.0f);
}