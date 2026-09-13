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

    // soft gradient from horizon to zenith
    float h = clamp(dir.y, -0.1, 1.0);
    vec3 zenith = uColor * 0.55;
    vec3 horizon = mix(vec3(1.0), uColor, 0.35) * 0.9;
    vec3 col = mix(horizon, zenith, pow(clamp(h, 0.0, 1.0), 0.6));

    // drifting procedural clouds
    float coverage = fbmDir(dir, uScale * 0.6, t * 0.12, 5);
    coverage = smoothstep(0.45, 0.85, coverage) * smoothstep(-0.02, 0.25, dir.y);
    vec3 cloudBright = mix(vec3(1.0), uColor, 0.3) * 1.05;
    vec3 cloudShade = uColor * 0.45;
    col = mix(col, mix(cloudShade, cloudBright, coverage), coverage * 0.85);

    // faint sparkles high in the sky
    float sparkle = skyStars(dir, 0.55, t) * smoothstep(0.15, 0.6, dir.y) * 0.5;
    col += sparkle * mix(vec3(1.0), uColor, 0.4);

    float alpha = clamp(0.25 + coverage * 0.45 + sparkle * 0.4, 0.0, 1.0);
    alpha *= uAlpha * clamp(uIntensity * 60.0, 0.0, 1.0);
    alpha *= smoothstep(-0.08, 0.05, dir.y);

    OutColor = vec4(col, alpha);
}
