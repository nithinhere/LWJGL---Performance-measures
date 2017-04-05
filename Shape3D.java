
/**
 * Shape3D.java - an abstract class representing an OpenGL graphical object

 *
 * 10/16/13 rdb derived from Shape3D.cpp
 * 09/28/15 rdb Revised for lwjgl              
 * 12/28/16 rdb Revised for lwjgl3.1; esp replace BufferUtils with * MemoryUtil
 */
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import org.joml.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

abstract public class Shape3D {
	// ------------------ class variables ------------------------------
	static final int MAX_COLORS = 8; // arbitrary number change it if need more
	// 6 is good for for Box faces,
	// 8 is good for Box vertex color assignment

	// these uniform variable specs need not be used by every object or
	// every shader program; but it makes sense for the class of
	// objects to share their uniform variables.
	//
	// ---- test flags -------------
	// These should be initialized to default values, unless the
	// code does not support the default, in which case initialize
	// to the "best" it does support. SceneManager defaults are
	// set to the target implementation and will override these.
	//
	static boolean sharedJointBuffers = false; // bsj code
	static boolean unSharedJointBuffers = false; // buj code
	static boolean sharedApartBuffers = false; // bsa code
	static boolean unSharedApartBuffers = false; // bua code
	static boolean textureCoordsShared = false; // tb code
	static boolean useTriangleStrips = false; // ts code

	static boolean useElements = false; // de
	// ----------- optional features 1 ----------------------
	static boolean interleaved = false; // ai
	static boolean blocked = false; // ab

	// ------------- optionalFeatures 2 ----------------------
	static boolean useThree = false; // c3
	static boolean useFour = false; // c4

	// ----------PSV multiply---------------------------------
	static boolean PSV_mc = false; // mc
	static boolean PSV_mg = false; // mg
	// uniform flag for psv
	protected static int psv_flag = -1; // Uniform id for matrix Flag

	static int shapeCount = 0; // used to limit impl warnings.
	protected static int uModel = -1; // uniform id for model matrix
	protected static int uColor = -1; // uniform id for color value

	// ------------------ instance variables common to all ---------------------
	// these uniform variable specs need not be used by every object or
	// every shader program; but it makes sense for the class of
	// objects to share their uniform variables.
	//
	private int shaderPgm = -1;
	private int vaoId = -1;

	private int nTriangles = -1;
	private int nVertices = -1; // #vertices in buffer (3*nTriangles)
	private int coordSize = 0; // 3 for xyz, 4 for xyzw
	private int normalSize = 0; // 3 for xyz, 4 for xyzw
	private int colorSize = 0; // 3 for xyz, 4 for xyzw
	private int vPosition = -1;
	private int vNormal = -1;
	private int vColor = -1;
	private int posSize = 0;

	// ------------- instance variables for separate buffers--------------
	// ------ vertex data and variables
	// private FloatBuffer coordBuffer = null;
	// private FloatBuffer normalBuffer = null;
	private FloatBuffer colorBuffer = null;
	private FloatBuffer coordBuffer = null;

	private int posVBO = -1;
	private int normalVBO = -1; // buffer for normals
	private int colorVBO = -1;
	// index variables for glDrawelements
	protected ByteBuffer indexbuffer = null;
	private int indexVBO = -1; // index VBO
	private int indexesCount = 0;
	private int indexsize = -1;
	protected ByteBuffer indexInfo = null; // ByteBuffer
	protected int noOfIndex = -1;

	// Shared Apart Buffer /bsa
	private static int posVBO_static = -1;
	private static int normalVBO_static = -1;
	private static int indexVBO_static = -1;

	// Joint Unshared Buffer /buj
	private static int combinedVBO = -1;
	private static int combinedVBO_static = -1;

	// interleaved Buffer
	private static int interleaveVBO = -1;

