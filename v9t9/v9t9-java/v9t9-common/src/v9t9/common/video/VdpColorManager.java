/*
  VdpColorManager.java

  (c) 2011-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.video;

import java.util.Arrays;

import org.ejs.gui.images.ColorMapUtils;
import org.ejs.gui.images.V99ColorMapUtils;

import ejs.base.utils.ListenerList;

/**
 * Handle all the needs of the VDP color model -- stock palettes,
 * greyscale handling, V9938 4-color mode & sprite palette handling, etc.
 * @author ejs
 *
 */
public class VdpColorManager {
	
	public interface IColorListener {
		void colorsChanged();
	}

	private ListenerList<IColorListener> listeners = new ListenerList<IColorListener>();
	
	protected int clearColor;

	protected int clearColor1;
	
	protected byte colorPalette[][];
	protected byte greyPalette[][];
	protected byte altSpritePalette[][];
	protected byte altSpritePaletteGrey[][];
	protected byte[][] thePalette;
	protected byte[][] theSpritePalette;
	protected boolean paletteMappingDirty = true;
	protected boolean clearFromPalette;
	protected boolean useAltSpritePalette;
	private boolean isGreyscale;

	private int bg;

	private int fg;

	protected static final byte[][] stockPaletteEd = {
		/* 0 */ { 0x00, 0x00, 0x00 }, 
		/* 1 */ { 0x00, 0x00, 0x00 },
		/* 2 */ { 0x40, (byte) 0xb0, 0x40 }, 
		/* 3 */ { 0x60, (byte) 0xc0, 0x60 },
		/* 4 */ { 0x40, 0x40, (byte) 0xc0 }, 
		/* 5 */ { 0x60, 0x60, (byte) 0xf0 },
		/* 6 */ { (byte) 0xc0, 0x40, 0x40 }, 
		/* 7 */ { 0x40, (byte) 0xf0, (byte) 0xf0 },
		/* 8 */ { (byte) 0xf0, 0x40, 0x40 }, 
		/* 9 */ { (byte) 0xff, (byte) 0x80, 0x60 },
		/* 10 */ { (byte) 0xf0, (byte) 0xc0, 0x40 },
		/* 11 */ { (byte) 0xff, (byte) 0xe0, 0x60 }, 
		/* 12 */ { 0x40, (byte) 0x80, 0x40 },
		/* 13 */ { (byte) 0xc0, 0x40, (byte) 0xc0 },
		/* 14 */ { (byte) 0xd0, (byte) 0xd0, (byte) 0xd0 },
		/* 15 */ { (byte) 0xff, (byte) 0xff, (byte) 0xff }, 
	};
	public static final byte[][] stockPalette = {
		/* 0 */ { 0x00, 0x00, 0x00 }, 
		/* 1 */ { 0x00, 0x00, 0x00 },
		/* 2 */ { 70, (byte) 183, 62 }, 
		/* 3 */ { 124, (byte) 208, 108 },
		/* 4 */ { 99, 91, (byte) 169 }, 
		/* 5 */ { 127, 113, (byte) 255 },
		/* 6 */ { (byte) 183, 98, 73 }, 
		/* 7 */ { 92, (byte) 199, (byte) 239 },
		/* 8 */ { (byte) 217, 107, 73 }, 
		/* 9 */ { (byte) 253, (byte) 142, 108 },
		/* 10 */ { (byte) 195, (byte) 206, 66 },
		/* 11 */ { (byte) 211, (byte) 219, 117 }, 
		/* 12 */ { 61, (byte) 160, 47 },
		/* 13 */ { (byte) 183, 99, (byte) 199 },
		/* 14 */ { (byte) 204, (byte) 204, (byte) 204 },
		/* 15 */ { (byte) 0xff, (byte) 0xff, (byte) 0xff }, 
			
	};
	protected static final byte[][] stockPaletteWashed = {
		/* 0 */ { 0x00, 0x00, 0x00 }, 
		/* 1 */ { 0x00, 0x00, 0x00 },
		/* 2 */ { 82, (byte) 190, 71 }, 
		/* 3 */ { (byte) 134, (byte) 213, 121 },
		/* 4 */ { 111, 103, (byte) 178 }, 
		/* 5 */ { (byte) 139, 123, (byte) 255 },
		/* 6 */ { (byte) 190, 110, 85 }, 
		/* 7 */ { 105, (byte) 205, (byte) 241 },
		/* 8 */ { (byte) 221, 118, 85 }, 
		/* 9 */ { (byte) 253, (byte) 153, 121 },
		/* 10 */ { (byte) 202, (byte) 212, 77 },
		/* 11 */ { (byte) 217, (byte) 222, (byte) 128 }, 
		/* 12 */ { 75, (byte) 171, 61 },
		/* 13 */ { (byte) 190, 110, (byte) 205 },
		/* 14 */ { (byte) 210, (byte) 210, (byte) 210 },
		/* 15 */ { (byte) 0xff, (byte) 0xff, (byte) 0xff }, 
	};
	public static final byte[][] stockPaletteV9938 = {
		fromRBG("000"), // 0
	    fromRBG("000"),  // 1
	    fromRBG("116"),  // 2
	    fromRBG("337"),  // 3
	    fromRBG("171"),  // 4
	    fromRBG("273"),  // 5
	    fromRBG("511"),  // 6
	    fromRBG("276"),  // 7
	    fromRBG("711"),  // 8
	    fromRBG("733"),  // 9
	    fromRBG("616"),  // A
	    fromRBG("646"),  // B
	    fromRBG("114"),  // C
	    fromRBG("652"),  // D
	    fromRBG("555"),  // E
	    fromRBG("777"),  // F
	};
	protected static final byte[][] altSpritePaletteGBR = {
		{ 0, 0, 0 },
		{ 0, 0, 2 },
		{ 0, 3, 0 },
		{ 0, 3, 2 },
		{ 3, 0, 0 },
		{ 3, 0, 2 },
		{ 3, 3, 0 },
		{ 3, 3, 2 },
		{ 4, 7, 2 },
		{ 0, 0, 7 },
		{ 0, 7, 0 },
		{ 0, 7, 7 },
		{ 7, 0, 0 },
		{ 7, 0, 7 },
		{ 7, 7, 0 },
		{ 7, 7, 7 }
	};

