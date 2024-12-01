#version 430
layout (location=0) in vec3 pos;
layout (location=1) in vec2 texCoord;
out vec2 tc;
out vec3 originalPosition;
uniform mat4 mv_matrix;
uniform mat4 p_matrix;
//layout (binding=0) uniform sampler3D samp; 
void main(void)
{
originalPosition = pos;
gl_Position = p_matrix * mv_matrix * vec4(pos, 1.0);
tc = texCoord;
}