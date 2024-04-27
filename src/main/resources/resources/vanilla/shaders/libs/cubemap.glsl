
uniform samplerCube cubemap;
uniform mat3 cubemapRotation;

vec3 getCubemapColor(vec3 pos, vec3 normal, vec3 cameraPos) {
    vec3 I = normalize(pos - cameraPos);
    vec3 R = reflect(I, normalize(normal));
    return texture(cubemap, R * cubemapRotation).rgb;
}