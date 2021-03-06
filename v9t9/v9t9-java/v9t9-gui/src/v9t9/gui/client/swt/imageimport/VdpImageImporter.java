/*
  VdpImageImporter.java

  (c) 2011-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.gui.client.swt.imageimport;

import static v9t9.common.hardware.VdpV9938Consts.*;

import org.ejs.gui.images.V99ColorMapUtils;

import v9t9.common.hardware.IVdpChip;
import v9t9.common.hardware.IVdpTMS9918A;
import v9t9.common.hardware.VdpV9938Consts;
import v9t9.common.memory.ByteMemoryAccess;
import v9t9.common.video.IVdpCanvas;
import v9t9.common.video.IVdpCanvasRenderer;
import v9t9.common.video.VdpFormat;
import v9t9.video.imageimport.ImageImportData;

/**
 * @author ejs
 *
 */
public class VdpImageImporter {

	private IVdpChip vdp;
	private IVdpCanvas canvas;
	private ImageImportData data;
	private IVdpCanvasRenderer canvasRenderer;

	/**
	 * @param canvasRenderer 
	 * @param importer 
	 * @param data
	 */
	public VdpImageImporter(IVdpChip vdp, IVdpCanvas canvas, IVdpCanvasRenderer canvasRenderer) {
		this.vdp = vdp;
		this.canvas = canvas;
		this.canvasRenderer = canvasRenderer;
	}

	public void importImageToCanvas(ImageImportData data) {
		synchronized (vdp) {
			synchronized (canvasRenderer) {
				synchronized (canvas) {
					this.data = data;
					setPalette();
					setVideoMemory();
					canvas.markDirty();
				}
			}
		}
	}

	/**
	 * @param data
	 */
	private void setPalette() {
		if (data == null)
			return;
		byte[][] thePalette = data.getThePalette();
		VdpFormat format = canvas.getFormat();
		
		int ncols = format.getNumColors();
		if (ncols < 256) {
			for (int c = 0; c < ncols; c++) {
				vdp.setRegister(VdpV9938Consts.REG_PAL0 + c, V99ColorMapUtils.rgb8ToRgbRBXG(thePalette[c]));
			}
		}
				
	}

	/**
	 * @param data 
	 * @param converted 
	 * 
	 */
	private void setVideoMemory() {
		VdpFormat format = canvas.getFormat();
		
		if (vdp instanceof IVdpTMS9918A) {
			IVdpTMS9918A vdp99 = (IVdpTMS9918A) vdp;
			if (format == VdpFormat.COLOR16_8x1 || format == VdpFormat.COLOR16_8x1_9938) {
				setVideoMemoryBitmapMode(vdp99);
			} 
			else if (format == VdpFormat.COLOR16_8x8) {
				setVideoMemoryGraphicsMode(vdp99);
			}
			else if (format == VdpFormat.COLOR16_1x1) {
				setVideoMemoryV9938BitmapMode(vdp99);
			}
			else if (format == VdpFormat.COLOR256_1x1) {
				setVideoMemoryV9938BitmapMode(vdp99);
			}
			else if (format == VdpFormat.COLOR4_1x1) {
				setVideoMemoryV9938BitmapMode(vdp99);
			}
			else if (format== VdpFormat.COLOR16_4x4) {
				setVideoMemoryMulticolorMode(vdp99);
			}
		}
	}

	/**
	 * @param data 
	 * @param vdp99
	 */
	private void setVideoMemoryMulticolorMode(IVdpTMS9918A vdp99) {
		ByteMemoryAccess patt = vdp99.getByteReadMemoryAccess(vdp99.getPatternTableBase());
		
		for (int y = 0; y < 48; y++) {
			for (int x = 0; x < 64; x += 2) {
				
				byte f = getPixel(x, y);
				byte b = getPixel(x + 1, y);
				
				int poffs = ((y >> 3) << 8) + (y & 7) + ((x >> 1) << 3);  
				//System.out.println("("+y+","+x+") = "+ poffs);
				vdp99.writeAbsoluteVdpMemory(patt.offset + poffs, (byte) ((f << 4) | b));
			}
		}
	}

	protected interface IBitmapModeImportHandler {
		byte createImageDataByte(int x, int row);

		/**
		 * @return
		 */
		int getRowStride();

		/**
		 * @return
		 */
		int getColumnStride();
	}
	

