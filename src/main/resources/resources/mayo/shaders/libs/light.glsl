
struct Light {
    vec3 pos;
    vec3 color;
    vec3 attenuation;

    bool directional;
    vec3 dir;
    float cutOff;
    float outerCutOff;
};

uniform vec3 ambient;
uniform int lightCount;
uniform Light lights[16];

float calculateAttenuation(vec3 attenuation, float distance) {
    return 1 / (attenuation.x + attenuation.y * distance + attenuation.z * (distance * distance));
}

float spotlightIntensity(Light light, vec3 lightDir) {
    float theta = dot(lightDir, normalize(-light.dir));
    float epsilon = (light.cutOff - light.outerCutOff);
    return clamp((theta - light.outerCutOff) / epsilon, 0, 1);
}

vec4 calculateLight(vec3 pos, vec3 normal) {
    vec3 norm = normalize(normal);
    vec3 diffuse = vec3(0);

    for (int i = 0; i < min(lightCount, 16); i++) {
        Light l = lights[i];

        vec3 diffDist = l.pos - pos;
        float attenuation = calculateAttenuation(l.attenuation, length(diffDist));
        if (attenuation <= 0.01f)
            continue;

        vec3 lightDir = normalize(diffDist);

        //diffuse
        float d = max(dot(norm, lightDir), 0);
        vec3 diff = attenuation * l.color * d;

        //spotlight
        if (l.directional) {
            float intensity = spotlightIntensity(l, lightDir);
            diff *= intensity;
        }

        diffuse += diff;
    }

    //return the light
    return vec4(ambient + diffuse, 1);
}