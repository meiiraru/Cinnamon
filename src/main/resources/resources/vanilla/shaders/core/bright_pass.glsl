#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;
out vec4 fragColor;

uniform sampler2D colorTex;
uniform sampler2D emissiveTex;
uniform float threshold = 1.0f;

void main() {
    //isolate the bright parts of the image
    vec3 sceneColor = texture(colorTex, texCoords).rgb;
    vec3 emissiveColor = texture(emissiveTex, texCoords).rgb;

    //calculate the brightness (luminance) of the scene color
    //float brightness = dot(sceneColor, vec3(0.2126f, 0.7152f, 0.0722f));
    //vec3 brightColor = sceneColor * step(threshold, brightness);
    vec3 brightColor = max(sceneColor - threshold, 0.0f);

    //combine with the emissive color
    vec3 bloomSource = brightColor + emissiveColor;
    fragColor = vec4(bloomSource, 1.0f);
}