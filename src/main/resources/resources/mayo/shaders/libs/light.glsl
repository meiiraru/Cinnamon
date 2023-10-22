
uniform vec3 ambientLight;
uniform vec3 lightPos;

vec4 calculateLighting(vec4 color) {
    //ambient
    vec3 ambient = ambientLight;

    //apply lighting
    return color * vec4(ambient, 1);
}