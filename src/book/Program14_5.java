package book;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_RGBA;
import static com.jogamp.opengl.GL.GL_RGBA8;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_3D;
import static com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import helpers.Sphere;
import helpers.Torus;

import com.jogamp.common.nio.Buffers;

import org.joml.*;
import java.nio.*;
import javax.swing.*;
import java.lang.Math;

public class Program14_5 extends JFrame implements GLEventListener {
	// Setup OpenGL Graphics Renderer
	// for the GL Utility
	private GLCanvas myCanvas;
	private int renderingProgram, skyboxProgram;
	private int vao[] = new int[2];
	private int vbo[] = new int[6];
	private float cameraX, cameraY, cameraZ;
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();
	private Matrix4f vMat = new Matrix4f();
	private Matrix4f mMat = new Matrix4f();
	private Matrix4f mvMat = new Matrix4f();
	private int mvLoc, pLoc, vLoc;
	private float aspect;
//	private String vShaderSource = "vertShader5_1.glsl";
//	private String fShaderSource = "fragShader5_1.glsl";
	private String vShaderSource = "vertShader14_5.glsl";
	private String fShaderSource = "fragShader14_5.glsl";
	// Camera
	private float cameraYaw = 0.0f; // Góc quay trái/phải của camera
	private float cameraPitch = 0.0f; // Góc quay lên/xuống của camera
	private float cameraRoll = 0.0f; // Góc nghiêng ngang của camera (thường không sử dụng nhiều)
	private float cameraSpeed = 0.1f; // Tốc độ di chuyển của camera
	private Vector3f cameraPosition = new Vector3f(0.0f, 0.0f, 0.0f); // Vị trí của camera
	private Torus myTorus = new Torus(0.5f, 0.2f, 48);;
	private int numTorusVertices, numTorusIndices;
	private int brickTexture, skyboxTexture;

	// wave
	private int noiseTexture;
	private int noiseWidth = 256;
	private int noiseHeight = 256;
	private int noiseDepth = 256;
	double[][][] noise = new double[noiseWidth][noiseHeight][noiseDepth];
	Random random = new Random();

