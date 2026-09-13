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

    // dense twinkling star field
    float stars = skyStars(dir, uScale * 0.28, t);
    float fineStars = skyStars(dir + 13.7, uScale * 0.45, t * 1.4) * 0.55;
    float milky = fbmDir(dir, uScale * 0.5, 0.0, 4);
    milky = smoothstep(0.45, 0.9, milky);

    vec3 starTint = mix(vec3(1.0), uColor, 0.25);
    vec3 col = (stars + fineStars) * starTint;
    col += milky * mix(vec3(0.35, 0.4, 0.55), uColor, 0.4) * 0.35 * (0.8 + 0.2 * sin(t * 0.5));

    // shooting stars: one bright streak per time window, sweeping across the dome
    float window = floor(t / 6.0);
    float phase = fract(t / 6.0);
    vec2 seed2 = vec2(hash12(vec2(window, 1.0)), hash12(vec2(window, 7.0)));
    float sAzimuth = seed2.x * 6.2831;
    float sElevation = 0.35 + seed2.y * 0.5;
    float sDirAzimuth = (hash12(vec2(window, 3.0)) - 0.5) * 1.2;
    float travel = phase * 2.2;
    float az = sAzimuth + sDirAzimuth * travel;
    float el = sElevation - travel * 0.25;

    vec2 sph = vec2(atan(dir.z, dir.x), asin(clamp(dir.y, -1.0, 1.0)));
    vec2 streakPos = vec2(az, el);
    float streakDist = length(vec2(atan(sin(sph.x - streakPos.x), cos(sph.x - streakPos.x)), sph.y - streakPos.y));
    float head = smoothstep(0.03, 0.0, streakDist);
    float tail = smoothstep(0.3, 0.0, streakDist + travel * 0.12) * smoothstep(0.0, 0.15, travel * 0.12 - streakDist + 0.3);
    float visible = step(phase, 0.6) * step(0.15, phase);
    col += (head * 1.6 + tail * 0.6) * visible * mix(vec3(1.0), uColor, 0.15);

    float alpha = clamp(stars + fineStars + milky * 0.5 + (head + tail) * visible, 0.0, 1.0);
    alpha *= uAlpha * clamp(uIntensity * 60.0, 0.0, 1.0);

    OutColor = vec4(col, alpha);
}
