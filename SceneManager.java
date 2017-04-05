
/**
 * SceneManager - encapsulates the creation and management of the Scenes.

 *          This class could use the Singleton pattern.
 *          
 *           This version's primary goal is to demonstrate texture mapping.
 *           Many features in other demo SceneManager classes are not present here.
 *               - no scene class; only one scene needed
 *               - only one object used: a thin Box object; we'll map texture on front
 *                 and back faces only.
 *           Light will not be used -- at least not in first version.
 *           
 * @author rdb
 * Created 11/12/15 from TextureDemo.SceneManager.
 * 11/27/16 rdb: edit to conform to LWJGL 3.1
 * 12/28/15 rdb: convert from BufferUtils to MemoryUtil
 */

import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.opengl.GL11.*;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUniform1f;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

import java.nio.FloatBuffer;
import java.util.*;
import java.io.*;
import java.nio.*;

public class SceneManager {
	// ---------------------- class variables --------------------------
	public static int numObjects = 5000;
	// configCode has multiple fields each ending in ".". First letter of each
	// field
	// is a code indicate a test "variable", the second defines the option.
	// The first option is default if the code is not present in the String:
	// b = buffers can be bsj, bsa, buj, or bua
	// sj shared by instances with position/normal data joined in 1 buffer
	// sa shared by instances, but position and normal are "a"part
	// uj unshared buffers but position/normal data joined in 1 buffer
	// ua unshared buffers with position and normal in separate buffers
	// d = draw mode; options: a glDrawArrays, e glDrawElements
	// m = PSV matrix calculation; options: c cpu, g gpu
	// a = vertex attribute order: options b blocked, i interleaved
	// c = vertex coordinate size: options 3 threeD, 4 homogeneous coord
	// l = lighting model: options c cpu, v vertex shader, f fragment shader
	// bt = add texture coordinates to all of the specified buffer options.
	// Codes may be added, but this code can ignore any entries that it.
	// doesn't support.
	public static String configCode = "bua.da.mc."; // unshared apart buffers,
													// glDrawArrays
													// matrix mul in gpu
	// -------- timing information
	public static int redrawCount = -1;
	public static float redrawSum = 0.0f;
	public static long lastReport; // time of last time report
	public static long reportInterval = 3000; // 3 seconds

	// -------- rotation step
	public static float deltaRotate = 3.0f; //

	// ---------------------- instance variables --------------------------
	private ArrayList<Scene> allScenes;
	private Scene curScene = null;
	private int curSceneIndex = 0;

	private boolean autoRotation = false;

	// --------- viewing parameters
	// private Vector3f eye = new Vector3f( 4, 4, 2 );
	private Vector3f eye = new Vector3f(0, 0, 10);
	private Vector3f center = new Vector3f(0, 0, 0);
	private Vector3f up = new Vector3f(0, 1, 0);

	// -------- perspective parameters
	private float fovyDegrees = 40;
	private float aspect = 1;
	private float near = 0.1f;
	private float far = 20;

	// -------- ortho parameters
	private float left = -2;
	private float right = 2;
	private float bottom = -2;
	private float top = 2;
	private float nearZ = 0.1f;
	private float farZ = 40;

	// --------- buffers/textures
	private FloatBuffer projXsceneBuf; // MemoryUtil allocation!
	// ------ buffers for matrix ---
	private FloatBuffer projBuf;
	private FloatBuffer sceneBuf;
	private FloatBuffer viewBuf;
	// ----- Flag for mc/ mg--------
	int flag = 0;

	// ------------------ constructor ------------------------
	/**
	 * Create views and make the scene.
	 */
	public SceneManager() {
		UtilsLWJGL.glError("--->SceneManger.ctor"); // clean out old errors
		setupTestOptions();

		allScenes = new ArrayList<Scene>();

		setupView();
		updateView();

		allScenes.add(makeScene(numObjects));
		curScene = allScenes.get(curSceneIndex);
		UtilsLWJGL.glError("<---SceneManger.ctor"); // clean out old errors
	}

	// ------------------- finalize -----------------------------
	/**
	 * setupView creates a Buffer with MemoryUtil; although SceneManager only
	 * has one instance, it's still a good idea to acknowledge the C++ like
	 * allocation and free it on cleanup.
	 */
	public void finalize() {
		MemoryUtil.memFree(projXsceneBuf);
	}

