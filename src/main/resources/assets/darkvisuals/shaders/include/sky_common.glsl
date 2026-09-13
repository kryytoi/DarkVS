// Shared helpers for DarkVisuals sky shaders.
// Provides noise, fbm and view-ray reconstruction from camera uniforms.

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
               mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;
        value += amplitude * noise2D(p);
        p = p * 2.03 + vec2(17.13, 9.71);
        amplitude *= 0.5;
    }
    return value;
}

// Rebuilds the world-space view ray for a fullscreen quad fragment.
// uCameraDir = (yawRad, pitchRad), uFov = vertical fov in degrees.
vec3 skyViewRay(vec2 screenPos) {
    float yaw = uCameraDir.x;
    float pitch = uCameraDir.y;
    float cp = cos(pitch);
    vec3 forward = vec3(sin(yaw) * cp, -sin(pitch), cos(yaw) * cp);
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    vec3 right = normalize(cross(forward, worldUp));
    vec3 up = cross(right, forward);
    float aspect = uResolution.x / max(uResolution.y, 1.0);
    float tanHalf = tan(radians(uFov) * 0.5);
    return normalize(forward + right * screenPos.x * tanHalf * aspect + up * screenPos.y * tanHalf);
}

// 3D value noise on a ray direction (projected onto a stable 2D domain)
float fbmDir(vec3 dir, float scale, float t, int octaves) {
    return fbm(vec2(atan(dir.z, dir.x) * scale + t, dir.y * scale * 1.7 - t * 0.6), octaves);
}

// Star field: dense hash grid on the direction sphere
float skyStars(vec3 dir, float density, float t) {
    vec2 sph = vec2(atan(dir.z, dir.x) * 40.0, dir.y * 80.0) * density;
    vec2 cell = floor(sph);
    vec2 f = fract(sph);
    float star = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 o = vec2(float(x), float(y));
            vec2 cellId = cell + o;
            float rnd = hash12(cellId);
            if (rnd > 0.82) {
                vec2 center = o + vec2(hash12(cellId + 7.3), hash12(cellId + 3.1)) - f;
                float dist = length(center);
                float twinkle = 0.5 + 0.5 * sin(t * (1.5 + rnd * 4.0) + rnd * 40.0);
                star += smoothstep(0.14, 0.0, dist) * twinkle * (rnd - 0.82) / 0.18;
            }
        }
    }
    return star;
}
