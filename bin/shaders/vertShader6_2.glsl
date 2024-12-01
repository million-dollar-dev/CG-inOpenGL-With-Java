#version 430

layout(location=0) in vec3 position;
layout(location=1) in vec2 texCoordIn; // Thêm tọa độ texture vào shader
layout(location=2) in vec3 normal;

uniform mat4 mv_matrix;
uniform mat4 p_matrix;

out vec2 texCoord; // Output to fragment shader

void main(void) {
    gl_Position = p_matrix * mv_matrix * vec4(position, 1.0);
    texCoord = texCoordIn; // Gán tọa độ texture cho output
}
