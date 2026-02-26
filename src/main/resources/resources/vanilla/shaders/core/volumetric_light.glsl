#type vertex
#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

out vec3 worldPos;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main() {
    vec4 world = model * vec4(aPosition, 1.0f);
    worldPos = world.xyz;
    gl_Position = projection * view * world;
}

#type fragment
#version 330 core

in vec3 worldPos;

out vec4 fragColor;

uniform sampler2D gDepth;
uniform mat4 invView;
uniform mat4 invProjection;
uniform vec3 camPos;

uniform int lightType;
uniform vec3 lightPos;
uniform vec3 lightDir;
uniform float radius;
uniform float height;

uniform float strength;
uniform vec3 color;

const int MAX_RAY_STEPS = 32;

vec3 getPosFromDepth(vec2 texCoords, float depth) {
    vec2 ndc = texCoords * 2.0f - 1.0f;
    vec4 clip = vec4(ndc, depth * 2.0f - 1.0f, 1.0f);
    vec4 viewSpace = invProjection * clip;
    viewSpace /= viewSpace.w;
    vec4 world = invView * viewSpace;
    return world.xyz;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898f, 78.233f))) * 43758.5453f);
}

float getSphereDensity(vec3 pos) {
    float dist = length(pos - lightPos);

    //outside sphere
    if (dist >= radius)
        return 0.0f;

    //normalize the distance within the sphere
    float t = dist / radius;

    //inner fade to create a halo effect at the center of the sphere
    float innerFade = smoothstep(0.0f, 1.0f, t);

    //outer fade to create a soft edge to the sphere
    float outerFade = 1.0f - smoothstep(0.0f, 0.9f, t);

    //combine fades
    return innerFade * outerFade;
}

float getSphereCameraFade() {
    float dist = length(camPos - lightPos);

    if (dist >= radius)
        return 1.0f;

    //fade based on how deep inside the sphere
    float depth = 1.0f - (dist / radius);
    return 1.0f - smoothstep(0.0f, 0.7f, depth);
}

float getConeDensity(vec3 pos) {
    vec3 toPoint = pos - lightPos;
    float alongAxis = dot(toPoint, lightDir);

    //outside cone height
    if (alongAxis < 0.0f || alongAxis > height)
        return 0.0f;

    //distance from axis
    vec3 axisPoint = lightPos + lightDir * alongAxis;
    float perpDist = length(pos - axisPoint);

    //get the radius at this height
    float coneRadius = (alongAxis / height) * radius;

    //outside cone
    if (perpDist >= coneRadius)
        return 0.0f;

    //normalize the position within cone
    float y = alongAxis / height;
    float x = perpDist / max(coneRadius, 0.001f);

    //steep radial falloff to keep brightness near the center
    float radialFade = pow(1.0f - x, 1.5f);

    //height fade
    float heightFade = 1.0f - smoothstep(0.1f, 1.0f, y);

    //tip fade
    float tipFade = smoothstep(0.0f, 0.1f, y);

    return radialFade * heightFade * tipFade;
}

float getConeCameraFade() {
    vec3 toPoint = camPos - lightPos;
    float alongAxis = dot(toPoint, lightDir);

    if (alongAxis < 0.0f || alongAxis > height)
        return 1.0f;

    vec3 axisPoint = lightPos + lightDir * alongAxis;
    float perpDist = length(camPos - axisPoint);
    float coneRadius = (alongAxis / height) * radius;

    if (perpDist >= coneRadius)
        return 1.0f;

    //fade based on how deep inside the cone
    float depth = 1.0f - (perpDist / coneRadius);
    return 1.0f - smoothstep(0.0f, 0.7f, depth);
}

void main() {
    //camera fade
    bool pointLight = lightType == 0;
    float cameraFade = pointLight ? getSphereCameraFade() : getConeCameraFade();
    if (cameraFade <= 0.001f)
        discard;

    //get scene depth for occlusion
    vec2 screenUV = gl_FragCoord.xy / vec2(textureSize(gDepth, 0));
    float sceneDepth = texture(gDepth, screenUV).r;
    vec3 scenePos = getPosFromDepth(screenUV, sceneDepth);
    float distToScene = length(scenePos - camPos);

    //setup ray marching from the camera
    vec3 rayDir = normalize(worldPos - camPos);
    float distToFrag = length(worldPos - camPos);
    float maxDist = min(distToFrag, distToScene);

    //setup step size to always use the same number of steps
    float stepSize = maxDist / float(MAX_RAY_STEPS);

    //add some dithering to reduce artifacts from the ray marching
    float dither = hash(gl_FragCoord.xy) * stepSize;

    //ray march
    float totalDensity = 0.0f;
    for (int i = 0; i < MAX_RAY_STEPS; i++) {
        float t = dither + stepSize * i;
        vec3 samplePos = camPos + rayDir * t;
        float density = pointLight ? getSphereDensity(samplePos) : getConeDensity(samplePos);
        totalDensity += density * stepSize;
    }

    //normalize the intensity to keep brightness at any distance
    float intensity = (totalDensity / radius) * strength * cameraFade;
    intensity = clamp(intensity, 0.0f, 1.0f);

    fragColor = vec4(color * intensity, intensity);
}
