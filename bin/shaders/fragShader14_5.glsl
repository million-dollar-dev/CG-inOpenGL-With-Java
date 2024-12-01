#version 430
in vec3 originalPosition; 
out vec4 color;
uniform mat4 mv_matrix;
uniform mat4 p_matrix;
layout (binding=0) uniform sampler3D samp;
void main(void)
{ 
color = texture(samp, originalPosition/2.0 + 0.5); 
}