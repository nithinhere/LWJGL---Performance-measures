#version 330 
/**
 * Simplest fragment shader
 * color comes from program
 */

in vec4 color;

out vec4 fcolor;   // only one out, it will be in position 0

void main()
{
    fcolor = color; // gets color from vertex shader
}
