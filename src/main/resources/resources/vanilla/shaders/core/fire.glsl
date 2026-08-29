#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;

out vec3 pos;
out vec2 texCoords;
out vec3 normal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat3 normalMat;

void main() {
    vec4 worldPos = model * vec4(aPosition, 1.0f);
    gl_Position = projection * view * worldPos;
    pos = worldPos.xyz;
    texCoords = aTexCoords;
    normal = normalize(normalMat * aNormal);
}

#type fragment
#version 330 core

in vec3 pos;
in vec2 texCoords;
in vec3 normal;

layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gORM;
layout (location = 3) out vec4 gEmissive;

uniform sampler2D noiseTex;
uniform float time;

uniform vec2 offset1 = vec2(1.5f, 1.0f);
uniform vec2 offset2 = vec2(2.5f, 2.0f);
uniform float speed1 = 1.2f;
uniform float speed2 = 1.8f;

uniform vec2 mask = vec2(0.35f, 0.4f);

uniform float brightness = 1.5f;
uniform float coreIntensity = 0.25f;
uniform vec4 color = vec4(1.0f, 0.35f, 0.0f, 1.0f);
uniform vec4 coreColor = vec4(1.0f);

void main() {
    vec2 uv = texCoords;
    uv.y = 1.0f - uv.y;

    //uv distortion
    float distortionNoise = texture(noiseTex, uv * 0.5f + vec2(0.0f, -time * 0.4f)).r;
    vec2 warpedUV = uv;
    warpedUV.x += (distortionNoise - 0.5f) * 0.3f * uv.y;

    //multiplicative wisps
    vec2 uv1 = warpedUV * offset1 + vec2(0.0f, -time * speed1);
    vec2 uv2 = warpedUV * offset2 + vec2(0.0f, -time * speed2);
    float n1 = texture(noiseTex, uv1).r;
    float n2 = texture(noiseTex, uv2).r;

    float fireNoise = n1 * n2;

    //procedural flame mask
    float width = mix(0.5f, mask.x, uv.y);
    float distFromCenter = abs(warpedUV.x - 0.5f);

    float finalMask = smoothstep(width, width - mask.x, distFromCenter);
    finalMask *= smoothstep(1.0f, mask.y, uv.y);
    finalMask *= smoothstep(0.0f, 0.05f, uv.y);

    //erosion
    float fire = finalMask * fireNoise * brightness;
    float alpha = smoothstep(0.1f, 0.4f, fire);
    if (alpha < 0.99f)
    discard;

    //color
    float heat = smoothstep(coreIntensity, 1.0f - coreIntensity, fire);
    float coreMask = pow(heat, 5.0f);
    vec3 finalColor = mix(color.rgb, coreColor.rgb, coreMask);

    //output to gbuffer
    gAlbedo   = vec4(0.0f, 0.0f, 0.0f, 1.0f);
    gNormal   = vec4(normal, 1.0f);
    gORM      = vec4(1.0f, 1.0f, 0.0f, 1.0f);
    gEmissive = vec4(finalColor, 1.0f);
}