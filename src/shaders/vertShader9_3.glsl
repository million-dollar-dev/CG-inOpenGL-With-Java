 #version 430
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
out vec3 varyingNormal;
out vec3 varyingVertPos;
uniform mat4 mv_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
layout (binding = 0) uniform samplerCube tex_map;
void main(void)
{ 
varyingVertPos = (mv_matrix * vec4(position,1.0)).xyz;
varyingNormal = (norm_matrix * vec4(normal,1.0)).xyz;
gl_Position = p_matrix * mv_matrix * vec4(position, 1.0);
}