package helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class ImportedModel {
	private Vector3f[] vertices;
	private Vector2f[] texCoords;
	private Vector3f[] normals;
	private int numVertices;

	public ImportedModel(String filename) {
		ModelImporter modelImporter = new ModelImporter();
		try {
			modelImporter.parseOBJ(filename); // uses modelImporter to get vertex information
			numVertices = modelImporter.getNumVertices();
			float[] verts = modelImporter.getVertices();
			float[] tcs = modelImporter.getTextureCoordinates();
			float[] norm = modelImporter.getNormals();
			vertices = new Vector3f[numVertices];
			texCoords = new Vector2f[numVertices];
			normals = new Vector3f[numVertices];
			for (int i = 0; i < vertices.length; i++) {
				vertices[i] = new Vector3f();
				vertices[i].set(verts[i * 3], verts[i * 3 + 1], verts[i * 3 + 2]);
				texCoords[i] = new Vector2f();
				texCoords[i].set(tcs[i * 2], tcs[i * 2 + 1]);
				normals[i] = new Vector3f();
				normals[i].set(norm[i * 3], norm[i * 3 + 1], norm[i * 3 + 2]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getNumVertices() {
		return numVertices;
	} // accessors

	public Vector3f[] getVertices() {
		return vertices;
	}

	public Vector2f[] getTexCoords() {
		return texCoords;
	}

	public Vector3f[] getNormals() {
		return normals;
	}

	private class ModelImporter { // values as read in from OBJ file
		private ArrayList<Float> vertVals = new ArrayList<Float>();
		private ArrayList<Float> stVals = new ArrayList<Float>();
		private ArrayList<Float> normVals = new ArrayList<Float>();
		// values stored for later use as vertex attributes
		private ArrayList<Float> triangleVerts = new ArrayList<Float>();
		private ArrayList<Float> textureCoords = new ArrayList<Float>();
		private ArrayList<Float> normals = new ArrayList<Float>();

		public void parseOBJ(String filename) throws IOException {
			InputStream input = ImportedModel.class.getClassLoader().getResourceAsStream("assets/" + filename);
			if (input == null) {
				throw new FileNotFoundException("File not found: assets/" + filename);
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			String line;
			while ((line = br.readLine()) != null) {

				if (line.startsWith("vt")) { // texture coordinates ("vt" case)
					String[] values = line.substring(3).trim().split("\\s+");
					if (values.length >= 2) {
						stVals.add(Float.valueOf(values[0])); // U
						stVals.add(Float.valueOf(values[1])); // V
					} else {
						for (String s : values) {
							stVals.add(Float.valueOf(s)); // extract the texture coordinate values
						}
					}
				} else if (line.startsWith("vn")) { // vertex normals ("vn" case)
					System.out.println("vn: " + line);
					for (String s : (line.substring(3)).trim().split("\\s+")) {
						normVals.add(Float.valueOf(s)); // extract the normal vector values
					}
				} else if (line.startsWith("v")) // vertex position ("v" case)
				{
					System.out.println("v: " + line);
					for (String s : (line.substring(2)).trim().split("\\s+")) {
						vertVals.add(Float.valueOf(s)); // extract the vertex position values
					}
				} else if (line.startsWith("f")) {
					String[] vertices = (line.substring(2)).trim().split("\\s+");
					if (vertices.length == 4) {
						// Xử lý tam giác 1: (v1, v2, v3)
						processFaceVertex(vertices[0], vertVals, stVals, normVals, triangleVerts, textureCoords,
								normals);
						processFaceVertex(vertices[1], vertVals, stVals, normVals, triangleVerts, textureCoords,
								normals);
						processFaceVertex(vertices[2], vertVals, stVals, normVals, triangleVerts, textureCoords,
								normals);

						// Xử lý tam giác 2: (v1, v3, v4)
						processFaceVertex(vertices[0], vertVals, stVals, normVals, triangleVerts, textureCoords,
								normals);
						processFaceVertex(vertices[2], vertVals, stVals, normVals, triangleVerts, textureCoords,
								normals);
						processFaceVertex(vertices[3], vertVals, stVals, normVals, triangleVerts, textureCoords,
								normals);
					} else if (vertices.length == 3) {
						processFaceVertex(vertices[0], vertVals, stVals, normVals, triangleVerts, textureCoords,
								normals);
						processFaceVertex(vertices[1], vertVals, stVals, normVals, triangleVerts, textureCoords,
								normals);
						processFaceVertex(vertices[2], vertVals, stVals, normVals, triangleVerts, textureCoords,
								normals);
					}
				}

			}
			input.close();
		}

		// accessors for retrieving the number of vertices, the vertices themselves,
		// and the corresponding texture coordinates and normals (only called once per
		// model)
		public int getNumVertices() {
			return (triangleVerts.size() / 3);
		}

		public float[] getVertices() {
			float[] p = new float[triangleVerts.size()];
			for (int i = 0; i < triangleVerts.size(); i++) {
				p[i] = triangleVerts.get(i);
			}
			return p;
		}

		// similar accessors for texture coordinates and normal vectors go here
		// Accessor to get texture coordinates
		public float[] getTextureCoordinates() {
			float[] t = new float[textureCoords.size()];
			for (int i = 0; i < textureCoords.size(); i++) {
				t[i] = textureCoords.get(i);
			}
			return t;
		}

		// Accessor to get normals
		public float[] getNormals() {
			float[] n = new float[normals.size()];
			for (int i = 0; i < normals.size(); i++) {
				n[i] = normals.get(i);
			}
			return n;
		}

		private void processFaceVertex(String vertex, List<Float> vertVals, List<Float> stVals, List<Float> normVals,
				List<Float> triangleVerts, List<Float> textureCoords, List<Float> normals) {
			String[] indices = vertex.split("/");

			String v = indices.length > 0 ? indices[0] : "0";
			String vt = indices.length > 1 ? indices[1] : "0";
			String vn = indices.length > 2 ? indices[2] : "0";

			int vertRef = (Integer.valueOf(v) - 1) * 3;
			int tcRef = (Integer.valueOf(vt) - 1) * 2;
			int normRef = (Integer.valueOf(vn) - 1) * 3;

			triangleVerts.add(vertVals.get(vertRef)); // X, Y, Z
			triangleVerts.add(vertVals.get(vertRef + 1));
			triangleVerts.add(vertVals.get(vertRef + 2));

			textureCoords.add(stVals.get(tcRef)); // U, V
			textureCoords.add(stVals.get(tcRef + 1));

			normals.add(normVals.get(normRef)); // Nx, Ny, Nz
			normals.add(normVals.get(normRef + 1));
			normals.add(normVals.get(normRef + 2));
		}

	}

	public static void main(String[] args) throws IOException {
		ImportedModel model = new ImportedModel("complex_cube.obj");
//		ImportedModel.ModelImporter imp = model.new ModelImporter();
//		imp.parseOBJ("complex_cube.obj");
//		for (float f: imp.getVertices()) {
//			System.out.print(f + ", ");
//		}
//		System.out.println(model.getNumVertices());
	}
}
