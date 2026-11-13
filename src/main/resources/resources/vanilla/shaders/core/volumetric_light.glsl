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
uniform sampler2D noiseTex;

uniform mat4 invView;
uniform mat4 invProjection;
uniform vec3 camPos;
uniform vec2 screenSize;

uniform int lightType;
uniform vec3 lightPos;
uniform vec3 lightDir;
uniform float radius;
uniform float height;

uniform float strength;
uniform vec3 color;

uniform bool castsShadows;
uniform sampler2D shadowMap;
uniform samplerCube shadowCubeMap;
uniform sampler2D cookieMap;
uniform mat4 lightSpaceMatrix;
uniform float farPlane;

uniform int raySteps = 32;

vec3 getPosFromDepth(vec2 texCoords, float depth) {
    vec2 ndc = texCoords * 2.0f - 1.0f;
    vec4 clip = vec4(ndc, depth * 2.0f - 1.0f, 1.0f);
    vec4 viewSpace = invProjection * clip;
    viewSpace /= viewSpace.w;
    vec4 world = invView * viewSpace;
    return world.xyz;
}

bool intersectSphere(vec3 ro, vec3 rd, vec3 center, float r, out float t0, out float t1) {
    vec3 oc = ro - center;
    float b = dot(oc, rd);
    float c = dot(oc, oc) - r * r;
    float disc = b * b - c;

    if (disc <= 0.0f)
        return false;

    float s = sqrt(disc);
    t0 = -b - s;
    t1 = -b + s;
    return true;
}

bool intersectCone(vec3 ro, vec3 rd, vec3 apex, vec3 axis, float h, float r, out float t0, out float t1) {
    vec3 O = ro - apex;
    float ov = dot(O, axis);
    float dv = dot(rd, axis);

    //axial slab -> 0 <= P.axis <= h
    float tSlab0 = -1e20f, tSlab1 = 1e20f;
    if (abs(dv) > 1e-6f) {
        float ta = -ov / dv;
        float tb = (h - ov) / dv;
        tSlab0 = min(ta, tb);
        tSlab1 = max(ta, tb);
    } else if (ov < 0.0f || ov > h) {
        return false;
    }

    float k = (h * h) / (h * h + r * r);
    float a = dv * dv - k;
    float b = ov * dv - k * dot(O, rd);
    float c = ov * ov - k * dot(O, O);

    float tCone0 = -1e20f, tCone1 = 1e20f;
    if (abs(a) < 1e-5f) {
        if (abs(b) < 1e-6f)
            return false;

        float tEdge = -c / (2.0f * b);
        if (b > 0.0f) tCone0 = tEdge;
        else          tCone1 = tEdge;
    } else {
        float disc = b * b - a * c;
        if (a < 0.0f) {
            if (disc <= 0.0f)
                return false;

            float s = sqrt(disc);
            tCone0 = (-b + s) / a;
            tCone1 = (-b - s) / a;
        } else {
            float s = sqrt(max(disc, 0.0f));
            float tSmall = (-b - s) / a;
            float tLarge = (-b + s) / a;
            if (dv > 0.0f) tCone0 = tLarge;
            else           tCone1 = tSmall;
        }
    }

    t0 = max(tSlab0, tCone0);
    t1 = min(tSlab1, tCone1);
    return t1 > t0;
}

float getSphereDensity(vec3 pos) {
    vec3 diff = pos - lightPos;
    float distSq = dot(diff, diff);
    float radiusSq = radius * radius;

    //outside sphere
    if (distSq >= radiusSq)
        return 0.0f;

    //normalize the distance within the sphere
    float t = sqrt(distSq) / radius;

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
    bool pointLight = lightType == 0;
    bool cookieLight = lightType == 3;

    //camera fade
    float cameraFade = pointLight ? getSphereCameraFade() : getConeCameraFade();
    if (cameraFade <= 0.001f)
        discard;

    //get scene depth for occlusion
    vec2 screenUV = gl_FragCoord.xy / screenSize;
    float sceneDepth = texture(gDepth, screenUV).r;
    vec3 scenePos = getPosFromDepth(screenUV, sceneDepth);
    float distToScene = length(scenePos - camPos);

    //setup ray marching from the camera
    vec3 rayDir = normalize(worldPos - camPos);

    float t0, t1;
    bool hit = pointLight ? intersectSphere(camPos, rayDir, lightPos, radius, t0, t1) : intersectCone(camPos, rayDir, lightPos, normalize(lightDir), height, radius, t0, t1);

    float tStart = max(0.0f, t0);
    float tEnd = min(distToScene, t1);
    if (!hit || tEnd <= tStart)
        discard;

    //setup step size to always use the same number of steps
    float stepSize = (tEnd - tStart) / float(raySteps);

    //add some dithering to reduce artifacts from the ray marching
    ivec2 noiseSize = textureSize(noiseTex, 0);
    float dither = texelFetch(noiseTex, ivec2(gl_FragCoord.xy) % noiseSize, 0).r * stepSize;

    //spot/cookie lights can have the matrices out of the raymarch loop
    vec4 rayStartLS = vec4(0.0f);
    vec4 rayDirLS   = vec4(0.0f);

    if (!pointLight && (castsShadows || cookieLight)) {
        rayStartLS = lightSpaceMatrix * vec4(camPos, 1.0f);
        rayDirLS   = lightSpaceMatrix * vec4(rayDir, 0.0f);
    }

    //ray march
    vec3 totalDensity = vec3(0.0f);
    for (int i = 0; i < raySteps; i++) {
        float t = tStart + dither + stepSize * float(i);
        vec3 samplePos = camPos + rayDir * t;
        float density = pointLight ? getSphereDensity(samplePos) : getConeDensity(samplePos);

        if (density <= 0.0f)
            continue;

        //apply density contribution
        vec3 sampleColor = vec3(density);
        if (pointLight) {
            if (castsShadows) {
                vec3 fragToLight = samplePos - lightPos;
                float currentDepth = length(fragToLight);
                float closest = texture(shadowCubeMap, fragToLight).r * farPlane;
                sampleColor *= currentDepth > closest ? 0.0f : 1.0f;
            }
        } else if (castsShadows || cookieLight) {
            vec4 ls = rayStartLS + rayDirLS * t;
            vec3 coords = (ls.xyz / ls.w) * 0.5f + 0.5f;

            if (coords.z > 1.0f || coords.x < 0.0f || coords.x > 1.0f || coords.y < 0.0f || coords.y > 1.0f)
                continue;

            if (castsShadows) {
                float closest = texture(shadowMap, coords.xy).r;
                if (coords.z > closest) //in shadow
                    continue;
            }

            if (cookieLight) {
                vec2 cookieCoords = vec2(coords.x, 1.0f - coords.y);
                sampleColor *= texture(cookieMap, cookieCoords).rgb;
            }
        }

        totalDensity += sampleColor * stepSize;
    }

    //normalize the intensity to keep brightness at any distance
    vec3 intensity = clamp((totalDensity / radius) * strength * cameraFade, 0.0f, 1.0f);
    float alpha = max(intensity.r, max(intensity.g, intensity.b));

    fragColor = vec4(color * intensity, alpha);
}
