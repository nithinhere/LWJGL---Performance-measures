/**
 * Scene class encapsulates the key elements of a Scene:

 *           ArrayList of Shape3D objects
 *           ArrayList of Lights that are in the Scene
 *           Scene transformation information
 *           
 * @author rdb
 * Created 11/03/15
 */
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import java.nio.FloatBuffer;
import java.util.*;

import org.joml.*;

public class Scene
{
    //--------------------- class variables ---------------------------------
    //--------------------- instance variables ------------------------------
    protected ArrayList<Shape3D> shapes;
    protected Matrix4f           sceneTransform;
    protected int                shaderProgram;  // shader program id
    private   boolean            sceneTransformChanged = true;

    
    //------- transformation parameters
    protected float      xRadians = 0;
    protected float      yRadians = 0;
    protected float      zRadians = 0;
    protected Vector3f   location = new Vector3f();
    protected Vector3f   scale = new Vector3f( 1, 1, 1 );
    
    private float deltaRotate = 5;
    
    //----------------------- constructor ----------------------------------
    /**
     * Create a Scene.
     */
    public Scene()
    {
        shapes = new ArrayList<Shape3D>();
    }
    //------------------- addShape( Shape3D ) --------------------------------
    /**
     * Add a shape to this scene.
     * @param shape Shape3D
     */
    public void addShape( Shape3D shape )
    {
        shapes.add( shape );
    }
    //------------------- redraw( ) --------------------------------
    /**
     * Update scene-related uniform variables and regenerate all shapes in the Scene.
     */
    public void redraw()
    {
        if ( sceneTransformChanged )
            updateSceneTransform();
        for ( Shape3D shape: shapes )
            shape.redraw();
    }
    //------------------ setRotateX( angle ) ---------------------------
    /**
     * Set rotation about x to specified angle.
     * @param alpha  rotation about x of alpha degrees.
     */
    public void setRotateX( float alpha )
    {
        xRadians = (float) java.lang.Math.toRadians( alpha );
        sceneTransformChanged = true;
    }
    //------------------ setRotateY( angle ) ---------------------------
    /**
     * Set rotation about y to specified angle.
     * @param alpha  rotation about y of alpha degrees.
     */
    public void setRotateY( float alpha )
    {
        yRadians = (float) java.lang.Math.toRadians( alpha );
        sceneTransformChanged = true;
    }
    //------------------ setRotateZ( angle ) ---------------------------
    /**
     * Set rotation about z to specified angle.
     * @param alpha  rotation about z of alpha degrees.
     */
    public void setRotateZ( float alpha )
    {
        zRadians = (float) java.lang.Math.toRadians( alpha );
        sceneTransformChanged = true;
    }
    //------------------ setRotateX( delta ) ---------------------------
    /**
     * Update current x rotation  by specified increment/decrement.
     * @param alpha  rotation changed by delta degrees.
     */
    public void rotateX( float delta )
    {
        xRadians += (float) java.lang.Math.toRadians( delta );
        sceneTransformChanged = true;
    }
    //------------------ setRotateY( delta ) ---------------------------
    /**
     * Update current y rotation  by specified increment/decrement.
     * @param alpha  rotation changed by delta degrees.
     */
    public void rotateY( float delta )
    {
        yRadians += (float) java.lang.Math.toRadians( delta );
        sceneTransformChanged = true;
    }
    //------------------ setRotateZ( delta ) ---------------------------
    /**
     * Update current z rotation  by specified increment/decrement.
     * @param alpha  rotation changed by delta degrees.
     */
    public void rotateZ( float delta )
    {
        zRadians += (float) java.lang.Math.toRadians( delta );
        sceneTransformChanged = true;
    }
    //------------------ updateSceneTransform --------------------------
    /**
     * A scene transformation is a modeling transform applied to all objects
     * in the scene prior to the viewing transformation. 
     * It is a convenient way to see a 3D scene without resorting to a complete 
     * interactive viewing transformation.
     * 
     * This scene transformation only applies a rotations: 1 about each axis,
     * using joml library methods.
     */
    public void updateSceneTransform()
    {
        LWJGL.sceneMatrix.identity();
        LWJGL.sceneMatrix.rotateX( xRadians )
                         .rotateY( yRadians )
                         .rotateZ( zRadians );        
    }
}
