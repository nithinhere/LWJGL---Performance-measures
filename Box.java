
/**
 * Box.java - a class implementation representing a Box object
 *           in OpenGL
 * Oct 16, 2013
 * rdb - derived from Box.cpp
 * 
 * 10/28/14 rdb - revised to explicitly draw faces
 *              - drawPrimitives -> drawObject( GL2 )
 *              - uses glsl
 * 11/10/14 rdb - existing rebuilds glsl buffers on every redraw.
 *                should and can only do it once.
 * 12/28/16 rdb - Converted from BufferUtils to MemoryUtil; no discernible 
 *                performance improvement up to 50000 objs at about 4.3 fps
 *                on my laptop. This may not be a useful test.
 * 03/09/16 rdb - moved everything but constructor to Shape3D
 */

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.*;

import org.lwjgl.system.MemoryUtil;

public class Box extends Shape3D {
	// --------- class variables -----------------

	// ++++++++++++++++++++ DrawArrays declarations +++++++++++++++++++++++++
	// vertex coordinates
	private static int nVertices = 36;
	private static float[] positions = { // 3-element coordinates for corners
											// identified with
											// 3 letter codes: [lr][bt][nf]
											// left/right bottom/top near/far

			// right face 2 triangles: rbn, rbf, rtf and rbn, rtf, rtn
			0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
			0.5f,
			// top face: ltn, rtn, rtf and ltn, rtf, ltf
			-0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f,
			-0.5f,
			// back face: rbf, lbf, ltf and rbf, ltf, rtf
			0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f,
			0.5f, -0.5f,
			// left face: lbf, lbn, ltn and lbf, ltn, ltf -- corrected
			-0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f,
			0.5f, -0.5f,
			// bottom face: lbf, rbf, rbn and lbf, rbn, lbn
			-0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f,
			-0.5f, 0.5f,
			// front face 2 triangles: lbn, rbn, rtn and lbn, rtn, ltn
			-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f,
			0.5f, };
	// ------------
	// Define normals to each vertex that are normal to the plane of face
	private float[] faceNormals = { // 3-element homog coordinates;
									// 3 letter codes are cube corners

			// right face 2 triangles: rbn, rbf, rtf and rbn, rtf, rtn
			1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f,
			// top face: ltn, rtn, rtf and ltn, rtf, ltf
			0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f,
			// back face: rbf, lbf, ltf and rbf, ltf, rtf
			0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
			// left face: lbf, lbn, ltn and lbf, ltn, ltf
			-1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f,
			// bottom face: lbf, rbf, rbn and lbf, rbn, lbn
			0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f,
			// front face 2 triangles: lbn, rbn, rtn and lbn, rtn, ltn
			0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, };
	// Normals to each vertex that are average of the shared face normals,
	// which is the same direction as the corner vertex position coords
	// This does not produce unit normals, so cpu and shader code needs
	// to normalize.
	private float[] vertexNormals = positions;

