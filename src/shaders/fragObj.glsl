#version 430 core

in vec2 tc;
out vec4 color;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
layout (binding=0) uniform sampler2D samp;

void main(void) {
    color = texture(samp, tc);
}
