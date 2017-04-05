/**
 * A substitute for java.awt.Color since it can't be used in 
 * the same application as LWJGL3 on Mac OS X.
 */

/**
 * @author rdb
 * 9/29/15
 * 01/17/17 rdb added rgb(), rgba() getRGB() and getRGBA() methods to return
 *          color components as a float[]. This is useful for interface to
 *          LWJGL GLSL data transfer methods, glUniform4fv etc.
 */
public class Color
{
    //---------------- instance variables ------------------------
    private float red;
    private float green;
    private float blue;
    private float alpha;
    
    private float[] rgba;
    //--------------------- constructors --------------------------
    /**
     * Simply 3 floats.
     */
    public Color( float r, float g, float b )
    {
        this( r, g, b, 1 );  // invoke 4 float constructor
    }    
    /**
     * 4 floats.
     */
    public Color( float r, float g, float b, float a )
    {
        rgba = new float[ 4 ];
        setColor( r, g, b, a );
    }
    /**
     * Getters like java.awt.Color, but also simpler ones like GLSL
     */
    public float getRed() 
    {
        return red;
    }
    public float getGreen() 
    {
        return green;
    }
    public float getBlue() 
    {
        return blue;
    }
    public float getAlpha() 
    {
        return alpha;
    }
    public float r() 
    {
        return red;
    }
    public float g() 
    {
        return green;
    }
    public float b() 
    {
        return blue;
    }
    public float a() 
    {
        return alpha;
    }

    /**
     * Convert a color to a float[3].
     * @return float[]  a 3 element float array in order r,g,b,a
     */
    public float[] get3f()     // return color as 4-float array
    {
    	float[] f3 = new float[ 3 ];
        f3[ 0 ] = red;
        f3[ 1 ] = green;
        f3[ 2 ] = blue;
        return f3;
    }    /**
     * Convert a color to a float[4].
     * @return float[]  a 4 element float array in order r,g,b,a
     */
    public float[] get4f()     // return color as 4-float array
    {
    	float[] f4 = new float[ 4 ];
        f4[ 0 ] = red;
        f4[ 1 ] = green;
        f4[ 2 ] = blue;
        f4[ 3 ] = alpha;
        return f4;
    }
    /**
     * Setters.
     */
    public void setColor( float r, float g, float b, float a )
    {
        rgba[ 0 ] = red = r;
        rgba[ 1 ] = green = g;
        rgba[ 2 ] = blue = b;
        rgba[ 3 ] = alpha = a;
    }
    public void setColor( Color c )
    {
        rgba[ 0 ] = red = c.red;
        rgba[ 1 ] = green = c.green;
        rgba[ 2 ] = blue = c.blue;
        rgba[ 3 ] = alpha = c.alpha;
    }
    public String toString()
    {
        return "[ " + red + ", " + green + ", " + blue + ", " + alpha + "]";
    }
}
