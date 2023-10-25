
struct Light {
    vec3 pos;
    vec3 color;
    float range;
};

uniform vec3 ambient;
uniform int lightCount;
uniform Light lights[4];

float calculateAttenuation(float range, float distance) {
    float linear = 4.5f / range;
    float quadratic = 75 / (range * range);
    return 1 / (1 + linear * distance + quadratic * (distance * distance));
}

vec4 calculateLight(vec3 pos, vec3 normal) {
    vec3 norm = normalize(normal);
    vec3 diffuse = vec3(0);

    for (int i = 0; i < lightCount; i++) {
        Light l = lights[i];

        vec3 diffDist = l.pos - pos;
        float attenuation = calculateAttenuation(l.range, length(diffDist));
        if (attenuation <= 0.01f)
            continue;

        // diffuse
        float diff = max(dot(norm, normalize(diffDist)), 0);
        diffuse += attenuation * l.color * diff;
    }

    //return the light
    return vec4(ambient + diffuse, 1);
}