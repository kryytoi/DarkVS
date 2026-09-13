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

    // aurora curtains: vertical bands that ripple along the horizon angle
    float azimuth = atan(dir.z, dir.x);
    float elevation = asin(clamp(dir.y, -1.0, 1.0));

    float curtainSeed = azimuth * uScale * 0.8;
    float drift = fbm(vec2(curtainSeed * 0.5, t * 0.25), 4);
    float curtain = fbm(vec2(curtainSeed + drift * 3.0, t * 0.35), 5);
    curtain = smoothstep(0.35, 0.9, curtain);

    // rays rise from the horizon and fade toward the zenith
    float bandHeight = 0.35 + 0.3 * fbm(vec2(curtainSeed * 0.7, t * 0.15), 3);
    float vertical = smoothstep(-0.05, 0.15, elevation) * smoothstep(bandHeight + 0.55, 0.05, elevation);
    float rayShimmer = 0.65 + 0.35 * sin(curtainSeed * 14.0 + t * 2.2 + drift * 8.0);

    // classic aurora palette blended with the theme color
    vec3 green = vec3(0.15, 0.95, 0.45);
    vec3 teal = vec3(0.1, 0.55, 0.9);
    vec3 purple = vec3(0.55, 0.2, 0.85);
    vec3 hue = mix(green, teal, clamp(elevation * 2.2, 0.0, 1.0));
    hue = mix(hue, purple, smoothstep(0.25, 0.6, elevation) * 0.55);
    hue = mix(hue, uColor, 0.35);

    vec3 col = hue * curtain * vertical * rayShimmer * 1.5;
    col += hue * curtain * vertical * 0.25; // soft ambient glow

    // a few stars behind the curtains
    float stars = skyStars(dir, 0.7, t) * smoothstep(0.05, 0.4, elevation) * 0.6;
    col += stars * vec3(0.9, 0.95, 1.0) * (1.0 - curtain * 0.6);

    float alpha = clamp(curtain * vertical * 1.3 + stars * 0.5, 0.0, 1.0);
    alpha *= uAlpha * clamp(uIntensity * 60.0, 0.0, 1.0);

    OutColor = vec4(col, alpha);
}
