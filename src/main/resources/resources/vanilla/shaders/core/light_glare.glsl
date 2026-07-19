#type vertex
#version 430 core

layout (location = 0) in vec2 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec2 texCoords;

uniform mat4 projection;
uniform mat4 view;

uniform vec3 lightPosition;
uniform float glareSize = 1.0f;
uniform float aspectRatio;
uniform float fadeSpeed = 10.0f;

uniform sampler2D gDepth;
uniform vec2 sampleRadius;
uniform int lightIndex;
uniform float deltaTime;

layout(std430, binding = 0) coherent buffer VisibilityBuffer {
    float visibilities[];
};

void main() {
    vec4 centerClipPos = projection * view * vec4(lightPosition, 1.0f);

    vec2 offset = aPosition * (glareSize / centerClipPos.w);
    offset.x /= aspectRatio;

    gl_Position = centerClipPos + vec4(offset * centerClipPos.w, 0.0f, 0.0f);
    texCoords = aTexCoords;

    //update visibility in SSBO only once per light per frame
    if (gl_VertexID == 0) {
        float targetVisibility = 0.0f;

        if (centerClipPos.w > 0.0f) {
            vec3 lightNDC    = centerClipPos.xyz / centerClipPos.w;
            vec2 lightUV     = lightNDC.xy * 0.5f + 0.5f;
            float lightDepth = lightNDC.z  * 0.5f + 0.5f;

            //9-tap occlusion check
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    vec2 offset = vec2(float(x), float(y)) * sampleRadius;
                    vec2 uv = lightUV + offset;

                    if (uv.x >= 0.0f && uv.x <= 1.0f && uv.y >= 0.0f && uv.y <= 1.0f) {
                        float sampleDepth = textureLod(gDepth, uv, 0.0f).r;
                        if (sampleDepth >= lightDepth)
                            targetVisibility += 1.0f;
                    }
                }
            }
            targetVisibility /= 9.0f;
        }

        //read and smooth the visibility value in the SSBO
        float currentVis = visibilities[lightIndex];
        visibilities[lightIndex] = mix(currentVis, targetVisibility, clamp(fadeSpeed * deltaTime, 0.0f, 1.0f));
    }
}

#type fragment
#version 430 core

in vec2 texCoords;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec3 color = vec3(1.0f);
uniform float intensity = 1.0f;

uniform int lightIndex;
layout(std430, binding = 0) buffer VisibilityBuffer {
    float visibilities[];
};


void main() {
    //sample texture
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a < 0.01f)
        discard;

    //get visibility from SSBO
    float visibility = visibilities[lightIndex];
    if (visibility <= 0.01f)
        discard;

    fragColor = vec4(tex.rgb * color, 0.0f) * visibility * intensity;
}