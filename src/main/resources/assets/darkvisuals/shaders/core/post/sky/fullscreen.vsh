#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 ScreenPos;

void main() {
    gl_Position = vec4(Position.xy, 1.0, 1.0);
    ScreenPos = Position.xy;
}
