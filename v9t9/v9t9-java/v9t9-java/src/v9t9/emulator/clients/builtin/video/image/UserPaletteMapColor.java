package v9t9.emulator.clients.builtin.video.image;

import org.ejs.coffee.core.utils.Pair;

import v9t9.emulator.clients.builtin.video.ColorMapUtils;

class UserPaletteMapColor extends BasePaletteMapper {
	
	public UserPaletteMapColor(byte[][] thePalette, int firstColor, int numColors,
			boolean isGreyscale) {
		super(thePalette, firstColor, numColors, false, isGreyscale);
	}
	
	@Override
	protected boolean isFixedPalette() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.ImageDataCanvas.IMapColor#mapColor(int, int[])
	 */
	@Override
	public int mapColor(int pixel, int[] distA) {
		Pair<Integer, Integer> info = getCloseColor(pixel);
		distA[0] = info.second;
		return info.first;
	}
	
	/**
	 * Get the closest color by sheer brute force 
	 * @param pixel
	 * @return
	 */
	private Pair<Integer, Integer> getCloseColor(int pixel) {
		if (isGreyscale) {
			return ColorMapUtils.getClosestColorByLumDistance(palette, firstColor, numColors, pixel);
		}
		return ColorMapUtils.getClosestColorByDistance(palette, firstColor, numColors, pixel, -1);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.ImageImport.IMapColor#getClosestColor(int[])
	 */
	@Override
	public int getClosestPalettePixel(int x, int y, int pixel) {
		Pair<Integer, Integer> info = getCloseColor(pixel);
		return getPalettePixels()[info.first];
	}
}