	// ------------------ setupTestOptions() ----------------------
	/**
	 * Parse the test options code string and set appropriate flags.
	 * 
	 */
	private void setupTestOptions() {
		// ------ buffer options--------------------------------
		Shape3D.sharedJointBuffers = configCode.contains("bsj");
		Shape3D.sharedApartBuffers = configCode.contains("bsa");
		Shape3D.unSharedJointBuffers = configCode.contains("buj");
		Shape3D.unSharedApartBuffers = configCode.contains("bua");
		Shape3D.textureCoordsShared = configCode.contains("tb");

		// ------- draw options--------------------------------
		Shape3D.useElements = configCode.contains("de");
		Shape3D.useTriangleStrips = configCode.contains("ds");

		// ---------optional features -1-----------------------
		// ------------ blocked and interleaved
		Shape3D.interleaved = configCode.contains("ai");
		Shape3D.blocked = configCode.contains("ab");
		// ---------optional features -1-----------------------
		// -------------- c3, c4 -----------
		Shape3D.useThree = configCode.contains("c3");
		Shape3D.useFour = configCode.contains("c4");

		// ----------PSV multiply------------------------------
		//
		Shape3D.PSV_mc = configCode.contains("mc");
		Shape3D.PSV_mg = configCode.contains("mg");

		// ------- other settings need to be done for other tests -------
	}

	// ------------------ makeScene --------------------------
	/**
	 * Create the objects that make up the scene.
	 * 
	 * @param n
	 *            int n is number of boxes to generate
	 */
	private Scene makeScene(int n) {
		UtilsLWJGL.glError("--->SceneManger.makeScene"); // clean out old errors
		long start = System.currentTimeMillis();
		Random rng = new Random(1);

		Scene scene = new Scene();
		float minSize = 0.05f;
		float deltaSize = 0.08f;
		float minXYZ = -1;
		float deltaXYZ = 1.9f;

		Box box = null;

		for (int i = 0; i < n; i++) {
			// 1st arg to Box: true: vertex color is face color;
			// false vertex has own color.
			// 2nd arg to Box: true: vertex normal is face normal
			// false: vertex normal is avg of shared face normals
			box = new Box(rng.nextBoolean(), rng.nextBoolean());
			float size = minSize + rng.nextFloat() * deltaSize;
			box.setSize(size, size, size);
			float x = minXYZ + rng.nextFloat() * deltaXYZ;
			float y = minXYZ + rng.nextFloat() * deltaXYZ;
			float z = minXYZ + rng.nextFloat() * deltaXYZ;
			box.setLocation(x, y, z);
			// rotate random amount around y axis
			box.setRotate(rng.nextFloat() * 360, 0, 1, 0);
			scene.addShape(box);
		}
		long elapsedMillis = System.currentTimeMillis() - start;
		float elapsedSecs = (float) elapsedMillis / 1000.0f;
		System.err.println("Scene creation time: " + elapsedSecs);
		UtilsLWJGL.glError("<---SceneManger.makeScene"); // clean out old errors
		return scene;
	}

	// --------------------- keyHandler ---------------------------
	/**
	 * Make this a full-fledged method called from the invoke method of the
	 * anonymous class created in setupKeyHandler.
	 * 
	 * @param long
	 *            window window Id
	 * @param int
	 *            key key code
	 * @param int
	 *            code "scancode" is low-level non-standard internal code
	 * @param int
	 *            action GLFW_PRESS or GLFW_RELEASE
	 * @param int
	 *            mods bits in int encode modifier keys pressed GLFW_MOD_ALT |
	 *            GLFW_MOD_SHIFT | GLFW_MOD_CONTROL | GLFW_MOD_SUPER (cmd on
	 *            mac)
	 */
	public void keyHandler(long window, int key, int code, int action, int mods) {
		if (curScene == null)
			return;
		switch (key) {
		case GLFW.GLFW_KEY_PERIOD: // next scene
			if (action == GLFW.GLFW_PRESS) {
				curSceneIndex++;
				if (curSceneIndex >= allScenes.size())
					curSceneIndex = 0;
				curScene = allScenes.get(curSceneIndex);
			}
			updateView();
			break;
		case GLFW.GLFW_KEY_COMMA: // prev scene
			if (action == GLFW.GLFW_PRESS) {
				curSceneIndex--;
				if (curSceneIndex < 0)
					curSceneIndex = allScenes.size() - 1;
				curScene = allScenes.get(curSceneIndex);
			}
			updateView();
			break;
		case GLFW_KEY_DOWN:
			if (action == GLFW.GLFW_REPEAT || action == GLFW.GLFW_PRESS)
				curScene.rotateX(deltaRotate);
			updateView();
			break;
		case GLFW_KEY_UP:
			if (action == GLFW.GLFW_REPEAT || action == GLFW.GLFW_PRESS)
				curScene.rotateX(-deltaRotate);
			updateView();
			break;
		case GLFW.GLFW_KEY_LEFT:
			if (action == GLFW.GLFW_REPEAT || action == GLFW.GLFW_PRESS)
				curScene.rotateY(-deltaRotate);
			updateView();
			break;
		case GLFW.GLFW_KEY_RIGHT:
			if (action == GLFW.GLFW_REPEAT || action == GLFW.GLFW_PRESS)
				curScene.rotateY(deltaRotate);
			updateView();
			break;
		case GLFW.GLFW_KEY_ESCAPE:
		case GLFW_KEY_Q:
			// this is another exit key
			if (action == GLFW_RELEASE) // use release so user can change mind
				glfwSetWindowShouldClose(window, true);
			break;
		case GLFW.GLFW_KEY_SLASH:
			// Toggle the auto rotation
			if (action == GLFW_RELEASE) // use release so user can change mind
				autoRotation = !autoRotation;
			break;
		default:
			// System.out.println( "key: " + key );
		}
	}