	protected byte getPixel(int x, int y) {
		
		int p;
		if (x < data.getConvertedImage().getWidth() && y < data.getConvertedImage().getHeight())
			p = data.getConvertedImage().getRGB(x, y) & 0xffffff;
		else
			p = 0;
		
		Integer c = data.getPaletteToIndex().get(p);
//			if (c == null && format == VdpFormat.COLOR256_1x1) {
//				c = paletteToIndex.get(paletteToIndex.ceilingKey(p));	// should be fixed now
//			}
		if (c == null) {
			return 0;
		}
		return (byte) (int) c;
	}
	
	class GraphicsMode4ImportHandler implements IBitmapModeImportHandler {
		@Override
		public byte createImageDataByte(int x, int row) {
			
			byte f = getPixel(x, row);
			byte b = getPixel(x + 1, row);
			return (byte) ((f << 4) | b);
		}

		@Override
		public int getRowStride() {
			return 128;
		}

		@Override
		public int getColumnStride() {
			return 2;
		}
	}

	class GraphicsMode5ImportHandler implements IBitmapModeImportHandler {
		@Override
		public byte createImageDataByte(int x, int y) {
			byte p = 0;
			for (int xo = 0; xo < 4; xo++) {
				byte c = getPixel(x + xo, y);
				p |= c << ((3 - xo) * 2);
			}
			return p;
		}

		@Override
		public int getRowStride() {
			return 128;
		}

		@Override
		public int getColumnStride() {
			return 4;
		}
	}

	class GraphicsMode6ImportHandler implements IBitmapModeImportHandler {
		@Override
		public byte createImageDataByte(int x, int y) {
			byte f = getPixel(x, y);
			byte b = getPixel(x + 1, y);
			
			return (byte) ((f << 4) | b);
		}

		@Override
		public int getRowStride() {
			return 256;
		}

		@Override
		public int getColumnStride() {
			return 2;
		}
	}

	class GraphicsMode7ImportHandler implements IBitmapModeImportHandler {
		@Override
		public byte createImageDataByte(int x, int y) {
			
			return getPixel(x, y);
		}

		@Override
		public int getRowStride() {
			return 256;
		}

		@Override
		public int getColumnStride() {
			return 1;
		}
		
	}
	/**
	 * @param data 
	 * @param vdp
	 */
	private void setVideoMemoryV9938BitmapMode(IVdpTMS9918A vdp) {
		IBitmapModeImportHandler handler;
		int mx;
		switch (vdp.getModeNumber()) {
		case MODE_GRAPHICS4:
			handler = new GraphicsMode4ImportHandler();
			mx = 256;
			break;
		case MODE_GRAPHICS5:
			handler = new GraphicsMode5ImportHandler();
			mx = 512;
			break;
		case MODE_GRAPHICS6:
			handler = new GraphicsMode6ImportHandler();
			mx = 512;
			break;
		case MODE_GRAPHICS7:
			handler = new GraphicsMode7ImportHandler();
			mx = 256;
			break;
		default:
			throw new IllegalStateException();	
		}
		
		int ystep = vdp.isInterlacedEvenOdd() ? 2 : 1;
		int my =  (vdp.getRegister(9) & 0x80) != 0 ? 212 : 192;
		int graphicsPageSize = vdp.getGraphicsPageSize();
		
		int colstride = handler.getColumnStride();
		int rowstride = handler.getRowStride();
		
		for (int eo = 0; eo < ystep; eo++) {
			ByteMemoryAccess patt = vdp.getByteReadMemoryAccess(vdp.getPatternTableBase()
					^ (eo != 0 ? graphicsPageSize : 0));
			for (int y = 0; y < my; y++) {
				int row = y * ystep + eo;
				for (int x = 0; x < mx; x += colstride) {
					
					byte byt = handler.createImageDataByte(x, row);
					
					int poffs = y * rowstride + (x / colstride); 
					vdp.writeAbsoluteVdpMemory(patt.offset + poffs, byt);
				}
			}
		}
		
	}
	/**
	 * @param data 
	 * @param vdp
	 */
	private void setVideoMemoryGraphicsMode(IVdpTMS9918A vdp) {
		ByteMemoryAccess screen = vdp.getByteReadMemoryAccess(vdp.getScreenTableBase());
		ByteMemoryAccess patt = vdp.getByteReadMemoryAccess(vdp.getPatternTableBase());
		ByteMemoryAccess color = vdp.getByteReadMemoryAccess(vdp.getColorTableBase());
		
		// assume char 255 is not used: fill screen with this and clear block 0xff for background
		for (int i = 0; i < 768; i++)
			vdp.writeAbsoluteVdpMemory(screen.offset + i, (byte) 0xff);

		for (int i = 0; i < 8; i++)
			vdp.writeAbsoluteVdpMemory(patt.offset + 255*8 + i, (byte) 0x0);

		byte b = 0;
		
		byte cb = (byte) vdp.getRegister(7);
		cb = (byte) ((cb & 0xf) | 0x10);
		
		b = (byte) ((cb >> 0) & 0xf);

		for (int i = 0; i < 32; i++)
			vdp.writeAbsoluteVdpMemory(color.offset + i, cb);

		int width = data.getConvertedImage().getWidth();
		int height = data.getConvertedImage().getHeight();
		
		int yoffs = ((192 - height) / 2) & ~0x7;
		int xoffs = ((256 - width) / 2) & ~0x7;
		for (int y = 0; y < height; y ++) {
			for (int x = 0; x < width; x+= 8) {
				int ch = ((y >> 3) * ((width + 7) >> 3)) + (x >> 3);
				if (ch > 0xff)
					throw new IllegalStateException();
				int choffs = (((y + yoffs) >> 3) << 5) + ((x + xoffs) >> 3);
				
				vdp.writeAbsoluteVdpMemory(screen.offset + choffs, (byte) ch);
				
				int poffs = (ch << 3) + (y & 7);
				
				byte p = 0;
				
				for (int xo = 0; xo < 8; xo++) {
					byte c = getPixel(x + xo, y);
					if (c != b) {
						p |= 0x80 >> xo;
					}
				}

				vdp.writeAbsoluteVdpMemory(patt.offset + poffs, p);
			}
		}
		
	}

