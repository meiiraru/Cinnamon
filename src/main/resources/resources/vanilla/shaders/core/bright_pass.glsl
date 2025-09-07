#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D emissiveTex;
uniform float threshold = 1.0f;
uniform vec3 luminance = vec3(0.2126f, 0.7152f, 0.0722f);


void main() {
    vec4 emissive = texture(emissiveTex, texCoords);

    //if the emissive is present, use it directly
    if (emissive.r + emissive.g + emissive.b > 0.01f) {
        fragColor = emissive;
        return;
    }

    vec4 color = texture(colorTex, texCoords);
    color += emissive; //add emissive to the color

    //calculate the brightness of the pixel
    float brightness = color.r * luminance.r + color.g * luminance.g + color.b * luminance.b;

    //if the brightness is above the threshold, keep the color, otherwise set it to black
    fragColor = brightness > threshold ? color : vec4(0.0f);
}