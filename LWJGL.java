import org.joml.Matrix4f;

/**
 * LWJGL.java encapsulates key "global" LWJGL-related information for this
 *     application. The class implements the Holder pattern.
 * 
 * All fields are package protected.
 * 
 * @author rdb
 */
public class LWJGL
{
    static int shaderProgram;
    
    // Key transformations needed during scene creation.
    static Matrix4f modelMatrix = null;  // excluding scene transform
    static Matrix4f viewMatrix = null;
    static Matrix4f sceneMatrix = null; 
    static Matrix4f smMatrix = null;     // scene*model
    static Matrix4f vsmMatrix = null;     // view*scene*model == opengl ModelView  
    
    static Matrix4f projectionMatrix = null;  // projection
    static Matrix4f pvsmMatrix = null;   // projection*view*scene*model

    static Matrix4f glNormalMatrix = new Matrix4f(); // needed to transform normals
}
