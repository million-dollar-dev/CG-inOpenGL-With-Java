package book;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_CCW;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
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

public class Program15_7 extends JFrame implements GLEventListener {
	// Setup OpenGL Graphics Renderer
	// for the GL Utility
	private GLCanvas myCanvas;
	private int renderingProgram, skyboxProgram, planeProgram, renderingProgramSURFACE, renderingProgramCubeMap,
			renderingProgramFLOOR;
	private int vao[] = new int[3];
	private int vbo[] = new int[9];
	private float cameraX, cameraY, cameraZ;
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();
	private Matrix4f vMat = new Matrix4f();
	private Matrix4f mMat = new Matrix4f();
	private Matrix4f mvMat = new Matrix4f();
	private int mvLoc, pLoc, vLoc, mLoc, nLoc;
	private float aspect;
	private String vShaderSource = "vertShader14_5.glsl";
	private String fShaderSource = "fragShader14_5.glsl";
//	private String vShaderSource = "vertShader5_1.glsl";
//	private String fShaderSource = "fragShader5_1.glsl";
	// Camera
	private float cameraYaw = 0.0f; // Góc quay trái/phải của camera
	private float cameraPitch = 0.0f; // Góc quay lên/xuống của camera
	private float cameraRoll = 0.0f; // Góc nghiêng ngang của camera (thường không sử dụng nhiều)
	private float cameraSpeed = 0.1f; // Tốc độ di chuyển của camera
	private Vector3f cameraPosition = new Vector3f(0.0f, 0.0f, 0.0f); // Vị trí của camera

	private Torus myTorus = new Torus(0.5f, 0.2f, 48);;
	private int numTorusVertices, numTorusIndices;
	private int brickTexture, skyboxTexture;
	private float planeHeight = 0.0f;
	private float cameraHeight = 2.0f;
	private float surfacePlaneHeight = 0.0f;
	private float floorPlaneHeight = -10.0f;

	// initial light location
	private Vector3f initialLightLoc = new Vector3f(2.0f, 50.0f, 2.0f);
	private float lightSpeed = 0.1f; // Tốc độ di chuyển nguồn sáng
	private float lightYaw = 0.0f; // Góc xoay quanh trục Y (theo chiều ngang)
	// properties of white light (global and positional) used in this scene
	float[] globalAmbient = new float[] { 0.2f, 0.2f, 0.2f, 1.0f };
	float[] lightAmbient = new float[] { 0.3f, 0.3f, 0.3f, 1.0f };
	float[] lightDiffuse = new float[] { 1.0f, 1.0f, 0.9f, 1.0f };
	float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	// gold material properties
	float[] matAmb = Utils.waterAmbient();
	float[] matDif = Utils.waterAmbient();
	float[] matSpe = Utils.waterAmbient();
	float matShi = Utils.waterShininess();
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mAmbLoc, mDiffLoc, mSpecLoc, mShiLoc;
	private Vector3f currentLightPos = new Vector3f(); // current light position as Vector3f
	private float[] lightPos = new float[3]; // current light position as float array
	private Matrix4f invTrMat = new Matrix4f();

	// phản chiếu
	private int[] bufferId = new int[2];
	private int reflectFrameBuffer, refractFrameBuffer;
	private int reflectTextureId, refractTextureId;
	private int aboveLoc;

	// wave
	private int noiseTexture;
	private int noiseWidth = 256;
	private int noiseHeight = 256;
	private int noiseDepth = 256;
	double[][][] noise = new double[noiseWidth][noiseHeight][noiseDepth];
	Random random = new Random();