	// Now define vertex faceColors; includes alpha coordinate
	private float faceColors[] = {
			// right face: red
			1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f,
			// top face: green
			0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f,
			// back face: magenta
			1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f,
			// left face: cyan
			0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f,
			// bottom face: yellow
			1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f,
			// front face: blue
			0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, };
	// Now define vertex Colors:
	// 8 colors for [lr][bt][nf] left/right bottom/top near/far
	// lbn = red, lbf = green, ltn = blue, ltf = cyan,
	// 1f, 0f, 0f 0f, 1f, 0f 0f, 0f, 1f 0f, 1f, 1f
	// rbn = pink, rbf = magenta, rtn = paleblue, rtf = yellow
	// 1f, 0.5f, 0.5f 1f, 0f, 1f 0.5f, 0.5f, 1f 0f, 1f, 1f
	private float vertexColors[] = {
			// right face: rbn, rbf, rtf and rbn, rtf, rtn
			1f, 0.5f, 0.5f, 1f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 1f, 0.5f, 0.5f, 1f, 1f, 1f, 0f, 1f, 0.5f, 0.5f, 1f, 1f,
			// top face: ltn, rtn, rtf and ltn, rtf, ltf
			0f, 0f, 1f, 1f, 0.5f, 0.5f, 1f, 1f, 1f, 1f, 0f, 1f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 0f, 1f, 1f, 1f,
			// back face: rbf, lbf, ltf and rbf, ltf, rtf
			1f, 0f, 1f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f, 0f, 1f,
			// left face: lbf, lbn, ltn and lbf, ltn, ltf
			0f, 1f, 0f, 1f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 1f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 1f,
			// bottom face: lbf, rbf, rbn and lbf, rbn, lbn
			0f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 0.5f, 0.5f, 1f, 0f, 1f, 0f, 1f, 1f, 0.5f, 0.5f, 1f, 1f, 0f, 0f, 1f,
			// front face: lbn, rbn, rtn and lbn, rtn, ltn
			1f, 0f, 0f, 1f, 1f, 0.5f, 0.5f, 1f, 0.5f, 0.5f, 1f, 1f, 1f, 0f, 0f, 1f, 0.5f, 0.5f, 1f, 1f, 0f, 0f, 1f,
			1f, };

	// +++++++++++++++++++++++++ DrawElements declarations ++++++++++++++++++++
	// ---------
	private static int nObjVertices = 8;
	// Define corner vertices near face first, then far face
	private static float[] vertices = {
			// lbn (0) rbn (1) rtn (2) ltn (3)
			-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
			// lbf (4) rbf (5) rtf (6) ltf (7)
			-0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f };
	// Define indexes
	private static byte[] indexes = {
			// rbn, rbf, rtf and rbn, rtf, rtn === Right face
			1, 5, 6, 1, 6, 2,
			// ltn, rtn, rtf and ltn, rtf, ltf === Top face
			3, 2, 6, 3, 6, 7,
			// rbf, lbf, ltf and rbf, ltf, rtf === Far face
			5, 4, 7, 5, 7, 6,
			// lbf, lbn, ltn and lbf, ltn, ltf === Left face
			4, 0, 3, 4, 3, 7,
			// lbf, rbf, rbn and lbf, rbn, lbn === Bottom face
			4, 5, 1, 4, 1, 0,
			// lbn, rbn, rtn and lbn, rtn, ltn === Front face
			0, 1, 2, 0, 2, 3, };
	// Define object vertex colors; near face first than far face
	private static float[] indexColors = {
			// lbn = red, rbn = green, rtn = blue, ltn = cyan,
			1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 1f, 1f,
			// lbf = pink, rbf = magenta, rtf = paleblue, ltf = yellow
			1f, 0.5f, 0.5f, 1f, 0f, 1f, 0.5f, 0.5f, 1f, 0f, 1f, 1f };

	// ------------- constructor -----------------------
	/**
	 * Construct the data for this box object. useFaceColors true => each box
	 * face gets a color else each vertex gets a color. useFaceNormals true =>
	 * each triangle vertex normal is its face normal else vertex normal is
	 * average of face normals of faces that share that corner
	 */
	public Box(boolean useFaceColors, boolean useFaceNormals) {
		UtilsLWJGL.glError("--->Box.ctor"); // clean out old errors
		float[] textureCoords = null;
		float[] normals = null;
		float[] colors = null;

		if (Shape3D.useElements) // using glDrawElements
		{
			normals = vertices; // aren't unit normals! Need to normalized
			colors = indexColors;
			setData(nObjVertices, vertices, indexes, normals, colors, textureCoords);
		} else // using glDrawArrays
		{
			normals = faceNormals;
			if (!useFaceNormals)
				normals = vertexNormals;
			colors = faceColors;
			if (!useFaceColors)
				colors = vertexColors;
			setData(nVertices, positions, normals, colors, textureCoords);

		}
		UtilsLWJGL.glError("---Box.ctor"); // clean out old errors
	}
}