	// ------------------ object instance variables ----------------------------
	protected float xLoc, yLoc, zLoc; // location (origin) of object
	protected float xSize, ySize, zSize; // size of the object
	protected float angle, dxRot, dyRot, dzRot; // rotation angle and axis

	protected Matrix4f modelMatrix = new Matrix4f();
	protected boolean modelNeedsUpdate = true;

	protected Color[] colors = new Color[MAX_COLORS];

	protected FloatBuffer[] colorBufs = new FloatBuffer[MAX_COLORS];
	protected FloatBuffer modelBuf = MemoryUtil.memAllocFloat(16);

	/// -----Object counter for shared VBO
	private static int objectCounter = 0;
	private static int Counter = 0;

	// ------------------ Constructors ----------------------------------
	/**
	 * Create a new object3D at position 0,0,0 of size 1,1,1
	 */
	public Shape3D() {
		UtilsLWJGL.glError("--->Shape3D"); // clear old glerrors
		shapeCount++;
		shaderPgm = LWJGL.shaderProgram;
		for (int i = 0; i < colors.length; i++) // fill arrays with null
		{
			colors[i] = null;
			colorBufs[i] = null;
		}
		setColor(1, 0, 0);
		setLocation(0, 0, 0);
		setSize(1, 1, 1);
		setRotate(0, 0, 1, 0);

		psv_flag = glGetUniformLocation(shaderPgm, "psv_flag");

		// ------------- Setup GLSL interface variables -------------
		createGLSLvars(); // uniform variables needed by Shapes
		objectCounter++;
		Counter++;
		// psv_flag = glGetUniformLocation(shaderPgm, "psv_flag");

		UtilsLWJGL.glError("<---Shape3D"); // clean out old errors
	}

	// ------------------------ finalize -----------------------------
	/**
	 * The colBuffer object is allocated by MemoryUtil and is not garbage
	 * collected. So, we need to free that memory when the Shape3D object
	 * disappears.
	 */
	public void finalize() {
		MemoryUtil.memFree(colorBuffer);
	}

	// ------------------------- createGLSLvars ---------------------------
	/**
	 * Shape3D objects need some uniform variables in the shader program to help
	 * control the shader execution. These include:
	 */
	protected void createGLSLvars() {
		UtilsLWJGL.glError("--->Shape3D.createGLSLvars"); // clear old glerrors

		// Create a vertex array object and save as an instance variable
		vaoId = glGenVertexArrays();

		// create attribute location references
		vPosition = glGetAttribLocation(shaderPgm, "vPosition");
		vNormal = glGetAttribLocation(shaderPgm, "vNormal");
		vColor = glGetAttribLocation(shaderPgm, "vColor");

		// create glGenBuffers for each ease based on the boolean values
		// Unshared Apart Buffers
		if (Shape3D.unSharedApartBuffers) {
			posVBO = glGenBuffers();
			normalVBO = glGenBuffers();
			colorVBO = glGenBuffers();
			indexVBO = glGenBuffers();
			// shared Apart Buffers
		} else if (Shape3D.sharedApartBuffers) {
			if (Shape3D.objectCounter == 0) {
				Shape3D.posVBO_static = glGenBuffers();
				Shape3D.normalVBO_static = glGenBuffers();
				Shape3D.indexVBO_static = glGenBuffers();
			}
			colorVBO = glGenBuffers();
		}
		// Unshared jointbuffers
		else if (Shape3D.unSharedJointBuffers) {
			Shape3D.combinedVBO = glGenBuffers();
			colorVBO = glGenBuffers();
		}

		// Shared Joint buffers
		else if (Shape3D.sharedJointBuffers) {
			if (Shape3D.objectCounter == 0) {
				Shape3D.combinedVBO_static = glGenBuffers();
			}
			colorVBO = glGenBuffers();
		}

		// create uniform variables
		uModel = glGetUniformLocation(shaderPgm, "uModel");
		uColor = glGetUniformLocation(shaderPgm, "uColor");

		float[] rgba = colors[0].get4f(); // get rgba as an array
		glUniform4fv(uColor, rgba);

		UtilsLWJGL.glError("<---Shape3D.createGLSLvars"); // check for glerrors
	}

