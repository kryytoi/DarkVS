#version 150

#moj_import <darkvisuals:common.glsl>

in vec3 Position;
in vec4 Color;

uniform sampler2D Sampler0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform vec4 Rect;
uniform vec4 Screen;
uniform vec4 Radius;
uniform vec4 Cuts;
uniform vec4 Tint;
uniform vec4 Glass;
uniform vec4 Blob0;
uniform vec4 Blob1;
uniform vec2 Blobs;
uniform float BlurRadius;

out vec2 fragCoord;
out vec2 texCoord;
out vec2 screenCoord;
out vec4 cornerRadii;
out vec4 cutSizes;
out vec4 tintColor;
out vec4 glass;
out vec4 blob0;
out vec4 blob1;
out vec2 blobs;
out float guiScale;
out float blurRadius;
out vec2 texelSize;
out float globalAlphaIn;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    fragCoord = rvertexcoord(gl_VertexID);
    texCoord = gl_Position.xy * 0.5 + 0.5;
    screenCoord = Rect.xy + fragCoord * Rect.zw;

    cornerRadii = Radius;
    cutSizes = Cuts;
    tintColor = Tint;
    glass = Glass;
    blob0 = Blob0;
    blob1 = Blob1;
    blobs = Blobs;
    guiScale = Screen.z;
    blurRadius = BlurRadius;
    texelSize = 1.0 / textureSize(Sampler0, 0);
    globalAlphaIn = Screen.w;
}