	// --------------------- sceneRotateZ() -------------------------------
	/**
	 * For better feedback that redraws are happening, let main program specify
	 * an small z rotation for each frame.
	 */
	public void sceneRotateZ() {
		if (autoRotation) {
			float dAngle = 1.0f;
			curScene.rotateZ(dAngle);
			updateView();
		}
	}

	// --------------------- setupView -----------------------------------
	/**
	 * void setupView
	 */
	private void setupView() {
		UtilsLWJGL.glError("--->SceneManger.settupView"); // clean out old
															// errors
		LWJGL.modelMatrix = new Matrix4f(); // excluding scene transform
		LWJGL.viewMatrix = new Matrix4f();
		LWJGL.sceneMatrix = new Matrix4f();

		LWJGL.projectionMatrix = new Matrix4f();
		LWJGL.smMatrix = new Matrix4f(); // scene*model
		LWJGL.vsmMatrix = new Matrix4f(); // view*scene*model == ModelView
		LWJGL.pvsmMatrix = new Matrix4f(); // projection*view*scene*model

		projXsceneBuf = MemoryUtil.memAllocFloat(16);

		// Buffers uploaded to shader to multiply

		projBuf = MemoryUtil.memAllocFloat(16);
		sceneBuf = MemoryUtil.memAllocFloat(16);
		viewBuf = MemoryUtil.memAllocFloat(16);

		UtilsLWJGL.glError("<---SceneManger.setupView"); // clean out old errors
	}

	// ------------------ updateView --------------------------
	/**
	 * We have a constant viewing and projection specification. Can define it
	 * once and send the spec to the shader.
	 */
	void updateView() {
		UtilsLWJGL.glError("--->SceneManger.updateView"); // clean out old
															// errors
		float fovy = (float) Math.toRadians(fovyDegrees);
		if (curScene != null)
			curScene.updateSceneTransform();

		// set projection and viewing matrices based on current parameters
		// (These don't change in this program, but this is a model for
		// future programs as well.)
		//
		// Use ortho projection because of uncertainty of perspective in JOML.
		LWJGL.projectionMatrix.identity().ortho(left, right, bottom, top, nearZ, farZ);

		LWJGL.viewMatrix.identity().lookAt(eye, center, up);
		if (Shape3D.PSV_mc) {
			LWJGL.smMatrix.set(LWJGL.sceneMatrix).mul(LWJGL.modelMatrix);
			LWJGL.vsmMatrix.set(LWJGL.viewMatrix).mul(LWJGL.smMatrix);
			LWJGL.pvsmMatrix.set(LWJGL.projectionMatrix).mul(LWJGL.vsmMatrix);

			// Now create the glNormalMatrix = transpose( inverse(mv) )
			// for us vsmMatrix is the ModelView matrix
			LWJGL.glNormalMatrix.set(LWJGL.vsmMatrix).invert().transpose();

			// get stores this matrix into its argument -- a buffer in this case
			projXsceneBuf = LWJGL.pvsmMatrix.get(projXsceneBuf);

			// --- now push the composite into a uniform var in vertex shader
			// this id does not need to be global since we never change
			// projection or viewing specs in this program.
			int unif_pXv = glGetUniformLocation(LWJGL.shaderProgram, "projXview");

			glUniformMatrix4fv(unif_pXv, false, projXsceneBuf);
			UtilsLWJGL.glError("<---SceneManger.updateView"); // clean out old
			// projection, scene, view uploaded to shader
			// get stores this matrix into its argument -- a buffer in this case
		}

		// ------------- Matrix multiplication in gpu ------
		if (Shape3D.PSV_mg) {
			projBuf = LWJGL.projectionMatrix.get(projBuf);
			sceneBuf = LWJGL.sceneMatrix.get(sceneBuf);
			viewBuf = LWJGL.viewMatrix.get(viewBuf);

			// --- now push the composite into a uniform var in vertex shader
			int unif_p = glGetUniformLocation(LWJGL.shaderProgram, "proj");
			int unif_v = glGetUniformLocation(LWJGL.shaderProgram, "view");
			int unif_s = glGetUniformLocation(LWJGL.shaderProgram, "scene");
			int flag_shadercode = glGetUniformLocation(LWJGL.shaderProgram, "psv_flag");

			glUniformMatrix4fv(unif_p, false, projBuf);
			glUniformMatrix4fv(unif_v, false, viewBuf);
			glUniformMatrix4fv(unif_s, false, sceneBuf);
			glUniform1f(flag_shadercode, 1);
			UtilsLWJGL.glError("<---SceneManger.updateView"); // clean out old

		}

	}

	// ------------------------ redraw() -------------------------------
	/**
	 * Initiate scene redraw invocations.
	 */
	void redraw() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		if (curScene != null)
			curScene.redraw();
		glFlush();
	}
}
