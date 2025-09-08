#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D gEmissiveTex;
uniform float threshold = 1.0f;

void main() {
    //isolate the bright parts of the image
    vec3 hdrColor = texture(colorTex, texCoords).rgb;
    vec3 brightReflections = max(hdrColor - threshold, 0.0f);

    //add emissive color
    vec3 emissiveColor = texture(gEmissiveTex, texCoords).rgb;
    vec3 bloomSource = brightReflections + emissiveColor;

    fragColor = vec4(bloomSource, 1.0f);
}