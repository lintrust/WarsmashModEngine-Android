package com.etheller.warsmash.util;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.viewer5.handlers.ResourceInfo;
import com.etheller.warsmash.viewer5.handlers.tga.TgaFile;
import com.google.code.appengine.imageio.spi.IIORegistry;
import com.google.common.base.Strings;
import com.hiveworkshop.blizzard.blp.BLPReaderSpi;
import com.lin.imageio.plugins.jpeg.JPEGImageReaderSpi;

/**
 * Uses AWT stuff
 */
public final class ImageUtils {
	private static final int BYTES_PER_PIXEL = 4;
	public static final String DEFAULT_ICON_PATH = "ReplaceableTextures\\CommandButtons\\BTNTemp.blp";

	static {
		// registration is important!!!
		if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
			javax.imageio.spi.IIORegistry.getDefaultInstance().registerServiceProvider(new BLPReaderSpi());
		}
		IIORegistry.getDefaultInstance().registerServiceProvider(new BLPReaderSpi());
		IIORegistry.getDefaultInstance().registerServiceProvider(new JPEGImageReaderSpi());
	}

	public static Texture getAnyExtensionTexture(final DataSource dataSource, final String path) {
		BufferedImage image;
		try {
//			var extension = new FileHandle(path).extension();
//			if (!Strings.isNullOrEmpty(extension)) {
//
//			}
			if (dataSource.has(path)) {
				DataSource ds = ((CompoundDataSource) dataSource).getDataSource(path);
				var res = new ResourceInfo(ds, path, path);
				return getTexture(res);
			}
			else {
				System.err.println("[RES_NOT_FOUND] "+path);
			}
//			final AnyExtensionImage imageInfo = getAnyExtensionImageFixRGB(dataSource, path, "texture");
//			image = imageInfo.getImageData();
//			if (image != null) {
//				return ImageUtils.getTexture(image, imageInfo.isNeedsSRGBFix());
//			}
		}
		catch (final IOException e) {
			return null;
		}
		return null;
	}

	public static AnyExtensionImage getAnyExtensionImageFixRGB(final DataSource dataSource, final String path,
			final String errorType) throws IOException {
		if (path.toLowerCase().endsWith(".blp")) {
			try (InputStream stream = dataSource.getResourceAsStream(path)) {
				if (stream == null) {
					final String tgaPath = path.substring(0, path.length() - 4) + ".tga";
					try (final InputStream tgaStream = dataSource.getResourceAsStream(tgaPath)) {
						if (tgaStream != null) {
							final BufferedImage tgaData = TgaFile.readTGA(tgaPath, tgaStream);
							return new AnyExtensionImage(false, tgaData);
						}
						else {
							final String ddsPath = path.substring(0, path.length() - 4) + ".dds";
							try (final InputStream ddsStream = dataSource.getResourceAsStream(ddsPath)) {
								if (ddsStream != null) {
									final BufferedImage image = ImageIO.read(ddsStream);
									return new AnyExtensionImage(false, image);
								}
								else {
									throw new IllegalStateException("Missing " + errorType + ": " + path);
								}
							}
						}
					}
				}
				else {
					final BufferedImage image = ImageIO.read(stream);
					return new AnyExtensionImage(true, image);
				}
			}
		}
		else {
			throw new IllegalStateException("Missing " + errorType + ": " + path);
		}
	}

	public static final class AnyExtensionImage {
		private final boolean needsSRGBFix;
		private final BufferedImage imageData;

		public AnyExtensionImage(final boolean needsSRGBFix, final BufferedImage imageData) {
			this.needsSRGBFix = needsSRGBFix;
			this.imageData = imageData;
		}

		public BufferedImage getImageData() {
			return this.imageData;
		}

		public BufferedImage getRGBCorrectImageData() {
			return this.needsSRGBFix ? forceBufferedImagesRGB(this.imageData) : this.imageData;
		}

		public boolean isNeedsSRGBFix() {
			return this.needsSRGBFix;
		}
	}

	public static Texture getTexture(final BufferedImage image, final boolean sRGBFix) {
		final int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

		// 4
		// for
		// RGBA,
		// 3
		// for
		// RGB

		final Pixmap pixmap = sRGBFix ? new Pixmap(image.getWidth(), image.getHeight(), Format.RGBA8888) {
			@Override
			public int getGLInternalFormat() {
				return GL30.GL_SRGB8_ALPHA8;
			}
		} : new Pixmap(image.getWidth(), image.getHeight(), Format.RGBA8888);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				final int pixel = pixels[(y * image.getWidth()) + x];
				pixmap.drawPixel(x, y, (pixel << 8) | (pixel >>> 24));
			}
		}
		final Texture texture = new Texture(pixmap);
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}

	public static Texture getTexture(ResourceInfo res) throws IOException {
		final ResourceInfo info = res;
		String path = info.getCachePath("blp2png", ".png");
		var file = Gdx.files.external(path);
		if (!file.exists()) {
			file.parent().mkdirs();

			ByteArrayOutputStream bos = new ByteArrayOutputStream(100 << 10);
			var fs = new FileOutputStream(file.file()) {
				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					super.write(b, off, len);
					bos.write(b, off, len);
				}

				@Override
				public void flush() throws IOException {
					bos.flush();
					super.flush();
				}
			};
			com.google.code.appengine.imageio.ImageIO.write(com.google.code.appengine.imageio.ImageIO.read(res.getResourceAsStream()), "png", fs);

			var pngData = bos.toByteArray();
			Pixmap pixmap = new Pixmap(pngData, 0, pngData.length);
			final Texture texture = new Texture(pixmap);

			pixmap.dispose();
			return texture;
//				System.out.println("[WRITE_BLP_PNG] " + file.path());
		}
		else {
			// load converted png from cache for the blp.
//			System.out.println("[LOAD_BLP_PNG] " + file.path());
			Pixmap pixmap = new Pixmap(file);
			final Texture texture = new Texture(pixmap);
			pixmap.dispose();
			return texture;
		}
	}

	public static Texture getTextureNoColorCorrection(final BufferedImage image) {
		final int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

		// 4
		// for
		// RGBA,
		// 3
		// for
		// RGB

		final Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Format.RGBA8888);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				final int pixel = pixels[(y * image.getWidth()) + x];
				pixmap.drawPixel(x, y, (pixel << 8) | (pixel >>> 24));
			}
		}
		final Texture texture = new Texture(pixmap);
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}

	public static Buffer getTextureBuffer(final BufferedImage image) {

		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int[] pixels = new int[imageWidth * imageHeight];
		image.getRGB(0, 0, imageWidth, imageHeight, pixels, 0, imageWidth);

		final ByteBuffer buffer = ByteBuffer.allocateDirect(imageWidth * imageHeight * BYTES_PER_PIXEL)
										  .order(ByteOrder.nativeOrder());
		// 4
		// for
		// RGBA,
		// 3
		// for
		// RGB

		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				final int pixel = pixels[(y * imageWidth) + x];
				buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red component
				buffer.put((byte) ((pixel >> 8) & 0xFF)); // Green component
				buffer.put((byte) (pixel & 0xFF)); // Blue component
				buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha component.
				// Only for RGBA
			}
		}

		buffer.flip();
		return buffer;
	}

	/**
	 * Convert an input buffered image into sRGB color space using component values
	 * directly instead of performing a color space conversion.
	 *
	 * @param in Input image to be converted.
	 * @return Resulting sRGB image.
	 */
	public static BufferedImage forceBufferedImagesRGB(final BufferedImage in) {
		// Resolve input ColorSpace.
		final ColorSpace inCS = in.getColorModel().getColorSpace();
		final ColorSpace sRGBCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		if (inCS == sRGBCS) {
			// Already is sRGB.
			return in;
		}
		if (inCS.getNumComponents() != sRGBCS.getNumComponents()) {
			throw new IllegalArgumentException("Input color space has different number of components from sRGB.");
		}

		// Draw input.
		final ColorModel lRGBModel = new ComponentColorModel(inCS, true, false, Transparency.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);
		final ColorModel sRGBModel = new ComponentColorModel(sRGBCS, true, false, Transparency.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);
		final BufferedImage lRGB = new BufferedImage(lRGBModel,
				lRGBModel.createCompatibleWritableRaster(in.getWidth(), in.getHeight()), false, null);
		for (int i = 0; i < in.getWidth(); i++) {
			for (int j = 0; j < in.getHeight(); j++) {
				lRGB.setRGB(i, j, in.getRGB(i, j));
			}
		}

		// Convert to sRGB.
		final BufferedImage sRGB = new BufferedImage(sRGBModel, lRGB.getRaster(), false, null);

		return sRGB;
	}

	private ImageUtils() {
	}
}
