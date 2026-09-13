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

    // sharp caustic web: ridged noise layered at two scales
    vec2 p = vec2(atan(dir.z, dir.x), asin(clamp(dir.y, -1.0, 1.0))) * uScale;
    vec2 flow = vec2(t * 0.15, t * -0.1);

    float n1 = fbm(p * 1.4 + flow, 4);
    float n2 = fbm(p * 2.8 - flow * 1.6 + 13.7, 4);
    float ridge1 = 1.0 - abs(n1 * 2.0 - 1.0);
    float ridge2 = 1.0 - abs(n2 * 2.0 - 1.0);
    float caustic = pow(clamp(ridge1 * ridge2 * 1.6, 0.0, 1.0), 3.0);

    vec3 base = uColor * 0.4 + vec3(0.03, 0.06, 0.09);
    vec3 bright = mix(vec3(1.0), uColor, 0.25);
    vec3 col = base + caustic * bright * 1.3;

    // slow pulsing glow between the caustic ridges
    float pulse = 0.5 + 0.5 * sin(t * 1.2 + n1 * 6.0);
    col += (1.0 - caustic) * pulse * 0.12 * mix(vec3(1.0), uColor, 0.5);

    float alpha = (0.3 + caustic * 0.65) * uAlpha * clamp(uIntensity * 60.0, 0.0, 1.0);
    alpha *= smoothstep(-0.06, 0.06, dir.y);

    OutColor = vec4(col, alpha);
}
