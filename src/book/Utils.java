package book;

import static com.jogamp.opengl.GL2ES2.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;

public class Utils {
	public static String[] readShaderSource(String filename) {
		Vector<String> lines = new Vector<String>();
		String[] program;
		InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream("shaders/" + filename);
		if (inputStream == null) {
			System.err.println("File not found: " + filename);
			return null;
		}
		try (Scanner sc = new Scanner(inputStream)) {
			while (sc.hasNext()) {
				lines.addElement(sc.nextLine());
			}
			program = new String[lines.size()];
			for (int i = 0; i < lines.size(); i++) {
				program[i] = (String) lines.elementAt(i) + "\n";
			}
		}
		return program;
	}

	public static int createShaderProgram(String vShaderSource, String fShaderSource) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		String vshaderSource[] = readShaderSource(vShaderSource);
		String fshaderSource[] = readShaderSource(fShaderSource);
		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0); // 3 is the count of lines of source
																					// code
		gl.glCompileShader(vShader);
		int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0); // 4 is the count of lines of source
																					// code
		gl.glCompileShader(fShader);
		int vfProgram = gl.glCreateProgram();
		gl.glAttachShader(vfProgram, vShader);
		gl.glAttachShader(vfProgram, fShader);
		gl.glLinkProgram(vfProgram);
		gl.glDeleteShader(vShader);
		gl.glDeleteShader(fShader);
		return vfProgram;
	}

	public static void printShaderLog(int shader) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int[] len = new int[1];
		int[] chWrittn = new int[1];
		byte[] log = null;
		// determine the length of the shader compilation log
		gl.glGetShaderiv(shader, GL_INFO_LOG_LENGTH, len, 0);
		if (len[0] > 0) {
			log = new byte[len[0]];
			gl.glGetShaderInfoLog(shader, len[0], chWrittn, 0, log, 0);
			System.out.println("Shader Info Log: ");
			for (int i = 0; i < log.length; i++) {
				System.out.print((char) log[i]);
			}
		}
	}

	public static void printProgramLog(int prog) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int[] len = new int[1];
		int[] chWrittn = new int[1];
		byte[] log = null;
		// determine the length of the program linking log
		gl.glGetProgramiv(prog, GL_INFO_LOG_LENGTH, len, 0);
		if (len[0] > 0) {
			log = new byte[len[0]];
			gl.glGetProgramInfoLog(prog, len[0], chWrittn, 0, log, 0);
			System.out.println("Program Info Log: ");
			for (int i = 0; i < log.length; i++) {
				System.out.print((char) log[i]);
			}
		}
	}

	public static boolean checkOpenGLError() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		boolean foundError = false;
		GLU glu = new GLU();
		int glErr = gl.glGetError();
		while (glErr != GL_NO_ERROR) {
			System.err.println("glError: " + glu.gluErrorString(glErr));
			foundError = true;
			glErr = gl.glGetError();
		}
		return foundError;
	}

	public static int loadTexture(String textureFileName) {
		Texture tex = null;
		try {
			InputStream stream = Utils.class.getClassLoader().getResourceAsStream("assets/" + textureFileName);
			if (stream == null) {
				throw new RuntimeException("File not found " + textureFileName);
			}
			tex = TextureIO.newTexture(stream, false, TextureIO.PNG); // Thay TextureIO.PNG bằng định dạng file phù hợp
																		// nếu cần
		} catch (Exception e) {
			e.printStackTrace();
		}
		int textureID = tex.getTextureObject();
		return textureID;
	}

	public static int loadTextureAWT(String textureFileName) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		BufferedImage textureImage = getBufferedImage(textureFileName);
		byte[] imgRGBA = getRGBAPixelData(textureImage, true);
		ByteBuffer rgbaBuffer = Buffers.newDirectByteBuffer(imgRGBA);
		int[] textureIDs = new int[1];
		gl.glGenTextures(1, textureIDs, 0);
		int textureID = textureIDs[0];
		gl.glBindTexture(GL_TEXTURE_2D, textureID);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureImage.getWidth(), textureImage.getHeight(), 0, GL_RGBA,
				GL_UNSIGNED_BYTE, rgbaBuffer);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		return textureID;
	}

	private static BufferedImage getBufferedImage(String fileName) {
		BufferedImage img;
		try {
			img = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			System.err.println("Error reading '" + fileName + '"');
			throw new RuntimeException(e);
		}
		return img;
	}

	private static byte[] getRGBAPixelData(BufferedImage img, boolean flip) {
		byte[] imgRGBA;
		int height = img.getHeight(null);
		int width = img.getWidth(null);
		WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4, null);
		ComponentColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
				new int[] { 8, 8, 8, 8 }, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
		// bits, has Alpha, isAlphaPreMultiplied
		// transparency
		// data transfer type
		BufferedImage newImage = new BufferedImage(colorModel, raster, false, null);
		Graphics2D g = newImage.createGraphics();
		if (flip) {
			AffineTransform gt = new AffineTransform();
			gt.translate(0, height);
			gt.scale(1, -1d);
			g.transform(gt);
		}
		g.drawImage(img, null, null);
		g.dispose();
		DataBufferByte dataBuf = (DataBufferByte) raster.getDataBuffer();
		imgRGBA = dataBuf.getData();
		return imgRGBA;
	}

