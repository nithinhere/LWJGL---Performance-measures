#version 330
/**
 * Simple vertex shader; it just transforms the vertex coordinate 
 * by the current projection * view * model matrix.
 */

uniform mat4 projXview;    // this is projection * viewing matrix 
uniform mat4 uModel;     
uniform vec4 uColor;       // for uColor
uniform float psv_flag;     // psv flag
                            // 0 - multiply in shader


in vec4 vPosition;
in vec4 vNormal;
in vec4 vColor;

uniform mat4 proj;  // P matrix
uniform mat4 scene; // S matrix
uniform mat4 view; // v matrix

uniform mat4 PSV; // P*S*V


in vec4 normal;

out vec4 color;      // since only 1 out var, it will be at 0
//---------- local variables --------------
//   In a complete system these would be uniform variables associated
//   with this object.
float ka = 0.7f;
float kd = 0.3f;

vec3 lightedColor( vec3 objColor, vec3 vertexNorm )
{
	vec3 lightDir = normalize( vec3( 2, 3, 4 )); // In obj coord space to light 3 faces 
	vec3 lightColor = vec3( 1, 1, 1 );
    vec3 vNorm = vec3( normalize( vertexNorm ));    
    vec3 col = ka * objColor + kd * objColor * lightColor * dot( lightDir, vNorm );
    return col;
}

void main()
{
	vec4 vPos = vec4( vPosition.xyz, 1 );
	vec3 color3 = vec3( vColor.rgb );
    if(psv_flag == 1)
    {
        gl_Position = proj * scene * view * uModel * vPos;
    }
    else
    {
        gl_Position = projXview * uModel * vPos;
    }
	
	color = vec4( lightedColor( color3, vec3( vNormal.xyz )), 1 );
}