	// wave's movement
	private float depthLookup = 0.0f;
	private int dOffsetLoc;
	private long currTime;   // Thời gian hiện tại (microseconds hoặc milliseconds)
	private long prevTime;
	/** Constructor to setup the GUI for this Component */
	public Program15_7() {
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
					System.out.println("lll");
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
				case java.awt.event.KeyEvent.VK_I: // Di chuyển nguồn sáng lên (trục Y tăng)
					initialLightLoc.y += lightSpeed;
					break;
				case java.awt.event.KeyEvent.VK_K: // Di chuyển nguồn sáng xuống (trục Y giảm)
					initialLightLoc.y -= lightSpeed;
					break;
				case java.awt.event.KeyEvent.VK_J: // Di chuyển nguồn sáng sang trái (trục X giảm)
					initialLightLoc.x -= lightSpeed * Math.cos(Math.toRadians(lightYaw));
					initialLightLoc.z += lightSpeed * Math.sin(Math.toRadians(lightYaw));
					break;
				case java.awt.event.KeyEvent.VK_L: // Di chuyển nguồn sáng sang phải (trục X tăng)
					initialLightLoc.x += lightSpeed * Math.cos(Math.toRadians(lightYaw));
					initialLightLoc.z -= lightSpeed * Math.sin(Math.toRadians(lightYaw));
					break;
				}

				System.out.println("camera y: " + cameraPosition.y + " / " + surfacePlaneHeight);
				System.out
						.println("light: " + initialLightLoc.x + " - " + initialLightLoc.y + " - " + initialLightLoc.z);
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
		planeProgram = Utils.createShaderProgram(vShaderSource, "fragShader15_1.glsl");
//		renderingProgramFLOOR = Utils.createShaderProgram("vertShader15_3_Surface.glsl",
//				"fragShader15_3_Surface.glsl");
		renderingProgramSURFACE = Utils.createShaderProgram("vertShader15_7.glsl", "fragShader15_7.glsl");
		renderingProgramFLOOR = Utils.createShaderProgram("vertShader5_1.glsl", "fragShader15_1.glsl");
		renderingProgramCubeMap = Utils.createShaderProgram("vertShader9_2.glsl", "fragShader9_2.glsl");
//		renderingProgramFLOOR = Utils.createShaderProgram("vertShaderFLOOR.glsl", "fragShaderFLOOR.glsl");
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
		brickTexture = Utils.loadTexture("brick1.png");
		gl.glBindTexture(GL_TEXTURE_2D, brickTexture);

		skyboxTexture = Utils.loadCubeMap("assets");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
		System.out.println(cameraPosition.y);
		createReflectRefractBuffers();
		// waves
		gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		generateNoise();
		noiseTexture = buildNoiseTexture();
		System.out.println(noiseTexture);
		prevTime = System.currentTimeMillis();
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
		currTime = System.currentTimeMillis();
		depthLookup += (float) (currTime - prevTime) * .0001f;
		prevTime = currTime;
		GL4 gl = (GL4) GLContext.getCurrentGL();

		// -------------------- Render Reflection Scene --------------------
		if (cameraPosition.y >= surfacePlaneHeight) {
			vMat.identity();
			vMat.translate(0.0f, cameraPosition.y - surfacePlaneHeight, 0.0f);
			vMat.rotateX((float) Math.toRadians(-cameraPitch));

			gl.glBindFramebuffer(GL_FRAMEBUFFER, reflectFrameBuffer);
			gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			prepForSkyBoxRender();
			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CCW); // Inside view of the skybox
			gl.glDisable(GL_DEPTH_TEST);
			gl.glDrawArrays(GL_TRIANGLES, 0, 36);
			gl.glEnable(GL_DEPTH_TEST);
		}

		// -------------------- Render Refraction Scene --------------------
		vMat.identity();
		vMat.translate(0.0f, -cameraPosition.y, 0.0f);
		vMat.rotateX((float) Math.toRadians(cameraPitch));

		gl.glBindFramebuffer(GL_FRAMEBUFFER, refractFrameBuffer);
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		if (cameraPosition.y >= surfacePlaneHeight) {
			prepForFloorRender();
			gl.glEnable(GL_DEPTH_TEST);
			gl.glDepthFunc(GL_LEQUAL);
			gl.glDrawArrays(GL_TRIANGLES, 0, 6);
		} else {
			prepForSkyBoxRender();
			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CCW); // Inside view of the skybox
			gl.glDisable(GL_DEPTH_TEST);
			gl.glDrawArrays(GL_TRIANGLES, 0, 36);
			gl.glEnable(GL_DEPTH_TEST);
		}

		// -------------------- Render Final Scene --------------------
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);

		// Update View Matrix
		vMat.identity();
		vMat.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		vMat.rotateX((float) Math.toRadians(-cameraPitch));
		vMat.rotateY((float) Math.toRadians(-cameraYaw));

		// Render Skybox
		prepForSkyBoxRender();
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW); // Inside view of the skybox
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);

		// Render Water Surface
		prepForTopSurfaceRender();
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, reflectTextureId);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, refractTextureId);
		gl.glActiveTexture(GL_TEXTURE2);
		gl.glBindTexture(GL_TEXTURE_3D, noiseTexture);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