	/** Constructor to setup the GUI for this Component */
	public Program14_5() {
		setTitle("Chapter4 - program1a");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		// Camera
		myCanvas.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				switch (e.getKeyCode()) {
				case java.awt.event.KeyEvent.VK_LEFT:
					cameraYaw -= 2.0f; // Quay trái
					break;
				case java.awt.event.KeyEvent.VK_RIGHT:
					cameraYaw += 2.0f; // Quay phải
					break;
				case java.awt.event.KeyEvent.VK_UP:
					cameraPitch -= 2.0f; // Ngẩng lên
					break;
				case java.awt.event.KeyEvent.VK_DOWN:
					cameraPitch += 2.0f; // Nhìn xuống
					break;
				case java.awt.event.KeyEvent.VK_W:
					// Di chuyển camera theo hướng nhìn
					cameraPosition.z -= cameraSpeed * Math.cos(Math.toRadians(cameraYaw));
					cameraPosition.x -= cameraSpeed * Math.sin(Math.toRadians(cameraYaw));
					System.out.println("wwww");
					break;
				case java.awt.event.KeyEvent.VK_S:
					cameraPosition.z += cameraSpeed * Math.cos(Math.toRadians(cameraYaw));
					cameraPosition.x += cameraSpeed * Math.sin(Math.toRadians(cameraYaw));
					break;
				case java.awt.event.KeyEvent.VK_A:
					// Di chuyển camera sang trái
					cameraPosition.x -= cameraSpeed * Math.cos(Math.toRadians(cameraYaw));
					cameraPosition.z += cameraSpeed * Math.sin(Math.toRadians(cameraYaw));
					break;
				case java.awt.event.KeyEvent.VK_D:
					// Di chuyển camera sang phải
					cameraPosition.x += cameraSpeed * Math.cos(Math.toRadians(cameraYaw));
					cameraPosition.z -= cameraSpeed * Math.sin(Math.toRadians(cameraYaw));
					break;
				case java.awt.event.KeyEvent.VK_Q:
					cameraPosition.y += cameraSpeed; // Di chuyển lên trên
					break;
				case java.awt.event.KeyEvent.VK_E:
					cameraPosition.y -= cameraSpeed; // Di chuyển xuống dưới
					break;
				}
			}
		});
		// camera
		myCanvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			private int lastX, lastY;

			@Override
			public void mouseDragged(java.awt.event.MouseEvent e) {
				int deltaX = e.getX() - lastX;
				int deltaY = e.getY() - lastY;
				cameraYaw += deltaX * 0.1f; // Điều chỉnh độ nhạy của chuột
				cameraPitch += deltaY * 0.1f;
				lastX = e.getX();
				lastY = e.getY();
			}

			@Override
			public void mouseMoved(java.awt.event.MouseEvent e) {
				lastX = e.getX();
				lastY = e.getY();
			}
		});
		this.add(myCanvas);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		Animator animtr = new Animator(myCanvas);
		animtr.start();
		this.setVisible(true);

	}
	// ------ Implement methods declared in GLEventListener ------

	/**
	 * Called back immediately after the OpenGL context is initialized. Can be used
	 * to perform one-time initialization. Run only once.
	 */
	@Override
	public void init(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		renderingProgram = Utils.createShaderProgram(vShaderSource, fShaderSource);
		skyboxProgram = Utils.createShaderProgram("vertShader9_2.glsl", "fragShader9_2.glsl");
		setupVertices();
		// Camera position
		cameraX = 0.0f;
		cameraY = 2.0f;
		cameraZ = 10.0f;

		float fov = (float) Math.toRadians(60.0f);
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		float near = 0.1f;
		float far = 1000.0f;

		// Setup Projection Matrix
		pMat.identity();
		pMat.perspective(fov, aspect, near, far);

		// Setup View Matrix
		vMat.identity();
		vMat.lookAt(new Vector3f(cameraX, cameraY, cameraZ), new Vector3f(0.0f, 0.0f, 0.0f),
				new Vector3f(0.0f, 1.0f, 0.0f));
//		brickTexture = Utils.loadTexture("brick1.png");
//		gl.glBindTexture(GL_TEXTURE_2D, brickTexture);
//		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
//		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		skyboxTexture = Utils.loadCubeMap("assets");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

		// waves
		generateNoise();
		noiseTexture = buildNoiseTexture();
		System.out.println(noiseTexture);
	}

	/**
	 * Call-back handler for window re-size event. Also called when the drawable is
	 * first set to visible.
	 */
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		aspect = (float) width / (float) height;
		gl.glViewport(0, 0, width, height);
		pMat.identity().perspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
	}

	/**
	 * Called back by the animator to perform rendering.
	 */
	@Override
	public void display(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);

		// Update View Matrix
		vMat.identity();
		vMat.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		vMat.rotateX((float) Math.toRadians(-cameraPitch));
		vMat.rotateY((float) Math.toRadians(-cameraYaw));

		// Model matrix (skybox at the center)
		mMat.identity();

		// Model-View matrix
		mvMat.identity();
		mvMat.mul(vMat).mul(mMat);
		mvMat.scale(1000);

		// Render Skybox
		renderSkybox(gl);

		// Render Torus
		renderTorus(gl);

	}

	/**
	 * Called back before the OpenGL context is destroyed. Release resource such as
	 * buffers.
	 */
	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	private void setupVertices() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		numTorusVertices = myTorus.getNumVertices();
		numTorusIndices = myTorus.getNumIndices();
		Vector3f[] vertices = myTorus.getVertices();
		Vector2f[] texCoords = myTorus.getTexCoords();
		Vector3f[] normals = myTorus.getNormals();
		int[] indices = myTorus.getIndices();
		float[] pvalues = new float[vertices.length * 3];
		float[] tvalues = new float[texCoords.length * 2];
		float[] nvalues = new float[normals.length * 3];
		for (int i = 0; i < numTorusVertices; i++) {
			pvalues[i * 3] = (float) vertices[i].x;
			pvalues[i * 3 + 1] = (float) vertices[i].y;
			pvalues[i * 3 + 2] = (float) vertices[i].z;
			tvalues[i * 2] = (float) texCoords[i].x;
			tvalues[i * 2 + 1] = (float) texCoords[i].y;
			nvalues[i * 3] = (float) normals[i].x;
			nvalues[i * 3 + 1] = (float) normals[i].y;
			nvalues[i * 3 + 2] = (float) normals[i].z;
		}
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(6, vbo, 0);
		// vertex position
		// texture coordinates
		// normal vector
		// generate VBOs as before, plus one for indices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		// vertex positions
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		// texture coordinates
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		// normal vectors
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[3]);
		// indices
		IntBuffer idxBuf = Buffers.newDirectIntBuffer(indices);
		gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuf.limit() * 4, idxBuf, GL_STATIC_DRAW);

		// ==================================
		// Cấu hình cho Đối tượng 2 (2 VBO)
		// ==================================

		float[] vertexPositions = { -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
				1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f,
				-1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
				1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
				-1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f,
				-1.0f };
		float[] cubeTextureCoord = { 1.00f, 0.66f, 1.00f, 0.33f, 0.75f, 0.33f, // back face lower right
				0.75f, 0.33f, 0.75f, 0.66f, 1.00f, 0.66f, // back face upper left
				0.75f, 0.33f, 0.50f, 0.33f, 0.75f, 0.66f, // right face lower right
				0.50f, 0.33f, 0.50f, 0.66f, 0.75f, 0.66f, // right face upper left
				0.50f, 0.33f, 0.25f, 0.33f, 0.50f, 0.66f, // front face lower right
				0.25f, 0.33f, 0.25f, 0.66f, 0.50f, 0.66f, // front face upper left
				0.25f, 0.33f, 0.00f, 0.33f, 0.25f, 0.66f, // left face lower right
				0.00f, 0.33f, 0.00f, 0.66f, 0.25f, 0.66f, // left face upper left
				0.25f, 0.33f, 0.50f, 0.33f, 0.50f, 0.00f, // bottom face upper right
				0.50f, 0.00f, 0.25f, 0.00f, 0.25f, 0.33f, // bottom face lower left
				0.25f, 1.00f, 0.50f, 1.00f, 0.50f, 0.66f, // top face upper right
				0.50f, 0.66f, 0.25f, 0.66f, 0.25f, 1.00f // top face lower left
		};
		gl.glBindVertexArray(vao[1]);
		// VBO5:
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer vertBufSB = Buffers.newDirectFloatBuffer(vertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBufSB.limit() * 4, vertBufSB, GL_STATIC_DRAW);
		// VBO6:
		FloatBuffer texBufferSB = FloatBuffer.wrap(cubeTextureCoord);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glBufferData(GL_ARRAY_BUFFER, texBufferSB.limit() * 4, texBufferSB, GL_STATIC_DRAW);
	}

	private void renderSkybox(GL4 gl) {
		// Sử dụng chương trình shader của skybox
		gl.glUseProgram(skyboxProgram);

		// Xây dựng View matrix (chỉ áp dụng phép quay)
		vMat.identity();
		vMat.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		vMat.rotateX((float) Math.toRadians(-cameraPitch));
		vMat.rotateY((float) Math.toRadians(-cameraYaw));

		// Model matrix của skybox (luôn ở gốc tọa độ)
		mMat.identity();

		// Tính toán Model-View matrix
		mvMat.identity();
		mvMat.mul(vMat).mul(mMat);
		mvMat.scale(100);
		// Gửi các ma trận tới shader
		vLoc = gl.glGetUniformLocation(skyboxProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(skyboxProgram, "p_matrix");
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		// Gắn VAO và VBO của skybox
		gl.glBindVertexArray(vao[1]);
		// set up vertices buffer for cube (buffer for texture coordinates not
		// necessary)
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		// make the cube map the active texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);
		// disable depth testing, and then draw the cube map
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);
	}

	private void renderTorus(GL4 gl) {
		// Sử dụng chương trình shader của torus
		gl.glUseProgram(renderingProgram);

		// Model matrix của torus (vị trí và kích thước)
		mMat.identity();
		mMat.translate(0.0f, -0.2f, -0.7f); // Dịch torus bên trong skybox
		mMat.scale(0.2f, 0.2f, 0.2f); // Thu nhỏ torus

		// Tính toán Model-View matrix
		mvMat.identity();
		mvMat.mul(vMat).mul(mMat);

//		mvMat.scale(100);

		// Gửi các ma trận tới shader
		mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		// Gắn VAO và VBO của torus
		gl.glBindVertexArray(vao[0]);
		gl.glEnableVertexAttribArray(0); // Vertex positions
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

		gl.glEnableVertexAttribArray(1); // Texture coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, noiseTexture);

		gl.glEnableVertexAttribArray(2); // Normal vectors
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);

		// Vẽ torus
		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[3]);
		gl.glDrawElements(GL_TRIANGLES, numTorusIndices, GL_UNSIGNED_INT, 0);
	}

	private void generateNoise() {
		for (int x = 0; x < noiseWidth; x++) {
			for (int y = 0; y < noiseHeight; y++) {
				for (int z = 0; z < noiseDepth; z++) {
					noise[x][y][z] = random.nextFloat();
				}
			}
		}
	}

	private int buildNoiseTexture() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		byte[] data = new byte[noiseWidth * noiseHeight * noiseDepth * 4];
		fillDataArray(data);
		ByteBuffer bb = Buffers.newDirectByteBuffer(data);
		int[] textureIDs = new int[1];
		gl.glGenTextures(1, textureIDs, 0);
		int textureID = textureIDs[0];
		gl.glBindTexture(GL_TEXTURE_3D, textureID);
		gl.glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA8, noiseWidth, noiseHeight, noiseDepth);
		gl.glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0, noiseWidth, noiseHeight, noiseDepth, GL_RGBA,
				GL_UNSIGNED_INT_8_8_8_8_REV, bb);
		gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		return textureID;
	}

	private void fillDataArray(byte data[]) {
		double maxZoom = 32.0;
		for (int i = 0; i < noiseWidth; i++) {
			for (int j = 0; j < noiseHeight; j++) {
				for (int k = 0; k < noiseDepth; k++) {
					data[i * (noiseWidth * noiseHeight * 4) + j * (noiseHeight * 4) + k * 4 + 0] = (byte) turbulence(i,
							j, k, maxZoom);
					data[i * (noiseWidth * noiseHeight * 4) + j * (noiseHeight * 4) + k * 4 + 1] = (byte) turbulence(i,
							j, k, maxZoom);
					data[i * (noiseWidth * noiseHeight * 4) + j * (noiseHeight * 4) + k * 4 + 2] = (byte) turbulence(i,
							j, k, maxZoom);
					data[i * (noiseWidth * noiseHeight * 4) + j * (noiseHeight * 4) + k * 4 + 3] = (byte) 255;
				}
			}
		}
	}

	private double smoothNoise(double zoom, double x1, double y1, double z1) { // fraction of x1, y1, and z1 (percentage
																				// from
		// current block to next block, for this texel)
		double fractX = x1 - (int) x1;
		double fractY = y1 - (int) y1;
		double fractZ = z1 - (int) z1;
		// indices for neighboring values with wrapping at the ends
		double x2 = x1 - 1;
		if (x2 < 0)
			x2 = (Math.round(noiseWidth / zoom)) - 1.0;
		double y2 = y1 - 1;
		if (y2 < 0)
			y2 = (Math.round(noiseHeight / zoom)) - 1.0;
		double z2 = z1 - 1;
		if (z2 < 0)
			z2 = (Math.round(noiseDepth / zoom)) - 1.0;
		// smooth the noise by interpolating the greyscale intensity along all three
		// axes
		double value = 0.0;
		value += fractX * fractY * fractZ * noise[(int) x1][(int) y1][(int) z1];
		value += (1 - fractX) * fractY * fractZ * noise[(int) x2][(int) y1][(int) z1];
		value += fractX * (1 - fractY) * fractZ * noise[(int) x1][(int) y2][(int) z1];
		value += (1 - fractX) * (1 - fractY) * fractZ * noise[(int) x2][(int) y2][(int) z1];
		value += fractX * fractY * (1 - fractZ) * noise[(int) x1][(int) y1][(int) z2];
		value += (1 - fractX) * fractY * (1 - fractZ) * noise[(int) x2][(int) y1][(int) z2];
		value += fractX * (1 - fractY) * (1 - fractZ) * noise[(int) x1][(int) y2][(int) z2];
		value += (1 - fractX) * (1 - fractY) * (1 - fractZ) * noise[(int) x2][(int) y2][(int) z2];
		return value;
	}

	private double turbulence(double x, double y, double z, double maxZoom) {
		double sum = 0.0, zoom = maxZoom;
		sum = (Math.sin((1.0 / 512.0) * (8 * Math.PI) * (x + z)) + 1) * 8.0;
		while (zoom >= 0.9) {
			// the last pass is when zoom=1.
			// compute weighted sum of smoothed noise maps
			sum = sum + smoothNoise(zoom, x / zoom, y / zoom, z / zoom) * zoom;
			zoom = zoom / 2.0;
			// for each zoom factor that is a power of two.
		}
		sum = 128.0 * sum / maxZoom; // guarantees RGB < 256 for maxZoom values up to 64
		return sum;
	}

	/** The entry main() method to setup the top-level container and animator */
	public static void main(String[] args) {
//		// Run the GUI codes in the event-dispatching thread for thread safety
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Program14_5();
			}
		});
	}

}