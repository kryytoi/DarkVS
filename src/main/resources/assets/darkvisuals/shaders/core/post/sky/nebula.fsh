#version 150

uniform float uTime;
uniform vec2 uResolution;
uniform vec3 uColor;
uniform float uAlpha;
uniform float uSpeed;
uniform float uScale;
uniform float uIntensity;
uniform vec2 uCameraDir;
uniform float uFov;

#moj_import <darkvisuals:sky_common.glsl>

in vec2 ScreenPos;
out vec4 OutColor;

void main() {
    vec3 dir = skyViewRay(ScreenPos);
    float t = uTime * uSpeed;

    // deep space nebula: domain-warped fbm clouds
    vec2 p = vec2(atan(dir.z, dir.x), asin(clamp(dir.y, -1.0, 1.0))) * uScale;
    vec2 q = vec2(fbm(p + vec2(0.0, t * 0.05), 4), fbm(p + vec2(5.2, 1.3) - t * 0.04, 4));
    vec2 r = vec2(fbm(p + q * 3.0 + vec2(1.7, 9.2) + t * 0.08, 4),
                  fbm(p + q * 3.0 + vec2(8.3, 2.8) - t * 0.06, 4));
    float f = fbm(p + r * 2.5, 5);
    f = smoothstep(0.3, 0.95, f);

    // nebula palette: theme color blended with magenta and gold
    vec3 colA = uColor * 0.9;
    vec3 colB = mix(vec3(0.8, 0.2, 0.65), uColor, 0.35);
    vec3 colC = mix(vec3(1.0, 0.75, 0.3), uColor, 0.55);
    vec3 nebula = mix(colA, colB, clamp(q.x * 1.4, 0.0, 1.0));
    nebula = mix(nebula, colC, clamp(r.y * 1.2, 0.0, 1.0) * 0.5);
    nebula *= f * 0.85;

    // bright core where the warp folds
    float core = smoothstep(0.75, 1.0, f) * (0.6 + 0.4 * sin(t * 0.8 + r.x * 10.0));
    nebula += core * mix(vec3(1.0), uColor, 0.3) * 0.6;

    // dense star field over the whole sky
    float stars = skyStars(dir, 1.0, t);
    vec3 starCol = mix(vec3(1.0), uColor, 0.2);
    vec3 col = nebula + stars * starCol * (1.0 - f * 0.35);

    float alpha = clamp(f * 0.8 + stars + core, 0.0, 1.0);
    alpha *= uAlpha * clamp(uIntensity * 60.0, 0.0, 1.0);

    OutColor = vec4(col, alpha);
}
