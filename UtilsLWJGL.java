/*
 * UtilsLWJGL.java from lwjgl3-demo-master
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 *   Essentially a copy of DemoUtils.java with the added public static methods:
 *      long openWindow( int winW, int winH )
 *      long openWindow( int winW, int winH, openglMajor, int openglMinor )
 *      int makeShaderProgram( String vertexShader, String fragmentShader )
 *      int makeShaderProgram( String vShader, String fShader, String version )
 *      
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 *
 * Edited by rdb Sept 2015:
 *  1. unpackaged it; simple demos; keep the structure simple. 
 *  2. minor formatting changes to conform more closely to my style.
 *  3. changed name from DemoUtils to UtilsLWJGL
 *  4. Added openWindow and makeShaderProgram
 *  5. Added checkBuffer method that reads a buffer back from GPU
 *      and compares to what was intended to be downloaded.
 * Nov 2016 rdb edits caused by changes in LWJGL3.1
 *  6. Changes in org.lwjgl.system.MemoryUtil
 *  7. Changes in glfw.GLFWvidmode
 *  8. glfwInit() returns boolean rather than GL11 constants
 *  9. GLContext seems to have disappeared.
 * 12/20/16 rdb
 *  Modified  openWindow( String title, int winW, int winH )
 *      so it has no default version, and specifies no version in hints;
 *      This makes lower end programs simpler: no need to specify version.
 *      Anything that needs more thna 4.1 should then query the version.
 *  Modified openWindow with version to try lower versions if specified
 *      version fails to get a window. Prints err at each failed attempt.
 * 12/28/16
 *  Modified BufferUtils to use MemoryUtil instead of BufferUtils (except when
 *      returning a Buffer object. No discernible peformance difference even  
 *      did 50000 objects with fpm in the 4 range.
 * 01/18/17 rdb Modified UtilsLWJGL.openWindow to work better in Eclipse
 *              where getenv is dangerous to use; instead use System.getProperty
 * 01/19/17 rdb Modified UtilsLWJGL.openWindow to better handle Linux.
 * 01/26/17 rdb Added prefix-based makeShaderProgram method
 */

