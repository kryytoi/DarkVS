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

vec3 hueToRgb(float h) {
    vec3 k = clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    return k;
}

void main() {
    vec3 dir = skyViewRay(ScreenPos);
    float t = uTime * uSpeed;

    float azimuth = atan(dir.z, dir.x);
    float elevation = asin(clamp(dir.y, -1.0, 1.0));

    // main arc and a fainter secondary arc
    float arcCenter = 0.45 + 0.06 * sin(t * 0.3);
    float arc1 = 1.0 - smoothstep(0.0, 0.16, abs(elevation - arcCenter));
    float arc2 = 1.0 - smoothstep(0.0, 0.10, abs(elevation - arcCenter * 1.75));
    float bandMask = smoothstep(0.6, 0.9, sin(azimuth * 0.5)) * 0.5 + 0.5;

    // rainbow hue runs across the band thickness
    float hue1 = (elevation - arcCenter) / 0.16 + 0.5;
    float hue2 = 1.0 - ((elevation - arcCenter * 1.75) / 0.10 + 0.5); // reversed secondary arc
    vec3 rainbow1 = hueToRgb(fract(hue1 * 0.85 + t * 0.02));
    vec3 rainbow2 = hueToRgb(fract(hue2 * 0.85 + 0.5 + t * 0.02));

    // soft noise so the bands breathe
    float breath = 0.75 + 0.25 * fbm(vec2(azimuth * uScale * 0.5, t * 0.2), 4);

    vec3 tint = normalize(uColor + 0.001);
    vec3 col = rainbow1 * arc1 * bandMask * breath * 0.95;
    col += rainbow2 * arc2 * bandMask * breath * 0.4;
    col = mix(col, col * tint * 1.6, 0.3);

    // gentle sun-glow near the top of the arc
    col += smoothstep(0.5, 0.9, arc1) * mix(vec3(1.0), tint, 0.5) * 0.2 * breath;

    float alpha = clamp(arc1 * bandMask * breath + arc2 * bandMask * 0.5, 0.0, 1.0);
    alpha *= uAlpha * clamp(uIntensity * 60.0, 0.0, 1.0);

    OutColor = vec4(col, alpha);
}
