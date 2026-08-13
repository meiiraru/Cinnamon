
//primitives / shapes

float sdSphere(vec3 p, float r) {
    return length(p) - r;
}

float sdBox(vec3 p, vec3 b) {
    vec3 q = abs(p) - b;
    return length(max(q, 0.0f)) + min(max(q.x, max(q.y, q.z)), 0.0f);
}

float sdRoundBox(vec3 p, vec3 b, float r) {
    vec3 q = abs(p) - b + r;
    return length(max(q, 0.0f)) + min(max(q.x, max(q.y, q.z)), 0.0f) - r;
}

float sdBoxFrame(vec3 p, vec3 b, float e) {
    p = abs(p) - b;
    vec3 q = abs(p + e) - e;
    return min(min(
            length(max(vec3(p.x, q.y, q.z), 0.0f)) + min(max(p.x, max(q.y, q.z)), 0.0f),
            length(max(vec3(q.x, p.y, q.z), 0.0f)) + min(max(q.x, max(p.y, q.z)), 0.0f)),
            length(max(vec3(q.x, q.y, p.z), 0.0f)) + min(max(q.x, max(q.y, p.z)), 0.0f)
    );
}

float sdTorus(vec3 p, vec2 t) {
    vec2 q = vec2(length(p.xz) - t.x, p.y);
    return length(q) - t.y;
}

float sdLink(vec3 p, float le, float r1, float r2) {
    vec3 q = vec3(p.x, max(abs(p.y) - le, 0.0f), p.z);
    return length(vec2(length(q.xy) - r1, q.z)) - r2;
}

float sdCone(vec3 p, vec2 c, float h) {
    vec2 q = h * vec2(c.x / c.y, -1.0f);
    vec2 w = vec2(length(p.xz), p.y);
    vec2 a = w - q * clamp(dot(w, q) / dot(q, q), 0.0f, 1.0f);
    vec2 b = w - q * vec2(clamp(w.x / q.x, 0.0f, 1.0f), 1.0f);
    float k = sign(q.y);
    float d = min(dot(a, a),dot(b, b));
    float s = max(k * (w.x * q.y - w.y * q.x), k * (w.y - q.y));
    return sqrt(d) * sign(s);
}

float sdPlane(vec3 p, vec3 n, float h) {
    return dot(p, n) - h;
}

float sdCapsule(vec3 p, vec3 a, vec3 b, float r) {
    vec3 pa = p - a;
    vec3 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0f, 1.0f);
    return length(pa - ba * h) - r;
}

float sdCylinder(vec3 p, float r, float h) {
    vec2 d = abs(vec2(length(p.xz), p.y)) - vec2(r, h);
    return min(max(d.x, d.y), 0.0f) + length(max(d, 0.0f));
}

float sdRoundedCylinder(vec3 p, float ra, float rb, float h) {
    vec2 d = vec2(length(p.xz) - ra + rb, abs(p.y) - h + rb);
    return min(max(d.x, d.y), 0.0f) + length(max(d, 0.0f)) - rb;
}

float sdPyramid(vec3 p, float h) {
    float m2 = h * h + 0.25f;

    p.xz = abs(p.xz);
    p.xz = (p.z > p.x) ? p.zx : p.xz;
    p.xz -= 0.5f;

    vec3 q = vec3(p.z, h * p.y - 0.5f * p.x, h * p.x + 0.5f * p.y);
    float s = max(-q.x, 0.0f);
    float t = clamp((q.y - 0.5f * p.z) / (m2 + 0.25f), 0.0f, 1.0f);
    float a = m2 * (q.x + s) * (q.x + s) + q.y * q.y;
    float b = m2 * (q.x + 0.5f * t) * (q.x + 0.5f * t) + (q.y - m2 * t) * (q.y - m2 * t);

    float d2 = min(q.y, -q.x * m2 - q.y * 0.5f) > 0.0f ? 0.0f : min(a, b);
    return sqrt((d2 + q.z * q.z) / m2 ) * sign(max(q.z, -p.y));
}

//distance operations

float opUnion(float a, float b) {
    return min(a, b);
}

float opSubtraction(float a, float b) {
    return max(-a, b);
}

float opIntersection(float a, float b) {
    return max(a, b);
}

float opXor(float a, float b) {
    return max(min(a, b), - max(a, b));
}

float opRound(float d, float radius) {
    return d - radius;
}

float opOnion(float d, float thickness) {
    return abs(d) - thickness;
}

float opSmoothUnion(float a, float b, float k) {
    k *= 4.0f;
    float h = max(k - abs(a - b), 0.0f);
    return min(a, b) - h * h * 0.25f / k;
}

float opSmoothSubtraction(float a, float b, float k) {
    return -opSmoothUnion(a, -b, k);
}

float opSmoothIntersection(float a, float b, float k) {
    return -opSmoothUnion(-a, -b, k);
}

//transformations

vec3 opTranslate(vec3 p, vec3 offset) {
    return p - offset;
}

vec3 opRepeat(vec3 p, vec3 spacing) {
    return p - spacing * round(p / spacing);
}

vec3 opEnlogate(vec3 p, vec3 len) {
    return p - clamp(p, vec3(0.0f), len);
}
