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

    // looking up from under water: layered flowing sine bands
    vec2 p = vec2(atan(dir.z, dir.x), asin(clamp(dir.y, -1.0, 1.0))) * uScale;
    float wave = 0.0;
    float amp = 0.5;
    float freq = 1.0;
    for (int i = 0; i < 5; i++) {
        wave += amp * sin(p.x * freq + t * (0.8 + freq * 0.3)) * cos(p.y * freq * 0.7 - t * 0.5);
        amp *= 0.55;
        freq *= 1.9;
    }
    wave = wave * 0.5 + 0.5;

    vec3 deep = uColor * 0.35 + vec3(0.02, 0.05, 0.08);
    vec3 shallow = mix(uColor, vec3(0.6, 0.95, 1.0), 0.4);
    vec3 col = mix(deep, shallow, pow(wave, 1.6));

    // shimmering highlights on the wave crests
    float caustic = fbm(p * 2.2 + vec2(t * 0.4, -t * 0.25), 4);
    caustic = smoothstep(0.55, 0.95, caustic);
    col += caustic * 0.35 * shallow;

    // gentle light shafts toward the zenith
    col += smoothstep(0.3, 1.0, dir.y) * shallow * 0.15;

    float alpha = (0.45 + wave * 0.3 + caustic * 0.25) * uAlpha * clamp(uIntensity * 60.0, 0.0, 1.0);
    alpha *= smoothstep(-0.06, 0.06, dir.y);

    OutColor = vec4(col, alpha);
}
