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

    // polar coordinates around the zenith
    float zenith = clamp(dir.y, 0.0, 1.0);
    float angle = atan(dir.z, dir.x);

    // twisting spiral: rotation grows with distance from the zenith
    float twist = t * 0.6 + (1.0 - zenith) * uScale * 1.4;
    float arms = 3.0;
    float spiral = sin(angle * arms + twist * 4.0 - (1.0 - zenith) * uScale * 6.0);

    // noise breaks the spiral into wisps
    float wisp = fbm(vec2(angle * uScale * 0.8 + twist, zenith * uScale * 1.2 - t * 0.3), 4);
    float density = smoothstep(0.1, 0.9, spiral * 0.5 + 0.5) * (0.55 + 0.65 * wisp);
    density *= smoothstep(0.0, 0.35, zenith) * smoothstep(1.05, 0.55, zenith);

    // bright spinning core at the zenith
    float core = smoothstep(0.45, 1.0, zenith) * (0.7 + 0.3 * sin(t * 3.0));
    core *= 0.6 + 0.4 * wisp;

    vec3 inner = mix(vec3(1.0), uColor, 0.25) * 1.4;
    vec3 outer = uColor * 0.7;
    vec3 col = mix(outer, inner, core) * density + inner * core * 0.9;

    // faint counter-rotating stars being pulled in
    float stars = skyStars(dir, 0.8, t * 1.5) * (1.0 - density) * 0.55;
    col += stars * mix(vec3(1.0), uColor, 0.3);

    float alpha = clamp(density + core * 0.6 + stars, 0.0, 1.0);
    alpha *= uAlpha * clamp(uIntensity * 60.0, 0.0, 1.0);
    alpha *= smoothstep(-0.04, 0.08, dir.y);

    OutColor = vec4(col, alpha);
}
