
struct Light {
    vec3 pos;
    vec3 diffuse;
};

uniform vec3 ambient;
uniform int lightCount;
uniform Light lights[4];

vec4 calculateLight(vec3 pos, vec3 normal) {
    vec3 norm = normalize(normal);
    vec3 diffuse = vec3(0);

    for (int i = 0; i < lightCount; i++) {
        Light l = lights[i];

        vec3 diffDist = l.pos - pos;
        float attenuation = 1 / length(diffDist);
        vec3 lightDir = normalize(diffDist);

        // diffuse
        float diff = max(dot(norm, lightDir), 0);
        diffuse += attenuation * l.diffuse * diff;
    }

    //return the light
    return vec4(ambient + diffuse, 1);
}