	/**
	 * @param data 
	 * @param vdp
	 */
	private void setVideoMemoryBitmapMode(IVdpTMS9918A vdp) {
		boolean isMono = vdp.isBitmapMonoMode();
		
		ByteMemoryAccess screen = vdp.getByteReadMemoryAccess(vdp.getScreenTableBase());
		ByteMemoryAccess patt = vdp.getByteReadMemoryAccess(vdp.getPatternTableBase());
		ByteMemoryAccess color = vdp.getByteReadMemoryAccess(vdp.getColorTableBase());
		
		byte f = 0, b = 0;
		
		if (isMono) {
			f = (byte) ((vdp.getRegister(7) >> 4) & 0xf);
			b = (byte) ((vdp.getRegister(7) >> 0) & 0xf);
		}

		for (int y = 0; y < 192; y++) {
			for (int x = 0; x < 256; x += 8) {
				
				int choffs = ((y >> 6) << 8) + ((y & 0x3f) >> 3) * 32 + (x >> 3);
				int ch = choffs & 0xff;
				
				if ((y & 7) == 0) {
					vdp.writeAbsoluteVdpMemory(screen.offset + choffs, (byte) ch);
				}

				int poffs = (y >> 6) * 0x800 + (ch << 3) + (y & 7);
				
				byte p = 0;
				
				if (!isMono) {
					// in color mode, by convention keep the foreground color
					// as the lesser color.
					f = getPixel(x, y);
					p = (byte) 0x80;
				
					boolean gotBG = false;
					for (int xo = 1; xo < 8; xo++) {
						byte c = getPixel(x + xo, y);
						if (c == f) {
							p |= 0x80 >> xo;
						} else {
							if (!gotBG) {
								if (c < f) {
									b = f;
									f = c;
									p ^= (0xff << (8 - xo));
									p |= 0x80 >> xo;
								} else {
									b = c;
								}
								gotBG = true;
							}
						}
					}
					
					vdp.writeAbsoluteVdpMemory(color.offset + poffs, (byte) ((f << 4) | (b)));
				} else {
					// in mono mode, mapper has matched with fg and bg from vr7
					for (int xo = 0; xo < 8; xo++) {
						byte c = getPixel(x + xo, y);
						if (c == f) {
							p |= 0x80 >> xo;
						}
					}
				}

				vdp.writeAbsoluteVdpMemory(patt.offset + poffs, p);
			}
		}
	}

}
