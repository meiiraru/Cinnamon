#type vertex
#version 330 core

layout (location = 0) in vec2 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec2 texCoords;

uniform mat4 projection;
uniform mat4 view;

uniform vec3 lightPosition;
uniform float glareSize = 1.0f;
uniform float aspectRatio;

void main() {
    vec4 centerClipPos = projection * view * vec4(lightPosition, 1.0f);

    vec2 offset = aPosition * (glareSize / centerClipPos.w);
    offset.x /= aspectRatio;

    gl_Position = centerClipPos + vec4(offset * centerClipPos.w, 0.0f, 0.0f);
    texCoords = aTexCoords;
}

#type fragment
#version 330 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform sampler2D gDepth;

uniform mat4 view;
uniform mat4 projection;

uniform vec3 lightPosition;
uniform vec3 color = vec3(1.0f);

uniform vec2 sampleRadius;

uniform float intensity = 1.0f;

void main() {
    //project light pos to screen space
    vec4 lightClipPos = projection * view * vec4(lightPosition, 1.0f);

    //screen depth test
    if (lightClipPos.w <= 0.0f)
        discard;

    vec3 lightNDC = lightClipPos.xyz / lightClipPos.w;
    vec2 lightUV = lightNDC.xy * 0.5f + 0.5f; //[0, 1] range

    //occlusion test
    float lightDepth = lightNDC.z * 0.5f + 0.5f; //[0, 1] range
    float visibility = 0.0f;

    //sample grid
    const int r = 1;
    for (int x = -r; x <= r; x++) {
        for (int y = -r; y <= r; y++) {
            vec2 offset = vec2(float(x), float(y)) * sampleRadius;
            vec2 uv = lightUV + offset;
            if (uv.x < 0.0f || uv.x > 1.0f || uv.y < 0.0f || uv.y > 1.0f)
                continue;

            float sampleDepth = texture(gDepth, uv).r;
            if (sampleDepth >= lightDepth)
                visibility += 1.0f;
        }
    }

    visibility /= 9.0f;
    if (visibility <= 0.0f)
        discard;

    //render light
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a <= 0.01f)
        discard;

    fragColor = vec4(tex.rgb * color, tex.a) * visibility * intensity;
}