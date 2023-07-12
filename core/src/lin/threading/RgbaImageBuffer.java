package lin.threading;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.utils.ByteArray;
import com.google.code.appengine.imageio.ImageIO;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteSource;
import org.apache.commons.imaging.formats.tiff.TiffImageData;
import org.apache.harmony.luni.util.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RgbaImageBuffer {
	private final int height;
	private final int width;
	private int[] rgbArray;
	private Pixmap pixmap;
	private boolean completed;

	private static byte[] intToByte2(int intValue) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (intValue >> 24);
		bytes[1] = (byte) (intValue >> 16);
		bytes[2] = (byte) (intValue >> 8);
		bytes[3] = (byte) (intValue);
		return bytes;
	}

	private static int byteToInt2(byte[] bytes) {
		return bytes[0] << 24 & 0xFF000000 |
					   bytes[1] << 16 & 0x00FF0000 |
					   bytes[2] << 8 & 0x0000FF00 |
					   bytes[3] & 0x000000FF;
	}

	public RgbaImageBuffer(int[] rgbArray, ByteBuffer buff, int width, int height, int bytesPerPixel) {
		this.totalBytes = width * height * bytesPerPixel;
		this.rgbArray = rgbArray == null ? new int[width * height] : rgbArray;
		this.buffer = buff != null ? buff : ByteBuffer.allocateDirect(totalBytes + 12)
													.order(ByteOrder.nativeOrder());
		this.buffer.mark();
//		this.buffer.putInt(width);
//		this.buffer.putInt(height);
//		this.buffer.putInt(Pixmap.Format.RGBA8888.ordinal());
		this.bytes = new byte[totalBytes];
		this.width = width;
		this.height = height;
	}

	private byte[] bytes;
	private final int totalBytes;
	private final ByteBuffer buffer;

	public byte[] getBytes() {
		return bytes;
	}

	public int[] getRgb() {
		return this.rgbArray;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public Pixmap getPixmap() throws IOException {
//		if (this.pixmap == null) {
//			var bs = new ByteArrayOutputStream();
//			DataOutputStream outputStream = new DataOutputStream(bs);
//			outputStream.writeInt(width);
//			outputStream.writeInt(height);
//			outputStream.writeInt(Pixmap.Format.RGBA8888.ordinal());
//			outputStream.flush();
//			bs.write(this.bytes);
//			ImageIO.write()
//			byte[] data = bs.toByteArray();
//			this.pixmap = new Pixmap(width,height,Pixmap.Format.RGBA8888);
//			PixmapPacker p = new PixmapPacker()
//		}
//		return this.pixmap;
		throw new NotImplementedException();
	}

	public int pixelRead(int offset, int x, int y, int pixel) {
		this.bytes[offset * 4 + 0] = (byte) ((pixel >> 16) & 0xFF);
		this.bytes[offset * 4 + 1] = (byte) ((pixel >> 8) & 0xFF);
		this.bytes[offset * 4 + 2] = (byte) ((pixel >> 0) & 0xFF);
		this.bytes[offset * 4 + 3] = (byte) ((pixel >> 24) & 0xFF);
		rgbArray[offset] = pixel;
		return pixel;
	}

	public void complete() {
		buffer.put(this.bytes);
		buffer.flip();
		int r = buffer.remaining();
		this.completed = true;
	}
}
