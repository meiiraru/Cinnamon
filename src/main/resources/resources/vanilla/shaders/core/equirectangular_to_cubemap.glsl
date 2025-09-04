#type vertex
#version 330 core
layout (location = 0) in vec3 aPos;

out vec3 pos;

uniform mat4 projection;
uniform mat4 view;

void main() {
    pos = aPos;
    gl_Position = projection * view * vec4(aPos, 1.0f);
}

#type fragment
#version 330 core

in vec3 pos;

out vec4 fragColor;

uniform sampler2D equirectangularMap;
uniform bool hdr;

const vec2 invAtan = vec2(0.1591f, 0.3183f);
const float gamma = 2.2f;
const float exposure = 1.5f;

vec2 sampleSphericalMap(vec3 v) {
    vec2 uv = vec2(atan(v.z, v.x), asin(v.y));
    uv *= invAtan;
    uv += 0.5f;
    return uv;
}

void main() {
    vec2 uv = sampleSphericalMap(normalize(pos));
    vec4 tex = texture(equirectangularMap, uv);
    vec3 color = tex.rgb;
    if (hdr) {
        color = vec3(1.0f) - exp(-color * exposure);
        color = pow(color, vec3(1.0f / gamma));
        color = clamp(color, 0.0f, 1.0f);
    }
    fragColor = vec4(color, tex.a);
}