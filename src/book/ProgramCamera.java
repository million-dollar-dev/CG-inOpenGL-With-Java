package book;

import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GLContext;
import org.joml.*;
import java.nio.*;
import javax.swing.*;
import java.lang.Math;

public class ProgramCamera extends JFrame implements GLEventListener {
	// Setup OpenGL Graphics Renderer
	// for the GL Utility
	private GLCanvas myCanvas;
	private int renderingProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[2];
	private float cameraX, cameraY, cameraZ;
	private float cubeLocX, cubeLocY, cubeLocZ;
	private float pyraLocX, pyraLocY, pyraLocZ;
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();
	private Matrix4f vMat = new Matrix4f();
	private Matrix4f mMat = new Matrix4f();
	private Matrix4f mvMat = new Matrix4f();
	private int mvLoc, pLoc;
	private float aspect;
	private String vShaderSource = "vertShader.glsl";
	private String fShaderSource = "fragShader.glsl";
	private double elapsedTime, startTime, tf;
	//Camera
	private float cameraYaw = 0.0f;    // Góc quay trái/phải của camera
	private float cameraPitch = 0.0f;  // Góc quay lên/xuống của camera
	private float cameraRoll = 0.0f;   // Góc nghiêng ngang của camera (thường không sử dụng nhiều)
	private float cameraSpeed = 0.1f;  // Tốc độ di chuyển của camera
	private Vector3f cameraPosition = new Vector3f(0.0f, 0.0f, 8.0f); // Vị trí của camera

	/** Constructor to setup the GUI for this Component */
	public ProgramCamera() {
		setTitle("Chapter4 - program1a");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		//Camera
		myCanvas.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				switch (e.getKeyCode()) {
                case java.awt.event.KeyEvent.VK_LEFT:
                    cameraYaw -= 2.0f;  // Quay trái
                    break;
                case java.awt.event.KeyEvent.VK_RIGHT:
                    cameraYaw += 2.0f;  // Quay phải
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
		//camera
		myCanvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
	        private int lastX, lastY;

	        @Override
	        public void mouseDragged(java.awt.event.MouseEvent e) {
	            int deltaX = e.getX() - lastX;
	            int deltaY = e.getY() - lastY;
	            cameraYaw += deltaX * 0.1f;   // Điều chỉnh độ nhạy của chuột
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
		setupVertices();
		cameraX = 0.0f;
		cameraY = 0.0f;
		cameraZ = 8.0f;
		cubeLocX = 0.0f;
		cubeLocY = -2.0f;
		cubeLocZ = 0.0f;
//		pyraLocX = 2.0f;
//		pyraLocY = 2.0f;
//		pyraLocZ = -2.0f;
		pyraLocX = 2.0f;
		pyraLocY = 2.0f;
		pyraLocZ = -3.0f;
	}

	/**
	 * Call-back handler for window re-size event. Also called when the drawable is
	 * first set to visible.
	 */
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
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
		gl.glUseProgram(renderingProgram);
		// use system time to generate slowly-increasing sequence of floating-point
		// values
		elapsedTime = System.currentTimeMillis() - startTime; // elapsedTime, startTime, and tf
		tf = elapsedTime / 1000.0;
		// would all be declared of type double.
		// get references to the uniform variables for the MV and projection matrices
		
		

		
		mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
		// build perspective matrix. This one has fovy=60, aspect ratio matches the
		// screen window.
		// Values for near and far clipping planes can vary as discussed in Section 4.9
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
		// build view matrix, model matrix, and model-view matrix
		// camera	
		vMat.identity();
	    vMat.rotateX((float) Math.toRadians(-cameraPitch));
	    vMat.rotateY((float) Math.toRadians(-cameraYaw));
	    vMat.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

		// draw the cube (use buffer #0)
		mMat.translation(cubeLocX, cubeLocY, cubeLocZ);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glFrontFace(GL_CW);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);

		// draw the pyramid (use buffer #1)
		mMat.translation(pyraLocX, pyraLocY, pyraLocZ);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glFrontFace(GL_CCW);
		gl.glDrawArrays(GL_TRIANGLES, 0, 18);

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
		float[] cubePositions = { -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f,
				1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f,
				1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
				1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f,
				1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f,
				-1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f };
		float[] pyramidPositions = { -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, -1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 0.0f, 1.0f, 0.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, -1.0f, -1.0f,
				-1.0f, -1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f,
				1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f };
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit() * 4, cubeBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer pyraBuf = Buffers.newDirectFloatBuffer(pyramidPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, pyraBuf.limit() * 4, pyraBuf, GL_STATIC_DRAW);
	}

	/** The entry main() method to setup the top-level container and animator */
	public static void main(String[] args) {
//		// Run the GUI codes in the event-dispatching thread for thread safety
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new ProgramCamera();
			}
		});
//				// Create the OpenGL rendering canvas
//				GLJPanel canvas = new First3D();
//				canvas.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
//
//				// Create a animator that drives canvas' display() at the specified FPS.
//				final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);
//
//				// Create the top-level container
//				final JFrame frame = new JFrame(); // Swing's JFrame or AWT's Frame
//				frame.getContentPane().add(canvas);
//				frame.addWindowListener(new WindowAdapter() {
//					@Override
//					public void windowClosing(WindowEvent e) {
//						// Use a dedicate thread to run the stop() to ensure that the
//						// animator stops before program exits.
//						new Thread() {
//							@Override
//							public void run() {
//								if (animator.isStarted())
//									animator.stop();
//								System.exit(0);
//							}
//						}.start();
//					}
//				});
//				frame.setTitle(TITLE);
//				frame.pack();
//				frame.setVisible(true);
//				animator.start(); // start the animation loop
//			}
//		});

	}

}