	// ++++++++++++++++++++++ public methods ++++++++++++++++++++++++++++++=
	// ------------ redraw() ----------------------------
	/**
	 * Update appropriate uniform variables that contain instance-specific
	 * values, such as model matrix. Draw the object using glsl and
	 * VertexBufferObjects with VAOs. We do not need to transfer the data; it's
	 * already there and the VAO has all the information needed for the GPU to
	 * process the glDrawArrays call.
	 */
	protected void redraw() {
		if (modelNeedsUpdate)
			updateModelMatrix();

		glUniformMatrix4fv(uModel, false, modelBuf);

		// identify which VAO specification needs to be drawn.
		glBindVertexArray(vaoId);
		if (Shape3D.useElements) {
			// draw elements
			// indexVbo for unshared incase of drawelemnts
			if (Shape3D.unSharedApartBuffers) {
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexVBO);
			}

			// indexVBO_static for shared apart buffers
			else if (Shape3D.sharedApartBuffers) {
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexVBO_static);
			}
			glDrawElements(GL_TRIANGLES, noOfIndex, GL_UNSIGNED_BYTE, 0);
			// draw arrays
		} else {
			glDrawArrays(GL_TRIANGLES, 0, nVertices);
		}

		// unbind the vao, we are done with it for now.
		glBindVertexArray(0);
	}

	// ----------------------- get/setLocation --------------------------------
	/**
	 * set location to the x,y,z position defined by the args
	 */
	public void setLocation(float x, float y, float z) {
		xLoc = x;
		yLoc = y;
		zLoc = z;
		modelNeedsUpdate = true;
	}

	/**
	 * return the value of the x origin of the shape
	 */
	public float getX() {
		return xLoc;
	}

	/**
	 * return the value of the y origin of the shape
	 */
	public float getY() {
		return yLoc;
	}

	/**
	 * return the value of the z origin of the shape
	 */
	public float getZ() {
		return zLoc;
	}

	/**
	 * return the location as a Point3f object
	 */
	public Vector3f getLocation() // return location as a Point
	{
		return new Vector3f(xLoc, yLoc, zLoc);
	}

	// ----------------------- get/setColor methods ---------------------------
	/**
	 * return the base color of the object
	 * 
	 * @return Color
	 */
	public Color getColor() // return color 0
	{
		return colors[0];
	}

	/**
	 * return any color of the object
	 * 
	 * @param i
	 *            index of color to retrieve
	 * @return Color
	 */
	public Color getColor(int i) // return color i
	{
		if (i >= 0 && i < MAX_COLORS)
			return colors[i];
		else
			return null; // should throw exception
	}

	/**
	 * set the "nominal" color of the object to the specified color; this does
	 * not require that ALL components of the object must be the same color.
	 * Typically, the largest component will take on this color, but the
	 * decision is made by the child class.
	 * 
	 * @param c
	 *            Color the color
	 */
	public void setColor(Color c) {
		setColor(0, c);
	}

	/**
	 * set the nominal color (index 0) to the specified color with floats
	 * 
	 * @param r
	 *            float red component
	 * @param g
	 *            float green component
	 * @param b
	 *            float blue component
	 */
	public void setColor(float r, float g, float b) {
		setColor(0, r, g, b, 1);
	}

	/**
	 * set the nominal color (index 0) to the specified color with floats
	 * 
	 * @param r
	 *            float red component
	 * @param g
	 *            float green component
	 * @param b
	 *            float blue component
	 * @param a
	 *            float alpha component
	 */
	public void setColor(float r, float g, float b, float a) {
		setColor(0, r, g, b, a);
	}

	/**
	 * set the index color entry to the specified color with floats
	 * 
	 * @param i
	 *            int which color index
	 * @param r
	 *            float red component
	 * @param g
	 *            float green component
	 * @param b
	 *            float blue component
	 */
	public boolean setColor(int i, float r, float g, float b) {
		return setColor(i, r, g, b, 1);
	}

	/**
	 * set the i-th color entry to the specified color with Color
	 * 
	 * @param i
	 *            int which color index
	 * @param r
	 *            float red component
	 * @param g
	 *            float green component
	 * @param b
	 *            float blue component
	 * @param a
	 *            float alpha component
	 */
	public boolean setColor(int i, float r, float g, float b, float a) {
		if (i < 0 || i > MAX_COLORS) // should throw an exception!
		{
			System.err.println("*** ERROR *** Shape3D.setColor: bad index: " + i + "\n");
			return false;
		}
		if (colors[i] == null)
			colors[i] = new Color(r, g, b, a); // put color at index
		else
			colors[i].setColor(r, g, b, a);

		// make buffer!
		colorBufs[i] = MemoryUtil.memAllocFloat(4);
		colorBufs[i].put(r).put(g).put(b).put(a).flip();
		return true;
	}

	/**
	 * set the i-th color entry to the specified color with Color
	 * 
	 * @param i
	 *            int which color index
	 * @param c
	 *            Color the color
	 */
	public boolean setColor(int i, Color c) {
		return setColor(i, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
	}

	// ------------------ setSize ----------------------------------------
	/**
	 * set the size of the shape to be scaled by xs, ys, zs That is, the shape
	 * has an internal fixed size, the shape parameters scale that internal
	 * size.
	 * 
	 * @param xs
	 *            float x Scale factor
	 * @param ys
	 *            float y Scale factor
	 * @param zs
	 *            float z Scale factor
	 * 
	 */
	public void setSize(float xs, float ys, float zs) {
		xSize = xs;
		ySize = ys;
		zSize = zs;
	}

	/**
	 * set the rotation parameters: angle, and axis specification
	 * 
	 * @param a
	 *            float angle of rotation
	 * @param dx
	 *            float x axis direction
	 * @param dy
	 *            float y axis direction
	 * @param dz
	 *            float z axis direction
	 */
	public void setRotate(float a, float dx, float dy, float dz) {
		angle = a;
		dxRot = dx;
		dyRot = dy;
		dzRot = dz;
		modelNeedsUpdate = true;
	}

	// ++++++++++++++++++++ protected methods +++++++++++++++++++++++++++++++
	// ---------------------- setData ---------------------------------------
	/**
	 * Take float arrays containing all the data needed to define the object.
	 * Any field except the vertex positions can be null indicating that that
	 * feature is not supported by this object.
	 * 
	 * This code does not support texture coords, but it should be easy to add.
	 * Individual components could be easily updated during execution for data
	 * has its own buffer; all it would take is to make the appropriate private
	 * "set" methods protected.
	 * 
	 * This implementation of setData only calls the "all separate" buffer
	 * model, but this is where you would decide which option needs to be done
	 * for this test and write the appropriate set subroutine.
	 *
	 * @param nVertices
	 *            int number vertices for all data fields, must be same
	 * @param positions
	 *            float[] xyz[w] floats for all vertices
	 * @param normals
	 *            float[] xyz[w] floats for all normals
	 * @param colors
	 *            float[] rgb[a] floats for all colors
	 * @param textureCoords
	 *            float[] rs[t] floats for all texture coords
	 */
	protected void setData(int nVertices, float[] positions, float[] normals, float[] colors, float[] textureCoords) {
		if (Shape3D.unSharedJointBuffers)
			setData_bsj(nVertices, positions, normals, colors, textureCoords);
		else if (Shape3D.sharedJointBuffers)
			setData_bsj(nVertices, positions, normals, colors, textureCoords);
		else if (Shape3D.unSharedApartBuffers)
			setDataUA(nVertices, positions, normals, colors, textureCoords);
		else if (Shape3D.sharedApartBuffers)
			setDataUA(nVertices, positions, normals, colors, textureCoords);

	}

	// -----------------------------Setdata for drawelements -------------
	/***
	 * Take float arrays containing all the data needed to define the object.
	 * Any field except the vertex positions can be null indicating that that
	 * feature is not supported by this object. This code does not support
	 * texture coords, but it should be easy to add. Individual components could
	 * be easily updated during execution for data has its own buffer; all it
	 * would take is to make the appropriate private "set" methods protected.
	 * 
	 * @param objectVertices
	 *            int objectNumber vertices for all data fields, must be same
	 * @param vertices
	 *            positions
	 * @param indexes
	 *            index array
	 * @param normals
	 * @param colors
	 * @param textureCoords
	 */
	protected void setData(int objectVertices, float[] vertices, byte[] indexes, float[] normals, float[] colors,
			float[] textureCoords) {
		if (Shape3D.unSharedApartBuffers)
			setDataUA(objectVertices, vertices, normals, colors, textureCoords, indexes);
		else if (Shape3D.sharedApartBuffers)
			setDataUA(objectVertices, vertices, normals, colors, textureCoords, indexes);
		// else if(Shape3D.unSharedJointBuffers)
		// if (Shape3D.unSharedJointBuffers)
		// setData_bsj(nVertices, positions, normals, colors, textureCoords);
		// else if (Shape3D.sharedJointBuffers)
		// setData_bsj(nVertices, positions, normals, colors, textureCoords);
		// else if (Shape3D.unSharedApartBuffers)
		// setDataUA(nVertices, positions, normals, colors, textureCoords);
		// else if (Shape3D.sharedApartBuffers)
		// setDataUA(nVertices, positions, normals, colors, textureCoords);
		// if (Shape3D.interleaved)
		// setDatainterleaved(nVertices, positions, normals, colors,
		// textureCoords);

	}

	// ++++++++++++++++++++ private methods +++++++++++++++++++++++++++++++
	// ---------------------- setDataUA -----------------------------------
	/**
	 * Unshared Apart Buffers; implements default buffer handling. bua Share
	 * shared Apart Buffers; bsa using same method for shared and unshared apart
	 */
	private void setDataUA(int nVerts, float[] pos, float[] norms, float[] colors, float[] texCoords, byte[] indexes) {
		setCoordData(nVerts, pos, indexes);
		setNormalData(nVerts, norms);
		setVertexColorData(nVerts, colors);

	}

	/**
	 * Unshared Apart Buffers; implements default buffer handling. bua Share
	 * Shared Apart Buffers; bsa using same method for shared and unshared apart
	 */
	// ---------------------- setDataUA - using draw
	// elements-----------------------------------
	private void setDataUA(int nVerts, float[] pos, float[] norms, float[] colors, float[] texCoords) {
		setCoordData(nVerts, pos);
		setNormalData(nVerts, norms);
		setVertexColorData(nVerts, colors);

	}

	// ---------------------setDatabsj----------------------------------------
	/***
	 * Joint Unshared Buffers; bsj Responsible for performing blocked as well as
	 * interleaved if not blocked and interleaved then joint buffer will take
	 * place
	 * 
	 * @param nVerts
	 * @param pos
	 * @param norms
	 * @param colors
	 * @param texCoords
	 */
	private void setData_bsj(int nVerts, float[] pos, float[] norms, float[] colors, float[] texCoords) {
		float[] combinedArray_ab = new float[pos.length + norms.length];
		float[] combinedArray_ai = new float[pos.length + norms.length];
		int p = 0;
		int p1 = 0;
		if (Shape3D.blocked) {
			// ---------blocked _combinedArray-------
			for (int i = 0; i < pos.length; i++) {
				combinedArray_ab[p++] = pos[i];
			}
			for (int i = 0; i < norms.length; i++) {
				combinedArray_ab[p++] = norms[i];
			}
		}

		// -----------interleaved _combinedArray --------
		else if (Shape3D.interleaved) {
			System.out.println("interleaved-----");
			int pi = 0;
			int ni = 0;
			int comp = 0;

			for (int index = 0; index < combinedArray_ai.length / 6; index++) {

				combinedArray_ai[comp++] = pos[pi++];
				combinedArray_ai[comp++] = pos[pi++];
				combinedArray_ai[comp++] = pos[pi++];

				combinedArray_ai[comp++] = norms[ni++];
				combinedArray_ai[comp++] = norms[ni++];
				combinedArray_ai[comp++] = norms[ni++];
				// System.out.println(combinedArray_ai[comp]);

			}

		}

		else {
			for (int i = 0; i < pos.length; i++) {
				combinedArray_ab[p1++] = pos[i];
			}
			for (int i = 0; i < norms.length; i++) {
				combinedArray_ab[p1++] = norms[i];
			}
		}

		if (Shape3D.blocked) {
			setCoordNormdata(nVerts, combinedArray_ab, pos, norms, colors, texCoords);
			setVertexColorData(nVerts, colors);

		} else if (Shape3D.interleaved) {
			setCoordNormdata(nVerts, combinedArray_ai, pos, norms, colors, texCoords);
			setVertexColorData(nVerts, colors);
			System.out.println("interleaved");

		} else {
			setCoordNormdata(nVerts, combinedArray_ab, pos, norms, colors, texCoords);
			setVertexColorData(nVerts, colors);
		}
	}

	/***
	 * Joint Unshared Buffers; bsj Private method takes combined Array of pos
	 * and norms call to loadbuffer created for joint buffers bsj and buj both
	 * uses the same method to setcoord and Normal data
	 */
	private void setCoordNormdata(int nVerts, float[] combinedArray, float[] pos, float[] norms, float[] colors,
			float[] texCoords) {
		FloatBuffer combinedBuffer = null;
		if (vPosition == -1)
			System.err.println("***** vPosition attribute is undefined!");
		combinedBuffer = MemoryUtil.memRealloc(combinedBuffer, combinedArray.length);
		coordSize = combinedArray.length / nVerts;
		posSize = pos.length / nVerts;

		nVertices = nVerts;
		nTriangles = nVerts / 3;
		if (vNormal == -1)
			System.err.println("***** vNormal attribute undefined!");
		normalSize = norms.length / nVerts;
		combinedBuffer.put(combinedArray).flip();
		// Check for buj, bsj
		if (unSharedJointBuffers)
			loadBuffer_joint(combinedBuffer, Shape3D.combinedVBO, vPosition, posSize, vNormal, normalSize, pos.length);
		else if (sharedJointBuffers)
			loadBuffer_joint(combinedBuffer, Shape3D.combinedVBO_static, vPosition, posSize, vNormal, normalSize,
					pos.length);

	}

	// ---------------------- setCoordData ----------------------------------
	/**
	 * Specify the vertex coordinate information for this object. Create and
	 * save a FloatBuffer with the information. A null first parameter discards
	 * previous vertex coord data
	 *
	 * @param nVerts
	 *            int number vertices
	 * @param coords
	 *            float[] array of coord positions to associate with each
	 *            vertex. If null is specified, deletes previous coords
	 */
	private void setCoordData(int nVerts, float[] coords) {
		FloatBuffer coordBuffer = null;
		// Could make buffer static and only allocate first time.
		if (coords == null) // Unused feature or redefining a shape
		{
			MemoryUtil.memFree(coordBuffer);
			coordBuffer = null;
			coordSize = 0;
			nTriangles = 0;
		} else {
			if (vPosition == -1)
				System.err.println("***** vPosition attribute is undefined!");
			coordBuffer = MemoryUtil.memRealloc(coordBuffer, coords.length);
			coordSize = coords.length / nVerts;
			nVertices = nVerts;
			nTriangles = nVerts / 3;
			coordBuffer.put(coords).flip();
		}
		if (Shape3D.unSharedApartBuffers) {
			loadBuffer(coordBuffer, posVBO, vPosition, coordSize);
		} else if (Shape3D.sharedApartBuffers) {
			loadBuffer(coordBuffer, Shape3D.posVBO_static, vPosition, coordSize);
		}
	}

	// ---------------------- setCoordData Draw Elements
	// ----------------------------------extra argument is indexs
	private void setCoordData(int nVerts, float[] coords, byte[] indexs) {

		if (coords == null || indexs == null) // Unused feature or redefining a
												// shape
		{
			MemoryUtil.memFree(coordBuffer);
			coordBuffer = null;
			coordSize = 0;
			nTriangles = 0;
			MemoryUtil.memFree(indexInfo);
			indexInfo = null;
			noOfIndex = 0;
			nTriangles = 0;
		} else {
			if (vPosition == -1)
				System.err.println("***** vPosition attribute is undefined!");
			coordBuffer = MemoryUtil.memRealloc(coordBuffer, coords.length);
			indexInfo = MemoryUtil.memRealloc(indexInfo, indexs.length);
			coordSize = coords.length / nVerts;
			noOfIndex = indexs.length;
			nVertices = nVerts;
			nTriangles = nVerts;
			coordBuffer.put(coords).flip();
			indexInfo.put(indexs).flip();

			// load buffer for draw elements
			if (Shape3D.unSharedApartBuffers) {

				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexVBO);
				glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexInfo, GL_STATIC_DRAW);
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
			} else if (Shape3D.sharedApartBuffers) {
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, Shape3D.indexVBO_static);
				glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexInfo, GL_STATIC_DRAW);
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
			}

		}
		if (Shape3D.unSharedApartBuffers) {
			loadBuffer(coordBuffer, posVBO, vPosition, coordSize);
		} else if (Shape3D.sharedApartBuffers) {
			loadBuffer(coordBuffer, Shape3D.posVBO_static, vPosition, coordSize);
		}

	}

	// ---------------------- setNormalData ----------------------------------
	/**
	 * Specify the vertex normal coord information for this object. Create and
	 * save a FloatBuffer with the information. A null first parameter discards
	 * previous normal data
	 *
	 * @param nVerts
	 *            int number of vertices
	 * @param normals
	 *            float[] array of coord positions to associate with each
	 *            vertex. If null is specified, deletes previous coords
	 */
	private void setNormalData(int nVerts, float[] normals) {
		FloatBuffer normalBuffer = null;
		// Could make buffer static and only allocate first time.
		if (normals == null) {
			MemoryUtil.memFree(normalBuffer);
			normalBuffer = null;
			normalSize = 0;
		} else {
			if (vNormal == -1)
				System.err.println("***** vNormal attribute undefined!");
			normalBuffer = MemoryUtil.memRealloc(normalBuffer, normals.length);
			normalSize = normals.length / nVerts;
			normalBuffer.put(normals).flip();
		}
		if (Shape3D.unSharedApartBuffers) {
			loadBuffer(normalBuffer, normalVBO, vNormal, normalSize);
		} else if (Shape3D.sharedApartBuffers) {
			loadBuffer(normalBuffer, Shape3D.normalVBO_static, vNormal, normalSize);
		}
	}

	// ---------------------- setVertexColorData
	// ----------------------------------
	/**
	 * Specify the vertex color information for this object. Create and save a
	 * FloatBuffer with the information. A null first parameter discards
	 * previous vertex coord data
	 *
	 * @param nVerts
	 *            int # of vertices to be colored
	 * @param colors
	 *            float[] array of color components to associate with each
	 *            vertex. If null is specified, deletes previous colors
	 */
	private void setVertexColorData(int nVerts, float[] colors) {

		// Could make buffer static and only allocate first time.
		if (colors == null) // Unused feature for (temporarily) emptying a shape
		{
			MemoryUtil.memFree(this.colorBuffer);
			colorBuffer = null;
			colorSize = 0;
			nTriangles = 0;
		} else {
			if (colorVBO == -1)
				System.err.println("***** vColor attribute undefined!");
			colorBuffer = MemoryUtil.memRealloc(colorBuffer, colors.length);
			colorSize = colors.length / nVerts;
			colorBuffer.put(colors).flip();
		}
		loadBuffer(colorBuffer, colorVBO, vColor, colorSize);
	}

	// ------------------ loadBuffer --------------------------------
	/**
	 * Send the specified buffer to its location in GPU.
	 * 
	 * @param buffer
	 *            FloatBuffer buffer containing floats to be down loaded.
	 * @param vbo
	 *            int id of vbo in gpu
	 * @param attrLoc
	 *            int id of attribute variable in shader
	 * @param attrSize
	 *            int number floats per attribute in buffer
	 */
	private void loadBuffer(FloatBuffer buffer, int vbo, int attrLoc, int attrSize) {
		if (buffer == null || attrLoc == -1)
			return;
		UtilsLWJGL.glError("--->loadBuffer"); // clean out errs
		glBindVertexArray(vaoId);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		// fill it with the data from the buffer
		glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

		// describe how vPosition data can be found in the current buffer
		glEnableVertexAttribArray(attrLoc);
		if (Shape3D.useFour)
			glVertexAttribPointer(attrLoc, 4, GL_FLOAT, false, 0, 0L);
		else
			glVertexAttribPointer(attrLoc, attrSize, GL_FLOAT, false, 0, 0L);

		glBindBuffer(GL_ARRAY_BUFFER, 0); // unbind the array buffer
		glBindVertexArray(0);
		UtilsLWJGL.glError("<---loadBuffer"); // clean out errs
	}

	/***
	 * Send the specified joint buffer to its location in GPU
	 * 
	 * @param buffer
	 *            /CombinedBuffer
	 * @param vbo
	 *            /Static or instance vbo
	 * @param attrLoc_pos
	 *            /vPosition
	 * @param attrLoc_norms
	 *            /vNormal
	 * @param attrSize_pos
	 * @param attrSize_norms
	 * @param coords
	 *            /pos.length
	 */
	private void loadBuffer_joint(FloatBuffer buffer, int vbo, int attrLoc_pos, int attrLoc_norms, int attrSize_pos,
			int attrSize_norms, int coords) {
		if (buffer == null || attrLoc_pos == -1)
			return;
		UtilsLWJGL.glError("--->loadBuffer"); // clean out errs
		glBindVertexArray(vaoId);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		// fill it with the data from the buffer
		glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

		// describe how vPosition data can be found in the current buffer
		glEnableVertexAttribArray(vPosition);
		glEnableVertexAttribArray(vNormal);
		glVertexAttribPointer(vPosition, 3, GL_FLOAT, false, 0, 0L);
		glVertexAttribPointer(vNormal, 3, GL_FLOAT, false, 0, coords * 4);
		glBindBuffer(GL_ARRAY_BUFFER, 0); // unbind the array buffer
		glBindVertexArray(0);
		UtilsLWJGL.glError("<---loadBuffer"); // clean out errs
	}

	// ----------------------- updateModelMatrix --------------------
	// --------------------------------
	/**
	 * Update the model matrix and its buffer.
	 */
	private void updateModelMatrix() {
		modelMatrix.identity();
		modelMatrix.translate(xLoc, yLoc, zLoc);
		modelMatrix.rotate(angle, dxRot, dyRot, dzRot);
		modelMatrix.scale(xSize, ySize, zSize);

		float[] modelFloats = new float[16];
		modelMatrix.get(modelFloats, 0);
		modelBuf.put(modelFloats).flip();
		modelNeedsUpdate = false;
	}

}