//	public static int loadCubemap(String[] faces) throws IOException {
//	    GL4 gl = (GL4) GLContext.getCurrentGL();
//	    int[] textureID = new int[1];
//	    gl.glGenTextures(1, textureID, 0);
//	    gl.glBindTexture(GL_TEXTURE_CUBE_MAP, textureID[0]);
//
//	    for (int i = 0; i < faces.length; i++) {
//	        TextureData data = TextureIO.newTextureData(gl.getGLProfile(), new File(faces[i]), false, "jpg");
//	        if (data != null) {
//	            gl.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA, data.getWidth(), data.getHeight(), 0,
//	                            GL_RGBA, GL_UNSIGNED_BYTE, data.getBuffer());
//	        } else {
//	            System.out.println("Failed to load: " + faces[i]);
//	        }
//	    }
//
//	    gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
//	    gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//	    gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//	    gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//	    gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
//
//	    return textureID[0];
//	}

	public static int loadCubeMap(String dirName) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		// assumes that the six file names are xp, xn, yp, yn, zp, and zn and are JPG
		// format
//		String topFile = Utils.class.getResource("/" + dirName + "/py.png").getPath().substring(1);
//		String leftFile = Utils.class.getResource("/" + dirName + "/nx.png").getPath().substring(1);
//		String backFile = Utils.class.getResource("/" + dirName + "/nz.png").getPath().substring(1);
//		String rightFile = Utils.class.getResource("/" + dirName + "/px.png").getPath().substring(1);
//		String frontFile = Utils.class.getResource("/" + dirName + "/pz.png").getPath().substring(1);
//		String bottomFile = Utils.class.getResource("/" + dirName + "/ny.png").getPath().substring(1);
		String topFile = Utils.class.getResource("/" + dirName + "/bluecloud_up.jpg").getPath().substring(1);
		String leftFile = Utils.class.getResource("/" + dirName + "/bluecloud_bk.jpg").getPath().substring(1);
		String backFile = Utils.class.getResource("/" + dirName + "/bluecloud_lf.jpg").getPath().substring(1);
		String rightFile = Utils.class.getResource("/" + dirName + "/bluecloud_ft.jpg").getPath().substring(1);
		String frontFile = Utils.class.getResource("/" + dirName + "/bluecloud_rt.jpg").getPath().substring(1);
		String bottomFile = Utils.class.getResource("/" + dirName + "/bluecloud_dn.jpg").getPath().substring(1);
		BufferedImage topImage = getBufferedImage(topFile);
		BufferedImage leftImage = getBufferedImage(leftFile);
		BufferedImage frontImage = getBufferedImage(frontFile);
		BufferedImage rightImage = getBufferedImage(rightFile);
		BufferedImage backImage = getBufferedImage(backFile);
		BufferedImage bottomImage = getBufferedImage(bottomFile);
		// getRGBAPixel is from Chapter 5. This time image NOT flipped because OpenGL
		// does it for us
		byte[] topRGBA = getRGBAPixelData(topImage, false);
		byte[] leftRGBA = getRGBAPixelData(leftImage, false);
		byte[] frontRGBA = getRGBAPixelData(frontImage, false);
		byte[] rightRGBA = getRGBAPixelData(rightImage, false);
		byte[] backRGBA = getRGBAPixelData(backImage, false);
		byte[] bottomRGBA = getRGBAPixelData(bottomImage, false);
		ByteBuffer topWrappedRGBA = ByteBuffer.wrap(topRGBA);
		ByteBuffer leftWrappedRGBA = ByteBuffer.wrap(leftRGBA);
		ByteBuffer frontWrappedRGBA = ByteBuffer.wrap(frontRGBA);
		ByteBuffer rightWrappedRGBA = ByteBuffer.wrap(rightRGBA);
		ByteBuffer backWrappedRGBA = ByteBuffer.wrap(backRGBA);
		ByteBuffer bottomWrappedRGBA = ByteBuffer.wrap(bottomRGBA);
		int[] textureIDs = new int[1];
		gl.glGenTextures(1, textureIDs, 0);
		int textureID = textureIDs[0];
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, textureID);
		gl.glTexStorage2D(GL_TEXTURE_CUBE_MAP, 1, GL_RGBA8, 1024, 1024);
		// attach the image texture to each face of the currently active OpenGL texture
		// ID
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, 0, 0, 1024, 1024, GL_RGBA, GL.GL_UNSIGNED_BYTE,
				rightWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, 0, 0, 1024, 1024, GL_RGBA, GL.GL_UNSIGNED_BYTE,
				leftWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0, 0, 1024, 1024, GL_RGBA, GL.GL_UNSIGNED_BYTE,
				bottomWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, 0, 0, 1024, 1024, GL_RGBA, GL.GL_UNSIGNED_BYTE,
				topWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, 0, 0, 1024, 1024, GL_RGBA, GL.GL_UNSIGNED_BYTE,
				frontWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, 0, 0, 1024, 1024, GL_RGBA, GL.GL_UNSIGNED_BYTE,
				backWrappedRGBA);
		// to help reduce seams
		gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		return textureID;
	}

	private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
		BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
		g.dispose();
		return resizedImage;
	}

	public static TextureData loadTextureData(String textureFileName) {
		try {
			// Truy cập tài nguyên từ thư mục "assets/"
			InputStream stream = Utils.class.getClassLoader().getResourceAsStream("assets/" + textureFileName);
			if (stream == null) {
				System.err.println("Không tìm thấy tệp hình ảnh: " + textureFileName);
				return null;
			}

			// Sử dụng TextureIO để đọc dữ liệu từ InputStream
			return TextureIO.newTextureData(GLProfile.get(GLProfile.GL4), stream, false, null);

		} catch (IOException e) {
			System.err.println("Lỗi khi tải tệp hình ảnh: " + textureFileName);
			e.printStackTrace();
			return null;
		}
	}

	// GOLD material - ambient, diffuse, specular, and shininess
	public static float[] goldAmbient() {
		return (new float[] { 0.2473f, 0.1995f, 0.0745f, 1 });
	}

	public static float[] goldDiffuse() {
		return (new float[] { 0.7516f, 0.6065f, 0.2265f, 1 });
	}

	public static float[] goldSpecular() {
		return (new float[] { 0.6283f, 0.5558f, 0.3661f, 1 });
	}

	public static float goldShininess() {
		return 51.2f;
	}

	// SILVER material - ambient, diffuse, specular, and shininess
	public static float[] silverAmbient() {
		return (new float[] { 0.1923f, 0.1923f, 0.1923f, 1 });
	}

	public static float[] silverDiffuse() {
		return (new float[] { 0.5075f, 0.5075f, 0.5075f, 1 });
	}

	public static float[] silverSpecular() {
		return (new float[] { 0.5083f, 0.5083f, 0.5083f, 1 });
	}

	public static float silverShininess() {
		return 51.2f;
	}

	// BRONZE material - ambient, diffuse, specular, and shininess
	public static float[] bronzeAmbient() {
		return (new float[] { 0.2125f, 0.1275f, 0.0540f, 1 });
	}

	public static float[] bronzeDiffuse() {
		return (new float[] { 0.7140f, 0.4284f, 0.1814f, 1 });
	}

	public static float[] bronzeSpecular() {
		return (new float[] { 0.3935f, 0.2719f, 0.1667f, 1 });
	}

	public static float bronzeShininess() {
		return 25.6f;
	}

	// Water material - ambient, diffuse, specular, and shininess
	public static float[] waterAmbient() {
		return (new float[] { 0.1f, 0.2f, 0.4f });
	}

	public static float[] waterDiffuse() {
		return (new float[] { 1.0f, 1.0f, 1.0f });
	}

	public static float[] waterSpecular() {
		return (new float[] { 0.8f, 0.9f, 1.0f });
	}

	public static float waterShininess() {
		return 128.0f;
	}

	public static void main(String[] args) {
//		System.out.println(Utils.readShaderSource("vertShader5_1.glsl").toString());
		System.out.println(Utils.loadTexture("whale.png"));

	}
}
