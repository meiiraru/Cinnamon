#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D colorTex;

uniform float brightness = 1.0f;
uniform float contrast = 1.0f;
uniform float gamma = 1.0f;
uniform float saturation = 1.0f;

void main() {
    //sample color
    vec4 color = texture(colorTex, texCoords);

    //apply brightness
    color.rgb *= brightness;

    //contrast
    color.rgb = (color.rgb - 0.5f) * contrast + 0.5f;

    //saturation
    float luma = dot(color.rgb, vec3(0.2125f, 0.7154f, 0.0721f));
    color.rgb = mix(vec3(luma), color.rgb, saturation);

    //gamma
    color.rgb = max(color.rgb, vec3(0.0f));
    color.rgb = pow(color.rgb, vec3(1.0f / gamma));

    fragColor = color;
}