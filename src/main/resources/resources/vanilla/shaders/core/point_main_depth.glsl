#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in int aTexID;
layout (location = 2) in vec2 aTexCoords;

flat out int texID;
out vec3 v_worldPos;
out vec2 v_texCoords;

void main() {
    vec4 pos = vec4(aPosition, 1.0f);
    gl_Position = pos;
    texID = aTexID;
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

flat in int texID;
in vec3 worldPos;
in vec2 texCoords;

uniform sampler2D textures[16];
uniform vec3 lightPos;
uniform float farPlane;

void main() {
    if (texID >= 0) {
        vec4 tex = vec4(0.0f);
        switch(int(texID)) {
            case  0: tex = texture(textures[0],  texCoords); break;
            case  1: tex = texture(textures[1],  texCoords); break;
            case  2: tex = texture(textures[2],  texCoords); break;
            case  3: tex = texture(textures[3],  texCoords); break;
            case  4: tex = texture(textures[4],  texCoords); break;
            case  5: tex = texture(textures[5],  texCoords); break;
            case  6: tex = texture(textures[6],  texCoords); break;
            case  7: tex = texture(textures[7],  texCoords); break;
            case  8: tex = texture(textures[8],  texCoords); break;
            case  9: tex = texture(textures[9],  texCoords); break;
            case 10: tex = texture(textures[10], texCoords); break;
            case 11: tex = texture(textures[11], texCoords); break;
            case 12: tex = texture(textures[12], texCoords); break;
            case 13: tex = texture(textures[13], texCoords); break;
            case 14: tex = texture(textures[14], texCoords); break;
            case 15: tex = texture(textures[15], texCoords); break;
        }

        if (tex.a < 0.01f)
            discard;
    }

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