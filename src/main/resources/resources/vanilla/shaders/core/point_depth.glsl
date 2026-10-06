#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoords;

out vec3 v_worldPos;
out vec2 v_texCoords;

uniform mat4 model;

void main() {
    vec4 pos = model * vec4(aPosition, 1.0f);
    gl_Position = pos;
    v_worldPos = pos.xyz;
    v_texCoords = aTexCoords;
}

#type geometry
#version 400 core

layout(triangles, invocations = 6) in;
layout(triangle_strip, max_vertices = 3) out;

in vec3 v_worldPos[];
in vec2 v_texCoords[];

out vec3 worldPos;
out vec2 texCoords;
uniform mat4 shadowMatrices[6];

void main() {
    for (int i = 0; i < 3; i++) {
        gl_Position = shadowMatrices[gl_InvocationID] * gl_in[i].gl_Position;
        worldPos = v_worldPos[i];
        texCoords = v_texCoords[i];
        gl_Layer = gl_InvocationID;
        EmitVertex();
    }
    EndPrimitive();
}

#type fragment
#version 330 core

in vec3 worldPos;
in vec2 texCoords;

uniform sampler2D textureSampler;
uniform vec3 lightPos;
uniform float farPlane;

void main() {
    vec4 tex = texture(textureSampler, texCoords);
    if (tex.a < 0.01f)
        discard;

    //compute face normal from screen-space derivatives of world position
    vec3 faceNormal = normalize(cross(dFdx(worldPos), dFdy(worldPos)));
    vec3 lightDir = normalize(lightPos - worldPos);

    //slope-scale bias
    float cosAngle = abs(dot(faceNormal, lightDir));
    float bias = 0.002f * sqrt(1.0f - cosAngle * cosAngle) / max(cosAngle, 0.001f);
    bias = min(bias, 0.01f);

    float lightDistance = length(worldPos - lightPos);
    lightDistance = lightDistance / farPlane;
    gl_FragDepth = lightDistance + bias;
}