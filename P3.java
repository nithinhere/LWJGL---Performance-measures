/**
 * P3.java - Framework for comparing the performance of using

 *                 various implementation options.
 *            
 * @author rdb
 * 11/12/15 version 1.0 derived from previous demos.
 * 11/27/16 version 1.1 edited to conform to LWJGL 3.1
 *             
 * This program makes use of code from demos found at lwjgl.org accessed as
 * lwjgl3-demo-master and downloaded in late August 2015. It also uses a
 * modified complete class from that package, UtilsLWJGL.
 */
import org.lwjgl.glfw.*;

import org.lwjgl.opengl.*;
import org.joml.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.PointerBuffer;

import java.nio.*;
import java.io.*;
import java.util.*;
import java.io.*;

public class P3
{
    //---------------------- class variables -------------------------
    //-------- timing information
    private static int   redrawCount = -1;
    private static float redrawSum   = 0.0f;
    private static long  lastReport;     // last time average time reported
    private static long  reportInterval = 3000; // 3 seconds
    private static float reportIntervalSecs = reportInterval / 1000.0f;
    private static int   maxBatchReports = 10;  // max reports in batch mode
    private static int   numReports = 0;        // # reports generated
 
    private static boolean batchRun = false;
    
    private static PrintWriter logger = null;
    
    //---------------------- instance variables ----------------------
    // window size parameters
    int windowW = 800;
    int windowH = 740;
        
    // We need to strongly reference callback instances.
    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback   keyCallback;
    
    // The window handle
    private long window;

    private SceneManager sceneMgr; 
        
    //--------------- Constructor ------------------------------------------
    public P3()
    {
        // Setup error callback to print to System.err.
    	//   Make this call prior to openWindow.
        errorCallback = GLFWErrorCallback.createPrint( System.err ).set();
        
        String windowTitle = this.getClass().getName();
        window = UtilsLWJGL.openWindow( windowTitle, windowW, windowH );

        // rdb: true => forward compatible;  do not use forward compatibility
        GL.createCapabilities( false ); 

        String glv = glGetString( GL_VERSION );
        System.err.println( "After window creation: " + glv );
        
        try 
        {
            LWJGL.shaderProgram = UtilsLWJGL.makeShaderProgram( "evalDemo" );
            glUseProgram( LWJGL.shaderProgram );
        } 
        catch ( IOException iox )  
        {
            System.err.println( "Shader construction failed." );
            System.exit( -1 );
        }
        
        sceneMgr = new SceneManager();
        setupKeyHandler();
        
        renderLoop();
            
        // Clean up GLFW stuff
        glfwFreeCallbacks( window );
        glfwDestroyWindow( window );
        glfwSetErrorCallback( null ).free();
        glfwTerminate();
    }
    
    //--------------------- setupKeyHandler ----------------------
    /**
     * This setupKeyHandler just invokes keyHandler in SceneManager.
     */
    private void setupKeyHandler()
    {
        // Setup a key callback. It is called every time a key is pressed, 
        //      repeated or released.
        glfwSetKeyCallback( window, 
            keyCallback = new GLFWKeyCallback()
            {
                @Override
                public void invoke( long window, int key, 
                                    int code, int action, int mods )
                {
                    sceneMgr.keyHandler( window, key, code, action, mods );
                }
            });
    }
    //-------------------------- loop ----------------------------
    /**
     * Loop until user closes the window or kills the program.
     */
    private void renderLoop() 
    {
        // set up common opengl characteristics
        glEnable( GL_DEPTH_TEST );
        glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
        glClearDepth( 1.0f );
 
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( glfwWindowShouldClose( window ) == false )
        {
            // clear the framebuffer
            glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT ); 

            // redraw the frame
            redraw();
            
            sceneMgr.sceneRotateZ();

            glfwSwapBuffers( window ); // swap the color buffers
            glfwPollEvents();  // for these tests, polling is important!
        }
    }
    //------------------------ redraw() ----------------------------
    void redraw()
    {
        long start = System.currentTimeMillis();
        sceneMgr.redraw();
        
        glFlush();
        long end = System.currentTimeMillis();
        float redrawSecs = ( end - start ) / 1000.0f;
        if ( redrawCount == -1 )
        {
            log( "Initial redraw: " + redrawSecs );
            redrawCount = 0;   // next redraw we'll start counting
            lastReport = end;
        }
        else
        {
            redrawCount++;
            redrawSum += redrawSecs; 
        }
        if ( end - lastReport > reportInterval )
        {
            float avg = redrawSum / redrawCount;
            float frameRate = redrawCount / reportIntervalSecs;
            log( String.format( "Average redraw (sec): %6.4f    %8.3f FPS", 
                                                 avg, frameRate ));
            lastReport = end;
            redrawCount = 0;
            redrawSum = 0;
            numReports++;
        } 
        if ( batchRun && numReports >= maxBatchReports )
        {
            log( "========= batch termination: time expired =================" );
            glfwSetWindowShouldClose( window, true );
        }
    }
    //-------------------------- log ----------------------------------
    /**
     * log test messages to standard out and a log file, if it was created.
     */
    public static void log( String line )
    {
        System.out.println( line );
        if ( logger != null )
            logger.println( line );
    }
    //-------------------------- logErr ----------------------------------
    /**
     * log error messages to standard out and a log file, if it was created.
     */
    public static void logErr( String line )
    {
        line = "****Error**** " + line;
        System.err.println( line );
        if ( logger != null )
            logger.println( line );
    }
    //----------------------- initializeTesting -------------------------------
    /**
     * Setup the testing framework; open the log file, write some header info.
     */
    public static void initializeTesting( String args[] )
    {
        if ( args.length > 0 )  // first argument is # objects to create
        {
            try 
            { 
                SceneManager.numObjects = Integer.parseInt( args[0] ); 
            } 
            catch ( NumberFormatException e ) 
            {
                System.err.println( "Argument" + args[0] + " must be int; ignoring.");
            }
            if ( args.length > 1 )
                SceneManager.configCode = args[ 1 ];
        }
        String outFileName = "P3-" + SceneManager.configCode + "-"
                                        + SceneManager.numObjects + ".txt";
        try
        {
            logger = new PrintWriter( new FileOutputStream( outFileName ), true );
        }
        catch ( IOException ioe )
        {
            System.err.println( "**** Unable to open out file: " + outFileName +
                                "  ---- Logging only to standard output." );
        }
        
        //------- set run batch option
        batchRun = SceneManager.configCode.contains( "rb" );
        
        String line = String.format( "--------------------------------------\n" 
                          + "P3 Evaluation test: #obj: %6d  configCode: %s", 
                          SceneManager.numObjects, SceneManager.configCode );
        log( line );
    }
       
    //------------------------- main ----------------------------------
    /**
     * main constructions the object, invokes init and terminates.
     */
    public static void main( String args[] )
    {
        initializeTesting( args );  // setup the testing environment
        try
        {
            P3 demo = new P3();
        }
        catch ( Exception ex )
        {
            System.err.println( "Exception: " + ex.getClass().getName() 
                                              + ex.getMessage() );
            ex.printStackTrace( System.err );
        }
        finally 
        {
            // close the logging file; esp. important if program crashes.
            logger.close();
        }
    }
}
