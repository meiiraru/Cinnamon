#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main() {
    gl_Position = projection * view * model * vec4(aPosition, 1.0f);
}

#type fragment
#version 330 core

layout (location = 0) out vec4 gAlbedo;

uniform sampler2D gDepth;
uniform sampler2D textureSampler;

uniform mat4 invView;
uniform mat4 invProjection;
uniform mat4 invModel;

uniform vec4 color = vec4(1.0f);
uniform float opacity = 1.0f;

vec4 getPosFromDepth(vec2 texCoords) {
    vec2 ndc = texCoords * 2.0f - 1.0f;
    float depth = texture(gDepth, texCoords).r;

    vec4 clip = vec4(ndc, depth * 2.0f - 1.0f, 1.0f);
    vec4 view = invProjection * clip;
    view /= view.w;

    vec4 world = invView * view;
    return world;
}

void main() {
    //transform world position to decal local space
    vec2 texCoords = gl_FragCoord.xy / vec2(textureSize(gDepth, 0));
    vec4 localPos = invModel * getPosFromDepth(texCoords);

    //check if the fragment is inside the decal box
    if (abs(localPos.x) > 0.5f || abs(localPos.y) > 0.5f || abs(localPos.z) > 0.5f)
        discard;

    //map local position to texture coordinates
    vec2 uv = localPos.xy + 0.5f;
    uv.y = 1.0f - uv.y; //flip y axis

    //sample texture
    vec4 tex = texture(textureSampler, uv);
    if (tex.a < 0.5f)
        discard;

    tex.a *= opacity; //apply overall opacity
    gAlbedo = tex * color;
}
