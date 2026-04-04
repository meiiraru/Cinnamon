#type vertex
#include shaders/libs/blit.vsh

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform float time;
uniform vec2 resolution;
uniform vec2 framebufferOffset;

//voronoi gradient
//by gls9102 - https://www.shadertoy.com/view/WdlyRS

uniform float size = 30.0f;
uniform vec3 col1 = vec3(15.0f,  8.0f,  5.0f) / 255.0f;
uniform vec3 col2 = vec3(46.0f, 23.0f, 15.0f) / 255.0f;

vec2 ran(vec2 uv) {
    uv *= vec2(dot(uv, vec2(127.1f, 311.7f)), dot(uv, vec2(227.1f, 521.7f)));
    return 1.0f - fract(tan(cos(uv) * 123.6f) * 3533.3f) * fract(tan(cos(uv) * 123.6f) * 3533.3f);
}

vec2 pt(vec2 id) {
    return sin(time * (ran(id + 0.5f) - 0.5f) + ran(id - 20.1f) * 8.0f) * 0.5f;
}

void main() {
    vec2 uv = texCoords - 0.5f + framebufferOffset / resolution;
    uv.y *= resolution.y / resolution.x;

    vec2 off = time / vec2(50.0f, 30.0f);
    uv += off;
    uv *= size;

    vec2 gv = fract(uv) - 0.5f;
    vec2 id = floor(uv);

    float mindist = 1e9f;
    vec2 vorv;
    for (float i = -1.0f; i <= 1.0f; i++) {
        for (float j= -1.0f; j <= 1.0f; j++) {
            vec2 offv = vec2(i, j);
            float dist = length(gv + pt(id + offv) - offv);
            if (dist < mindist) {
                mindist = dist;
                vorv = (id + pt(id + offv) + offv) / size - off;
            }
        }
    }

    vec3 col = mix(col1, col2, clamp(vorv.x * 2.2f + vorv.y, -1.0f, 1.0f) * 0.5f + 0.5f);
    fragColor = vec4(col, 1.0f);

    //fragColor += vec4(vec3(smoothstep(0.08f, 0.05f, gv.x + pt(id).x)), 0.0f);
    //fragColor -= vec4(vec3(smoothstep(0.05f, 0.03f, gv.x + pt(id).x)), 0.0f);
}
