package objTest;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;

import helpers.ImportedModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ObjLoader {
	private List<float[]> vertices;
	private List<int[]> faces;
	private int vaoId;
	private int vboId;
	private int eboId;
	private int vertexCount;

	public ObjLoader() {
		vertices = new ArrayList<>();
		faces = new ArrayList<>();
	}

	public void load(String filePath) {
		InputStream input = ImportedModel.class.getClassLoader().getResourceAsStream("assets/" + filePath);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("v ")) {
					String[] tokens = line.split("\\s+");
					float x = Float.parseFloat(tokens[1]);
					float y = Float.parseFloat(tokens[2]);
					float z = Float.parseFloat(tokens[3]);
					vertices.add(new float[] { x, y, z });
				} else if (line.startsWith("f ")) {
					String[] tokens = line.split("\\s+");
					int[] face = new int[3];
					for (int i = 1; i <= 3; i++) {
						String[] parts = tokens[i].split("/");
						face[i - 1] = Integer.parseInt(parts[0]) - 1;
					}
					faces.add(face);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		vertexCount = faces.size() * 3;
	}

	public void init(GL4 gl) {
		FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertices.size() * 3);
		for (float[] vertex : vertices) {
			vertexBuffer.put(vertex);
		}
		vertexBuffer.flip();

		IntBuffer indexBuffer = GLBuffers.newDirectIntBuffer(vertexCount);
		for (int[] face : faces) {
			indexBuffer.put(face);
		}
		indexBuffer.flip();

		// Tạo VAO
		int[] vao = new int[1];
		gl.glGenVertexArrays(1, vao, 0);
		vaoId = vao[0];
		gl.glBindVertexArray(vaoId);

		// Tạo VBO
		int[] vbo = new int[1];
		gl.glGenBuffers(1, vbo, 0);
		vboId = vbo[0];
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vboId);
		gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexBuffer.limit() * Float.BYTES, vertexBuffer, GL4.GL_STATIC_DRAW);

		// Tạo EBO
		int[] ebo = new int[1];
		gl.glGenBuffers(1, ebo, 0);
		eboId = ebo[0];
		gl.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, eboId);
		gl.glBufferData(GL4.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.limit() * Integer.BYTES, indexBuffer,
				GL4.GL_STATIC_DRAW);

		// Thiết lập thuộc tính đỉnh
		gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 3 * Float.BYTES, 0);
		gl.glEnableVertexAttribArray(0);

		// Giải phóng
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
		gl.glBindVertexArray(0);
	}

	public void draw(GL4 gl) {
		System.out.println(vertexCount);
		gl.glBindVertexArray(vaoId);
		gl.glDrawElements(GL4.GL_TRIANGLES, vertexCount, GL4.GL_UNSIGNED_INT, 0);
		gl.glBindVertexArray(0);
	}

	public void cleanup(GL4 gl) {
		int[] buffers = { vboId, eboId };
		gl.glDeleteBuffers(2, buffers, 0);
		gl.glDeleteVertexArrays(1, new int[] { vaoId }, 0);
	}
}
