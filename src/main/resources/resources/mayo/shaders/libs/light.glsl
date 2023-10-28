
struct Light {
    vec3 pos;
    vec3 color;
    vec3 attenuation;
};

uniform vec3 ambient;
uniform int lightCount;
uniform Light lights[16];

float calculateAttenuation(vec3 attenuation, float distance) {
    return 1 / (attenuation.x + attenuation.y * distance + attenuation.z * (distance * distance));
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

        // diffuse
        float diff = max(dot(norm, normalize(diffDist)), 0);
        diffuse += attenuation * l.color * diff;
    }

    //return the light
    return vec4(ambient + diffuse, 1);
}