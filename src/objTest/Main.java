package objTest;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import org.joml.Matrix4f;
import com.jogamp.opengl.util.GLBuffers;

import book.Utils;

import javax.swing.*;
import java.nio.FloatBuffer;

public class Main extends JFrame implements GLEventListener {
	private ObjLoader objLoader;
	private int shaderProgram;
	private Matrix4f modelMatrix;
	private Matrix4f viewMatrix;
	private Matrix4f projectionMatrix;

	public Main() {
		GLProfile profile = GLProfile.get(GLProfile.GL4);
		GLCapabilities capabilities = new GLCapabilities(profile);
		GLCanvas canvas = new GLCanvas(capabilities);

		canvas.addGLEventListener(this);
		canvas.setSize(800, 600);

		this.add(canvas);
		this.setTitle("OpenGL 4 3D Model Viewer");
		this.setSize(800, 600);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);

		FPSAnimator animator = new FPSAnimator(canvas, 60);
		animator.start();

		objLoader = new ObjLoader();
		objLoader.load("Palm.obj");

		modelMatrix = new Matrix4f().identity();
		viewMatrix = new Matrix4f().lookAt(0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
		projectionMatrix = new Matrix4f().perspective((float) Math.toRadians(45.0), 800f / 600f, 0.1f, 100.0f);
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL4 gl = drawable.getGL().getGL4();
		gl.glEnable(GL4.GL_DEPTH_TEST);

		// Tạo shader program từ file vertex và fragment shader
		shaderProgram = Utils.createShaderProgram("vertObj.glsl", "fragObj.glsl");
		projectionMatrix = new Matrix4f().identity();
		objLoader.init(gl);
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL4 gl = drawable.getGL().getGL4();
		objLoader.cleanup(gl);
		gl.glDeleteProgram(shaderProgram);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL4 gl = drawable.getGL().getGL4();
		gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(shaderProgram);

		// Truyền các ma trận model, view, projection vào shader
		setMatrix4(gl, shaderProgram, "model", modelMatrix);
		setMatrix4(gl, shaderProgram, "view", viewMatrix);
		setMatrix4(gl, shaderProgram, "projection", projectionMatrix);

		objLoader.draw(gl);

		gl.glUseProgram(0);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		float aspect = (float) width / height;
		projectionMatrix.identity().perspective((float) Math.toRadians(45.0), aspect, 0.1f, 100.0f);
	}

	public static void main(String[] args) {
		new Main();
	}

	// Hàm setMatrix4 để truyền ma trận vào shader
	private void setMatrix4(GL4 gl, int programId, String uniformName, Matrix4f matrix) {
		int location = gl.glGetUniformLocation(programId, uniformName);
		if (location == -1) {
			System.err.println("Không tìm thấy uniform: " + uniformName);
			return;
		}

		FloatBuffer matrixBuffer = GLBuffers.newDirectFloatBuffer(16);
		matrix.get(matrixBuffer);
		matrixBuffer.flip(); // Đặt lại con trỏ đọc trước khi truyền vào OpenGL

		gl.glUniformMatrix4fv(location, 1, false, matrixBuffer);
	}
}