//	    gl.glFrontFace(GL_CCW);
		if (cameraPosition.y >= surfacePlaneHeight) {
			gl.glFrontFace(GL_CCW); // Camera trên mặt nước
		} else {
			gl.glFrontFace(GL_CW); // Camera dưới mặt nước
		}
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);

		// Render Floor
		prepForFloorRender();
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_3D, noiseTexture);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glFrontFace(GL_CCW);
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);

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
		gl.glGenBuffers(9, vbo, 0);
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

		// ==================================
		// Cấu hình cho Đối tượng 3 (2 VBO)
		// ==================================
		float[] PLANE_POSITIONS = { -128.0f, 0.0f, -128.0f, -128.0f, 0.0f, 128.0f, 128.0f, 0.0f, -128.0f, 128.0f, 0.0f,
				-128.0f, -128.0f, 0.0f, 128.0f, 128.0f, 0.0f, 128.0f };
		float[] PLANE_TEXCOORDS = { 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f };
		float[] PLANE_NORMALS = { 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f, 0.0f };

		gl.glBindVertexArray(vao[2]);
		// VBO5:
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer vertBufPlane = Buffers.newDirectFloatBuffer(PLANE_POSITIONS);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBufPlane.limit() * 4, vertBufPlane, GL_STATIC_DRAW);
		// VBO6:
		FloatBuffer texBufferPlane = FloatBuffer.wrap(PLANE_TEXCOORDS);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glBufferData(GL_ARRAY_BUFFER, texBufferPlane.limit() * 4, texBufferPlane, GL_STATIC_DRAW);
		// VBO6:
		FloatBuffer norBufferPlane = FloatBuffer.wrap(PLANE_NORMALS);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glBufferData(GL_ARRAY_BUFFER, norBufferPlane.limit() * 4, norBufferPlane, GL_STATIC_DRAW);

	}

	private void renderTorus(GL4 gl) {
		// Sử dụng chương trình shader của torus
		gl.glUseProgram(renderingProgram);

		// Model matrix của torus (vị trí và kích thước)
		mMat.identity();
		mMat.translate(0.0f, 0.0f, -0.7f); // Dịch torus bên trong skybox
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

	private void installLights(int program) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		// save the light position in a float array
		lightPos[0] = currentLightPos.x();
		lightPos[1] = currentLightPos.y();
		lightPos[2] = currentLightPos.z();
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(program, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(program, "light.ambient");
		diffLoc = gl.glGetUniformLocation(program, "light.diffuse");
		specLoc = gl.glGetUniformLocation(program, "light.specular");
		posLoc = gl.glGetUniformLocation(program, "light.position");
		mAmbLoc = gl.glGetUniformLocation(program, "material.ambient");
		mDiffLoc = gl.glGetUniformLocation(program, "material.diffuse");
		mSpecLoc = gl.glGetUniformLocation(program, "material.specular");
		mShiLoc = gl.glGetUniformLocation(program, "material.shininess");

		// set the uniform light and material values in the shader
		gl.glProgramUniform4fv(program, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(program, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(program, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(program, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(program, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(program, mAmbLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(program, mDiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(program, mSpecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(program, mShiLoc, matShi);
	}

	private void createReflectRefractBuffers() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		// initialize reflection framebuffer
		// called once from init()
		gl.glGenFramebuffers(1, bufferId, 0);
		reflectFrameBuffer = bufferId[0];
		gl.glBindFramebuffer(GL_FRAMEBUFFER, reflectFrameBuffer);
		gl.glGenTextures(1, bufferId, 0);
		reflectTextureId = bufferId[0];
		gl.glBindTexture(GL_TEXTURE_2D, reflectTextureId);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, myCanvas.getWidth(), myCanvas.getHeight(), 0, GL_RGBA,
				GL_UNSIGNED_BYTE, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, reflectTextureId, 0);
		gl.glDrawBuffer(GL_COLOR_ATTACHMENT0);
		gl.glGenTextures(1, bufferId, 0);
		gl.glBindTexture(GL_TEXTURE_2D, bufferId[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, myCanvas.getWidth(), myCanvas.getHeight(), 0,
				GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, bufferId[0], 0);

		// initialize refraction framebuffer
		gl.glGenFramebuffers(1, bufferId, 0);
		refractFrameBuffer = bufferId[0];
		gl.glBindFramebuffer(GL_FRAMEBUFFER, refractFrameBuffer);
		gl.glGenTextures(1, bufferId, 0);
		refractTextureId = bufferId[0];
		gl.glBindTexture(GL_TEXTURE_2D, refractTextureId);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, myCanvas.getWidth(), myCanvas.getHeight(), 0, GL_RGBA,
				GL_UNSIGNED_BYTE, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, refractTextureId, 0);
		gl.glDrawBuffer(GL_COLOR_ATTACHMENT0);
		gl.glGenTextures(1, bufferId, 0);
		gl.glBindTexture(GL_TEXTURE_2D, bufferId[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, myCanvas.getWidth(), myCanvas.getHeight(), 0,
				GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, bufferId[0], 0);
	}

	private void prepForSkyBoxRender() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(renderingProgramCubeMap);
		vLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "p_matrix");
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		// vbo[0] holds the skybox vertices
		gl.glBindVertexArray(vao[1]);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);
	}

	private void prepForTopSurfaceRender() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(renderingProgramSURFACE);
		mLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "norm_matrix");
		dOffsetLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "depthOffset");
		if (dOffsetLoc == -1) {
		    System.err.println("Uniform 'depthOffset' không được tìm thấy hoặc đã bị tối ưu hóa.");
		}
		gl.glUniform1f(dOffsetLoc, depthLookup);
		FloatBuffer depthOffsetValue = FloatBuffer.allocate(1); // Tạo FloatBuffer với kích thước 1
		gl.glGetUniformfv(renderingProgramSURFACE, dOffsetLoc, depthOffsetValue);
		System.out.println("Depth Offset Value: " + depthOffsetValue.get(0));
		System.out.println(depthLookup);
		mMat.translation(0.0f, surfacePlaneHeight, 0.0f);
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
		currentLightPos.set(initialLightLoc);
		installLights(renderingProgramSURFACE);
		// get references to uniform variables
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		///
		aboveLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "isAbove");
		if (cameraPosition.y >= surfacePlaneHeight) {
			gl.glUniform1i(aboveLoc, 1);
		} else
			gl.glUniform1i(aboveLoc, 0);

		// VBOs 1, 2, and 3 contain the plane vertices, texcoords, and normals
		gl.glBindVertexArray(vao[2]);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
	}

	private void prepForFloorRender() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(renderingProgramFLOOR);
//		mLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "m_matrix");
//		vLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "v_matrix");
//		pLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "p_matrix");
//		nLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "norm_matrix");
//		mMat.translation(0.0f, floorPlaneHeight, 0.0f);
//		mMat.invert(invTrMat);
//		invTrMat.transpose(invTrMat);
//		currentLightPos.set(initialLightLoc);
//		installLights(renderingProgramFLOOR);
//		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
//		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
//		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
//		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		mMat.identity();
		mMat.translation(0, floorPlaneHeight, 0);
		mvMat.identity();
		mvMat.mul(vMat).mul(mMat);
		mvLoc = gl.glGetUniformLocation(planeProgram, "mv_matrix");
		mLoc = gl.glGetUniformLocation(planeProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(planeProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(planeProgram, "p_matrix");
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		aboveLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "isAbove");
		if (cameraPosition.y >= surfacePlaneHeight)
			gl.glUniform1i(aboveLoc, 1);
		else
			gl.glUniform1i(aboveLoc, 0);
		//
		gl.glBindVertexArray(vao[2]);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
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
		sum = (Math.sin((1.0 / 512.0) * (8 * Math.PI) * (x + z - 4 * y)) + 1) * 8.0;
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
				new Program15_7();
			}
		});
	}

}