	private static byte[] fromRBG(String hex) {
		int r = Integer.parseInt(hex.substring(0, 1), 16);
		int b = Integer.parseInt(hex.substring(1, 2), 16);
		int g = Integer.parseInt(hex.substring(2, 3), 16);
		return V99ColorMapUtils.getGRB333(g, r, b);
	}

	public static final byte[][][] palettes() {
		return new byte[][][] {
			stockPaletteEd,
			stockPaletteV9938
		};
	}

	public static byte[][][] allPalettes() {
		return new byte[][][] {
			stockPalette,
			stockPaletteEd,
			stockPaletteV9938,
			stockPaletteWashed
		};
	}
	
	/**
	 * @param vdp 
	 * 
	 */
	public VdpColorManager() {

		colorPalette = new byte[16][];
    	byte[][] stockpalette = stockPalette;
		for (int i = 0; i < 16; i++)
    		colorPalette[i] = Arrays.copyOf(stockpalette[i], 3); 

    	greyPalette = new byte[16][];
    	for (int i = 0; i < 16; i++)
    		greyPalette[i] = ColorMapUtils.rgbToGrey(stockpalette[i]);
    	
    	altSpritePalette = new byte[16][];
    	altSpritePaletteGrey = new byte[16][];
    	for (int i = 0; i < 16; i++) {
    		altSpritePalette[i] = V99ColorMapUtils.getGRB333(
    				altSpritePaletteGBR[i][0], altSpritePaletteGBR[i][1], altSpritePaletteGBR[i][2]);
    		altSpritePaletteGrey[i] = ColorMapUtils.rgbToGrey(altSpritePalette[i]);
    	}
    	
    	setGreyscale(false);
	}

	/** Get the RGB triple for the palette entry. */
	public byte[] getRGB(int idx) {
		if (idx == 0 && !clearFromPalette)
			idx = clearColor;
		if (idx >= thePalette.length)
			idx = 0;
		return thePalette[idx];
	}

	/** Get the RGB triple for the palette entry for a sprite. */
	public byte[] getSpriteRGB(int idx) {
		if (idx == 0 && !clearFromPalette)
			idx = clearColor;
		if (idx >= thePalette.length)
			return null;
		return theSpritePalette[idx];
	}

	/** Set the RGB triple for the palette entry. */
	public void setRGB(int idx, byte[] rgb) {
		if (colorPalette[idx] == null)
			colorPalette[idx] = new byte[3];
		colorPalette[idx][0] = rgb[0];
		colorPalette[idx][1] = rgb[1];
		colorPalette[idx][2] = rgb[2];
		
		greyPalette[idx] = ColorMapUtils.rgbToGrey(rgb);
		
		paletteMappingDirty = true;
	}

