#version 430 
in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVector;
in vec4 glp;
out vec4 fragColor;
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
uniform mat4 norm_matrix;
uniform int isAbove;
layout (binding=0) uniform sampler2D reflectTex;
layout (binding=1) uniform sampler2D refractTex;
void main(void)
{ 
// normalize the light, normal, and view vectors:
 vec3 L = normalize(varyingLightDir);
 vec3 N = normalize(varyingNormal);
 vec3 V = normalize(-v_matrix[3].xyz - varyingVertPos);
 vec3 H = normalize(varyingHalfVector);
 // compute light reflection vector with respect to N:
 vec3 R = normalize(reflect(-L, N));
 // get the angle between the light and surface normal:
 float cosTheta = dot(L,N);
 // angle between the view vector and reflected light:
 float cosPhi = dot(H,N);
 // compute ADS contributions (per pixel), and combine to build output color:
 vec3 ambient = ((globalAmbient * material.ambient) + (light.ambient * material.ambient)).xyz;
 vec3 diffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta,0.0);
 vec3 specular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi,0.0), material.shininess);

 vec4 mixColor, reflectColor, refractColor, blueColor;
 if (isAbove == 1)
 { 
 refractColor = texture(refractTex, (vec2(glp.x,glp.y))/(2.0*glp.w)+0.5);
 reflectColor = texture(reflectTex, (vec2(glp.x,-glp.y))/(2.0*glp.w)+0.5);
 mixColor = (0.2 * refractColor) + (1.0 * reflectColor);
 }
 else
 {
 refractColor = texture(refractTex, (vec2(glp.x,glp.y))/(2.0*glp.w)+0.5);
 blueColor = vec4(0.0, 0.25, 1.0, 1.0);
 mixColor = (0.5 * blueColor) + (0.6 * refractColor);
 }
 fragColor = vec4((mixColor.xyz * (ambient + diffuse) + 0.75*specular), 1.0);
}
