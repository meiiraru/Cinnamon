#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex; //GL_RGBA
uniform usampler2D stencilTex; //GL_DEPTH24_STENCIL8
uniform vec2 resolution;

uniform int numSteps = 12;
uniform float radius = 3.0f;
uniform vec3 color = vec3(1.0f);

const float TAU = 6.28318530;

void main() {
    vec3 texCcolor = texture(colorTex, texCoords).rgb;
    uint stencil = texture(stencilTex, texCoords).r;

    vec2 aspect = 1.0f / resolution;
    float outlinemask = 0.0f;
    for (float i = 0.0f; i < TAU; i += TAU / numSteps) {
        vec2 offset = vec2(sin(i), cos(i)) * aspect * radius;
        float col = texture(stencilTex, texCoords + offset).r;
        outlinemask = mix(outlinemask, 1.0f, col);
    }
    outlinemask = mix(outlinemask, 0.0f, float(stencil));

    //vec4 final = vec4(vec3(texture(stencilTex, texCoords).r), 1.0f);
    vec4 final = mix(vec4(texCcolor, 1.0f), vec4(color, 1.0f), clamp(outlinemask, 0.0f, 1.0f));
    fragColor = final;
}