//rdb package org.lwjgl.demo.opengl.util;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; //Eclipse says not used, but it is!
import static org.lwjgl.opengl.GL20.*; //Eclipse says not used, but it is!
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilsLWJGL 
{
    //------------------------ openWindow ----------------------------
    /**
     * Do whatever is necessary to open a GLFW window. This code is heavily
     * based on code from lwjgl3-demo-master. 
     * @author rdb  
     * @param winW int    width of window to create
     * @param winH int    height of window
     * @return int        window Id for created window
     * 
     * Returns the window id.
     */
	public static long openWindow( String title, int winW, int winH )
    {
        // if system environment specifies default Open GL version, use it
        //    GLVERSION is an environment variable defined by rdb to help
        //    provide better portability of code. 
		// ***System.getenv doesn't work for user env variables in Eclipse
        //
        String oglVersion = System.getenv( "GLVERSION" );
        if ( oglVersion != null )
            return openWindow( title, winW, winH, oglVersion );

        String osname = System.getProperty( "os.name" );

        // Now check if on linux and MESA_GL_VERSION is set; if so use it
        //    MESA_GL_VERSION is defined by the Mesa GL environment
        //*** System.getenv doesn't work for user env variables in Eclipse
        oglVersion = System.getenv( "MESA_GL_VERSION" );
        
        if ( osname.equalsIgnoreCase( "linux" ) && oglVersion != null )
            return openWindow( title, winW, winH, oglVersion );

        // Because of idiosyncracies of Mac OS X OpenGL implementation, need
        //  to make a hack here; must specify a desired version.
        if ( osname.startsWith( "Mac" ) || osname.equalsIgnoreCase( "linux" ))
        {
            return openWindow( title, winW, winH, 4, 1 );
        }
        // This code is modeled after Anton's demos
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException( "Unable to initialize GLFW" );

        // Configure our window
        glfwDefaultWindowHints(); // optional, current window hints are default
        glfwWindowHint( GLFW_VISIBLE, GL_FALSE ); // window hidden after create
        glfwWindowHint( GLFW_RESIZABLE, GL_TRUE ); // window is resizable
        glfwWindowHint( GLFW_DEPTH_BITS, 32 );  // is this needed for depth??
        glfwWindowHint( GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE ); 
        glfwWindowHint( GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE );

        // Create the window
        long windowId = glfwCreateWindow( winW, winH, title, NULL, NULL );

        if ( windowId == NULL )
            throw new RuntimeException( "Failed to create the GLFW window" );
        return windowId;
    }
    /**
     * Do whatever is necessary to open a GLFW window. This code is heavily
     * based on code from lwjgl3-demo-master.
     * @author rdb  
     * @par:am winW int    width of window to create
     * @param winH int    height of window
     * @param String      openglVersion, in form major.minor[FC]
     * @return int        window Id for created window
     * 
     * Returns the window id.
     */
	public static long openWindow( String title, int winW, int winH, 
			                      String oglVersion )
    {
        // oglVersion must be of the form "4.1" = major.minor
        //    Note: linux/MESA allows option "FC" to indicate forward comp.
        //          This then becomes part of the regex.
        Pattern vPattern = Pattern.compile( "(\\d+)\\.(\\d+)\\.*" );
        Matcher m  = vPattern.matcher( oglVersion );
        
        if ( !m.find() )
        {
            System.err.println( "UtilsLWJGL.openWindow parameter error: "
               + "GL version string is not of the form major.minor: " +
               oglVersion + "\n     Trying with 4.3" );
            return openWindow( title, winW, winH, 4, 3 );
        }

        try
        {
            int major = new Integer( m.group( 1 ) );
            int minor = new Integer( m.group( 2 ) );
            return openWindow( title, winW, winH, major, minor );
        }
        catch ( NumberFormatException nfe )
        {
            System.err.println( "Unexpected error in openWindow processing!\n" +
                  "with version parameter: " + oglVersion );
        }
        return NULL;
    }
    /**
     * Do whatever is necessary to open a GLFW window. This code is heavily
     * based on code from lwjgl3-demo-master.
     * @author rdb  
     * @par:am winW int    width of window to create
     * @param winH int    height of window
     * @param int openglMajor  major version # for opengl
     * @param int openglMinor  minor version # for opengl
     * @return int        window Id for created window
     * 
     * Returns the window id.
     */
	public static long openWindow( String title, int winW, int winH, 
			                      int openglMajor, int openglMinor )
    {
    	long windowId = NULL;    //  NULL is defined in glfw.
        
        // Initialize GLFW. Most GLFW functions won't before doing this.
        if ( !glfwInit() )  // rdb 8
            throw new IllegalStateException( "Unable to initialize GLFW" );
        
        // Configure our window
        glfwWindowHint( GLFW_VISIBLE, GL_TRUE ); // window visible after create
        glfwWindowHint( GLFW_RESIZABLE, GL_TRUE ); // window is resizable
        glfwWindowHint( GLFW_DEPTH_BITS, 32 );  // is this needed for depth
        //---- next two are critical ---------
        glfwWindowHint( GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE ); 
        glfwWindowHint( GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE ); 

        // 09/10/15: Mac OS/X, Windows, and Linux all support 3.3!
        // 09/22/15: Well, kind of ???

        while ( openglMajor > 1 && windowId == NULL )
        {
            glfwWindowHint( GLFW_CONTEXT_VERSION_MAJOR, openglMajor );
            glfwWindowHint( GLFW_CONTEXT_VERSION_MINOR, openglMinor );
 
            // Create the window
            windowId = glfwCreateWindow( winW, winH, title, NULL, NULL );
 
            if ( windowId == NULL )
            {
                System.err.println( "GLFW window creation failed for Open GL " 
                        + openglMajor + "." + openglMinor );
                if ( openglMinor > 0 ) 
                    openglMinor--;
                else 
                {   
                    openglMinor = 5; // a hack; this means we'll miss 3.6+ etc
                    openglMajor--;
                }
            }
        }
        if ( windowId == NULL )
        {        	
            throw new RuntimeException( "Failed to create the GLFW window: " );
        }
       
        // Get the resolution of the primary monitor
        GLFWVidMode vmode = glfwGetVideoMode( glfwGetPrimaryMonitor() );
        int centerX = ( vmode.width() - winW ) / 2;
        int centerY = ( vmode.height() - winH ) / 2;

        // Center our window
        glfwSetWindowPos( windowId, centerX, centerY );
        
        // Make the OpenGL context current
        glfwMakeContextCurrent( windowId );
        
        // rdb: true => forward compatible;  do not use forward compatibility
        GL.createCapabilities(); 
        
        // Enable v-sync
        glfwSwapInterval( 0 );

        String glv = glGetString( GL_VERSION );
        System.err.println( "*** Using GL version: " + glv );

        glfwShowWindow( windowId );
        return windowId;
    }

    //------------------- glError( String ) ----------------------------------
    /**
     * Utility function to check for gl error condition and print a message 
     *          to std err.
     * @author rdb
     */
	public static int glError( String id )
	{
	    int err = glGetError(); 
	    if ( err == GL_NO_ERROR )
	        return GL_NO_ERROR;
	    System.err.println( "*** GL error " + id + " :" + glErrorMessage( err ));
	    return err;
	}
    //------------------- glErrorMessage ----------------------------------
    /**
     * Map gl error code to a simple String message.
     * @author rdb
     * 
     */
    public static String glErrorMessage( int errCode )
    {
        String errName;
        switch ( errCode )
        {
        case GL11.GL_INVALID_ENUM:  //  0x0500;
            errName = "GL_INVALID_ENUM";
            break;
        case GL11.GL_INVALID_VALUE: //  0x0501;
            errName = "GL_INVALID_VALUE";
            break;
        case GL11.GL_INVALID_OPERATION: //  0x0502;
            errName = "GL_INVALID_OPERATION";
            break;
        case GL11.GL_STACK_OVERFLOW:  //  0x0503;
            errName = "GL_STACK_OVERFLOW";
            break;
        case GL11.GL_STACK_UNDERFLOW:   // 0x0504;
            errName = "GL_STACK_UNDERFLOW";
            break;
        case GL11.GL_OUT_OF_MEMORY:    //  0x0505;
            errName = "GL_OUT_OF_MEMORY";
            break;
        case GL_INVALID_FRAMEBUFFER_OPERATION:  //  0x0506;
            errName = "GL_INVALID_FRAMEBUFFER_OPERATION";
            break;
        case 0x0507:
            errName = "GL_CONTEXT_LOST";
            break;
        default:
            errName = "GL unknown error";
            break;
        }
        return String.format( "GL Error: 0x0%3x (%5d) %s", errCode, errCode, errName );
    }
	
    //--------------- makeShaderProgram( String ) ---------------
    /**
     * Create the shader programs for this application. This is a
     * convenience interface for the common case, where the vertex shader
     * and the fragment shader have the same name -- except for the .vsh or
     * .fsh extension. Pass in only the prefix.
     *   
     * @author rdb
     * @param  prefix String     filename prefix for both shaders
     * @return long                   shader program id
 	 *
	 * @throws IOException
     */
    public static int makeShaderProgram( String prefix ) 
    		throws IOException
    {
    	return makeShaderProgram( prefix + ".vsh", prefix + ".fsh" ); 
    }
    //--------------- makeShaderProgram( String, String ) ---------------
    /**
     * Create the shader programs for this application. This code is
     * based on code from lwjgl3-demo-master. 
     * @author rdb
     * @param vertexShader String     filename for the vertex shader code
     * @param fragmentShader String   filename for the fragment shader code
     * @return long                   shader program id
	 *
	 * @throws IOException
     */
    public static int makeShaderProgram( String vertexShader, 
                                         String fragmentShader ) 
    		throws IOException
    {
        // last parameter is desired version; default is system default.
    	return makeShaderProgram( vertexShader, fragmentShader, null );
    }
    /**
     * Create shader programs for application for a specific opengl version.
     * This code is based on code from lwjgl3-demo-master. 
     * @author rdb
     * @param vertexShader String     filename for the vertex shader code
     * @param fragmentShader String   filename for the fragment shader code
     * @param version String          desired opengl version in form: 3.3
     * @return long                   shader program id
	 *
	 * @throws IOException
     */
    public static int makeShaderProgram( String vertexShader, 
                                         String fragmentShader, 
                                         String version ) 
    		throws IOException
    {
		int vshader = createShader( vertexShader, GL_VERTEX_SHADER );
		int fshader = createShader( fragmentShader, GL_FRAGMENT_SHADER );

		int program = glCreateProgram();
		glAttachShader( program, vshader );
		glAttachShader( program, fshader );

		glLinkProgram( program );
		int linked = glGetProgrami( program, GL_LINK_STATUS );
		String programLog = glGetProgramInfoLog( program );
		if ( programLog.trim().length() > 0 ) 
        {
			System.err.println( programLog );
		}
		if ( linked == 0 ) 
        {
			throw new AssertionError( "Could not link program" );
		}
		return program;
	}
    //------------- checkBuffer ---------------------------
    /**
     * Read data from currently bound buffer and compare it to the 
     * FloatBuffer that is expected there. Return a count of mismatches.
     * If there are mismatches, the first "printErrors" of those will be printed.
     * 
     * If printErrors < 0, all entries will be printed with error count and
     *     the absolute value of printErrors is the number of values per line.
     * If a filename is specified, it is used for output; otherwise System.err.
     */
    public static int checkBuffer( FloatBuffer sent, int nFloats, int printInfo )
    {
        return checkBuffer( sent, nFloats, printInfo, null );
    }
    public static int checkBuffer( FloatBuffer sent, int nFloats, int printInfo, 
                                    String file )
    {
        PrintWriter out;
        if ( file == null )
            out = new PrintWriter ( System.err );
        try {
            out = new PrintWriter( file );
        }
        catch ( IOException ioe )
        {
            System.err.println( "Can't open " + file + " -- Using System.err"  );
            out = new PrintWriter( System.err );
        }
            
        FloatBuffer gpuBuf = MemoryUtil.memAllocFloat( nFloats );
        glGetBufferSubData( GL_ARRAY_BUFFER, 0, gpuBuf );

        boolean printData =  ( printInfo < 0 );
        int numsPerLine = -printInfo;
        int numsOnLine = 0;
        
        int printErrMax = Math.max( 0,  printInfo );
            
        int errCount = 0;
        int printCount = 0;
        sent.rewind();
        gpuBuf.rewind();
        for ( int j = 0; j < nFloats; j++ )
        {
            float sValue = sent.get();
            float bValue = gpuBuf.get();
            if ( printData )
            {
                out.print(  bValue + " " );
                if ( ++numsOnLine >= numsPerLine )
                {
                    out.println();
                    numsOnLine = 0;
                }
            }
            
            if ( sValue != bValue )
            {
                errCount++;
                if ( printCount++ < numsPerLine )
                    out.printf( "*** checkBuffer mismatch %d= %7.3f should be %7.3f\n",
                                          errCount, bValue, sValue );
            }
        }
        sent.flip();
        
        if ( errCount > 0 && printErrMax > 0 )
            out.printf(  "*** checkBuffer:  %d  mismatches in %d values.\n",
                       errCount, nFloats );
        out.close();
        MemoryUtil.memFree( gpuBuf );
        return errCount;
    }    
    //------------- printBuffer ---------------------------
    /**
     * Read data from currently bound buffer and print the contents. 
     * The printLimit parameter specifies the maximum number of values to print.
     */
    public static void printBuffer( String title, int nFloats, int printLimit )
    {
        printBuffer( title, nFloats, printLimit, null );
    }
    public static void printBuffer( String title, int nFloats, int printLimit, String file )
    {
        PrintWriter out;
        if ( printLimit <= 0 )
            printLimit = nFloats;
        if ( file == null )
            out = new PrintWriter ( System.out );
        else
        {
            try {
                out = new PrintWriter( file );
            }
            catch ( IOException ioe )
            {
                System.err.println( "UtilsLWJGL.printBuffer: Can't open " 
                        + file + " -- Using System.err"  );
                out = new PrintWriter( System.out );
            }
        }
        out.println( title ); 
        FloatBuffer gpuBuf = MemoryUtil.memAllocFloat( nFloats );
        glGetBufferSubData( GL_ARRAY_BUFFER, 0, gpuBuf );

        int numsPerLine = 9;  // this is enough to hold 1 triangle
        int numsOnLine = 0;
            
        gpuBuf.rewind();
        for ( int j = 0; j < printLimit; j++ )
        {
            float val = gpuBuf.get();
                out.print(  val + " " );
                if ( ++numsOnLine >= numsPerLine )
                {
                    out.println();
                    numsOnLine = 0;
                }
            
        }
        gpuBuf.flip();
        if ( numsOnLine != 0 )
            out.println();
        out.close();
        MemoryUtil.memFree( gpuBuf );
        return;
    }    
    //+++++++++++++++ Kai Burjack methods from lwjgl3-demo-master +++++++++++++++ 
    /**
     * Create a shader object from the given classpath resource.
     *
     * @param resource
     *            the class path
     * @param type
     *            the shader type
     *
     * @return the shader object id
     *
     * @throws IOException
     */
    public static int createShader( String resource, int type ) 
           throws IOException 
    {
        return createShader( resource, type, null );
    }

    /**
     * Reads the specified resource and returns the raw data as a ByteBuffer.
     *
     * @param resource   the resource to read
     * @param bufferSize the initial buffer size
     *
     * @return the resource data
     *
     * @throws IOException if an IO error occurs
     */
    public static ByteBuffer ioResourceToByteBuffer( String resource, int bufferSize ) 
           throws IOException 
    {
        ByteBuffer buffer;

        File file = new File( resource );
        if ( file.isFile() ) 
        {
            FileInputStream fis = new FileInputStream( file );
            FileChannel fc = fis.getChannel();
            buffer = MemoryUtil.memAlloc( ( int )fc.size() + 1 );

            while ( fc.read( buffer ) != -1 ) ;

            fc.close();
            fis.close();
        } 
        else 
        {
            buffer = MemoryUtil.memAlloc( bufferSize );

            InputStream source = Thread.currentThread()
                        .getContextClassLoader().getResourceAsStream( resource );
            if ( source == null )
                throw new FileNotFoundException( resource );

            try 
            {
                ReadableByteChannel rbc = Channels.newChannel( source );
                try 
                {
                    while ( true ) 
                    {
                        int bytes = rbc.read( buffer );
                        if ( bytes == -1 )
                            break;
                        if ( buffer.remaining() == 0 )
                            buffer = MemoryUtil.memRealloc( buffer, buffer.capacity() * 2 );
                    }
                } 
                finally 
                {
                    rbc.close();
                }
            } 
            finally 
            {
                source.close();
            }
        }

        buffer.flip();
        return buffer;
    }

    /**
     * Create a shader object from the given classpath resource.
     *
     * @param resource
     *            the class path
     * @param type
     *            the shader type
     * @param version
     *            the GLSL version to prepend to the shader source, or null
     *
     * @return the shader object id
     *
     * @throws IOException
     */
    public static int createShader( String resource, int type, String version ) 
           throws IOException 
    {
        int shader = glCreateShader( type );

        //rdb note: the "magic" number 8192 is only and "initial" buffer size.
        //          The code increases the buffer if it is not large enough
        ByteBuffer source = ioResourceToByteBuffer( resource, 8192 );

        if ( version == null ) 
        {
            PointerBuffer strings = MemoryUtil.memAllocPointer( 1 );
            IntBuffer lengths = MemoryUtil.memAllocInt( 1 );

            strings.put( 0, source );
            lengths.put( 0, source.remaining());

            glShaderSource( shader, strings, lengths );
            MemoryUtil.memFree( lengths );
            MemoryUtil.memFree( strings );
        } 
        else 
        {
            PointerBuffer strings = MemoryUtil.memAllocPointer( 1 );
            IntBuffer lengths = MemoryUtil.memAllocInt( 2 );

            String preambleString = "#version " + version + "\n"; 
            byte[] barray = preambleString.getBytes();
            ByteBuffer preamble = MemoryUtil.memAlloc( barray.length );
            preamble.put( barray );

            strings.put( 0, preamble );
            lengths.put( 0, preamble.remaining());

            strings.put( 1, source );
            lengths.put( 1, source.remaining());

            glShaderSource( shader, strings, lengths );
            MemoryUtil.memFree( lengths );
            MemoryUtil.memFree( strings );
        }

        glCompileShader( shader );
        int compiled = glGetShaderi( shader, GL_COMPILE_STATUS );
        String shaderLog = glGetShaderInfoLog( shader );
        if ( shaderLog.trim().length() > 0 ) 
        {
            System.err.println( shaderLog );
        }
        if ( compiled == 0 ) 
        {
        	// rdb added "resource" to message
            throw new AssertionError( "Could not compile shader: " + resource );
        }
        return shader;
    }
}
