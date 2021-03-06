/*
  BasePaletteMapper.java

  (c) 2011-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package org.ejs.gui.images;


abstract public class BasePaletteMapper implements IPaletteMapper {
	private final boolean canSetPalette;
//	private int minDist;
	protected byte[][] palette;
	protected int numColors;
	protected int firstColor;
	protected final boolean isColorMappedGreyscale;
	
	protected int[] palettePixels;

	public BasePaletteMapper(byte[][] palette, int firstColor, int numColors, boolean canSetPalette, boolean isGreyscale) {
		this.palette = palette;
		this.isColorMappedGreyscale = isGreyscale;
		
		this.firstColor = firstColor;
		this.numColors = numColors;
		this.canSetPalette = canSetPalette;
		
//		minDist = Integer.MAX_VALUE;
//		for (int c = 0; c < numColors; c++) {
//			int cpixel = ColorMapUtils.rgb8ToPixel(palette[c]);
//			for (int d = c + 1; d < numColors; d++) {
//				int dist;
//				if (!isGreyscale)
//					dist = ColorMapUtils.getRGBDistance(palette[d], cpixel);
//				else
//					dist = ColorMapUtils.getRGBLumDistance(palette[d], cpixel);
//				if (dist > 0 && dist < minDist)
//					minDist = dist;
//			}
//		}
		
	}
	

	@Override
	public byte[][] getPalette() {
		return palette;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.ImageImport.IMapColor#getNumColors()
	 */
	@Override
	public int getNumColors() {
		return numColors;
	}
	
	@Override
	public int getMinimalPaletteDistance() {
		//if (canSetPalette)
			// 0xff --> 0xe0 for R, G, B
			return 0x20*0x20 * 3;

		//return minDist;
	}
	
	protected boolean isFixedPalette() {
		return !canSetPalette;
	}


	/**
	 * Get RGB pixel for each palette entry.
	 * The pixels are calculated lazily in case the
	 * palette changes (this is called only after the
	 * mapping is complete).
	 * @return
	 */
	protected int[] getPalettePixels() {
		if (palettePixels == null) {

			palettePixels = new int[numColors];
			
			for (int x = 0; x < numColors; x++) {
				byte[] nrgb = palette[x];
				if (isColorMappedGreyscale)
					nrgb = V99ColorMapUtils.getRgbToGreyForGreyscaleMode(nrgb);
				palettePixels[x] = ColorMapUtils.rgb8ToPixel(nrgb);
			}
		}
		return palettePixels;
	}
	
	@Override
	public int getPalettePixel(int c) {
		return getPalettePixels()[c]; //ColorMapUtils.rgb8ToPixel(palette[c]);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.image.IPaletteColorMapper#getClosestPalettePixel(int, int, int)
	 */
	@Override
	public int getClosestPalettePixel(int x, int y, int pixel) {
		int v = getClosestPaletteEntry(x, y, pixel);
		return v >= 0 ? getPalettePixel(v) : pixel;
	}
}