	public void setRGB333(int idx, byte[] rgb) {
		byte[] low = V99ColorMapUtils.getMapForRGB333(rgb);
		setGRB333(idx, low[1] >> 5, low[0] >> 5, low[2] >> 5); 
	}
	
	/** Set the RGB triple for the palette entry, using 3-bit RGB (usually from a palette). */
	public void setGRB333(int idx, int g, int r, int b) {
		setRGB(idx, V99ColorMapUtils.getGRB333(g, r, b));
	}

	/** Get the 8-bit RGB values from a packed 3-3-2 GRB byte */
	public void getGRB332(byte[] rgb, byte grb) {
		V99ColorMapUtils.getGRB332(rgb, grb, isGreyscale);
	}

	public byte[] getStockRGB(int i) {
		return stockPalette[i];
	}

	public void setGreyscale(boolean b) {
		if (isGreyscale != b || thePalette == null) {
			this.isGreyscale = b;
			thePalette = b ? greyPalette : colorPalette;
			theSpritePalette = useAltSpritePalette ? (b ? altSpritePaletteGrey : altSpritePalette) : thePalette;
			paletteMappingDirty = true;
			fireChanged();
		}
	}

	public boolean isGreyscale() {
		return isGreyscale;
	}

	public byte[][] getPalette() {
		return thePalette;
	}

	/**
	 * @return the colorPalette
	 */
	public byte[][] getColorPalette() {
		return colorPalette;
	}

	public int getClearColor() {
		return clearColor;
	}
	
	/**
	 * @return the clearColor1
	 */
	public int getClearColor1() {
		return clearColor1;
	}

	/**
	 * Tell whether the color 0 is transparent or a color in the
	 * palette.
	 * @param b true: clear (color 0) is a palette color, false: transparent
	 */
	public void setClearFromPalette(boolean b) {
		if (clearFromPalette != b) {
			clearFromPalette = b;
			fireChanged();
		}
	}

	/**
	 * Tell whether the color 0 is transparent or a color in the
	 * palette.
	 * @return true: clear (color 0) is a palette color, false: transparent
	 */
	public boolean isClearFromPalette() {
		return clearFromPalette;
	}


	/**
	 * Set the real color that the "clear" color has
	 * @param c 1-15 for a real color or 0 for transparent, or some other value if supported 
	 */
	public void setClearColor(int c) {
		if (clearColor != c) {
			this.clearColor = c;
			fireChanged();
		}
	}

	/**
	 * Set the real color for the "clear" color
	 * in an even-odd tiling mode
	 * @param c
	 */
	public void setClearColor1(int c) {
		if (clearColor1 != c) {
			this.clearColor1 = c;
			fireChanged();
		}
	}

	final public int getFourColorModeColor(int idx, boolean even) {
		if (idx != 0 || clearFromPalette) 
			return idx;
		return even ? clearColor : clearColor1;
	}

	/**
	 * @param b
	 */
	public void useAltSpritePalette(boolean b) {
		if (useAltSpritePalette != b) {
			paletteMappingDirty = true;
			useAltSpritePalette = b;
			setGreyscale(isGreyscale());	// reset sprite palette 
			
			fireChanged();
		}
	}
	
	public boolean isStandardPalette() {
		boolean isStandardPalette = false;
		for (byte[][] palette : VdpColorManager.allPalettes()) {
			if (palette.length == thePalette.length) {
				boolean match = true;
				for (int i = 0; i < palette.length; i++) {
					if (!Arrays.equals(palette[i], thePalette[i])) {
						match = false;
						break;
					}
				}
				if (match) {
					isStandardPalette = true;
					break;
				}
			}
		}
		return isStandardPalette;
	}
	
	public void setForegroundBackground(int fg, int bg) {
		if (this.fg != fg || this.bg != bg) {
			this.fg = fg;
			this.bg = bg;
			fireChanged();
		}
	}
	public int getForeground() {
		return fg;
	}
	public void setForeground(int fg) {
		if (this.fg != fg) {
			this.fg = fg;
			fireChanged();
		}
	}
	public int getBackground() {
		return bg;
		
	}
	public void setBackground(int bg) {
		if (this.bg != bg) {
			this.bg = bg;
			fireChanged();
		}
	}
	
	public void addListener(IColorListener listener) {
		listeners.add(listener);
	}
	public void removeListener(IColorListener listener) {
		listeners.remove(listener);
	}
	
	protected void fireChanged() {
		if (!listeners.isEmpty()) {
			listeners.fire(new ListenerList.IFire<VdpColorManager.IColorListener>() {

				@Override
				public void fire(IColorListener listener) {
					listener.colorsChanged();
				}
			});
		}
	}
}
