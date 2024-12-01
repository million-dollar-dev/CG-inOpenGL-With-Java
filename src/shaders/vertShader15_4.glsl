#version 430
layout (location=0) in vec3 vertPos;
layout (location=2) in vec3 vertNormal; 
out vec3 varyingNormal;
out vec3 varyingLightDir;
out vec3 varyingVertPos;
out vec3 varyingHalfVector;
out vec4 glp;
out vec3 originalPosition;
struct PositionalLight
{ 
vec4 ambient;  
vec4 diffuse;  
vec4 specular;  
vec3 position;
};
struct Material
{ 
vec4 ambient;  
vec4 diffuse;  
vec4 specular;  
float shininess;
};
uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix; 
uniform mat4 p_matrix;
uniform mat4 norm_matrix;   // for transforming normals
layout (binding=2) uniform sampler3D noiseTex;
void main(void)
{ 
 varyingHalfVector = normalize(normalize(varyingLightDir) + normalize(-varyingVertPos)).xyz;
 varyingVertPos=(m_matrix * vec4(vertPos,1.0)).xyz;
 varyingLightDir = light.position - varyingVertPos;
 varyingNormal=(norm_matrix * vec4(vertNormal,1.0)).xyz;
 originalPosition = vertPos; 
 glp = p_matrix * v_matrix * m_matrix * vec4(vertPos,1.0);
 gl_Position = glp;
}