/**
 * 
 */
package v9t9.video.imageimport;


import org.ejs.gui.images.ColorOctree;

import v9t9.common.hardware.IVdpChip;
import v9t9.common.hardware.IVdpTMS9918A;
import v9t9.common.video.IVdpCanvas;
import v9t9.common.video.VdpFormat;
import ejs.base.properties.FieldProperty;
import ejs.base.properties.PropertySource;

/**
 * @author ejs
 *
 */
public class ImageImportOptions {

	public enum Dither {
		NONE("None"),
		ORDERED("Ordered"),
		//ORDERED2("Ordered2"),
		FS("Floyd-Steinberg");
		
		private final String label;

		private Dither(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}

	public enum Palette {
		STANDARD("Standard"),
		CURRENT("Current"),
		OPTIMIZED("Optimized");
		
		private final String label;

		private Palette(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	protected boolean asGreyScale;
	protected Palette paletteOption = Palette.OPTIMIZED;
	protected boolean ditherMono;
	protected boolean isMonoMode;
	protected Dither ditherType = Dither.NONE;
	
	protected ColorOctree octree;
	
	private FieldProperty paletteOptionProperty;
	private FieldProperty ditheringProperty;
	private FieldProperty ditherMonoProperty;
	protected IVdpCanvas canvas;
	protected IVdpChip vdp;
	private boolean canSetPalette;
	
	/**
	 * @param iVdpChip 
	 * @param canvas 
	 * 
	 */
	public ImageImportOptions(IVdpCanvas canvas, IVdpChip iVdpChip) {
		this.canvas = canvas;
		this.vdp = iVdpChip;
		paletteOptionProperty = new FieldProperty(this, "paletteOption", "Palette Selection");
		ditheringProperty = new FieldProperty(this, "ditherType", "Dithering");
		ditherMonoProperty = new FieldProperty(this, "ditherMono", "Dither Monochrome");
	}
	/**
	 * @return
	 */
	public void addToPropertySource(PropertySource ps) {
		ps.addProperty(paletteOptionProperty);
		ps.addProperty(ditheringProperty);
		ps.addProperty(ditherMonoProperty);
	}
	
	public boolean isAsGreyScale() {
		return asGreyScale;
	}
	public void setAsGreyScale(boolean asGreyScale) {
		this.asGreyScale = asGreyScale;
	}
	public Palette getPalette() {
		return paletteOption;
	}
	public void setPalette(Palette option) {
		this.paletteOption = option;
	}
	public Dither getDitherType() {
		return ditherType;
	}
	public void setDitherType(Dither dither) {
		this.ditherType = dither;
	}
	
	public boolean isDitherMono() {
		return ditherMono;
	}
	public void setDitherMono(boolean ditherMono) {
		this.ditherMono = ditherMono;
	}
	
	/**
	 * Call to reset options to the presumed best ones for the
	 * current video mode.
	 */
	public void resetOptions() {
		VdpFormat format = canvas.getFormat();
		
		if (vdp.getRegisterCount() > 10) {
			// hack: graphics mode 2 allows setting the palette too, 
			// but for comparison shopping, pretend we can't.
			if (format == VdpFormat.COLOR16_8x1) {
				canSetPalette = false;
			} else {
				canSetPalette = format != VdpFormat.COLOR256_1x1;
			}
		} else {
			canSetPalette = false;
		}
		
		
		////
		
		setPalette(canSetPalette ? Palette.OPTIMIZED : Palette.STANDARD);
		
		/////
		isMonoMode = vdp instanceof IVdpTMS9918A && ((IVdpTMS9918A) vdp).isBitmapMonoMode();
		
		setDitherMono(isMonoMode);
		setDitherType(format == VdpFormat.COLOR16_8x1 ? Dither.ORDERED : Dither.FS);
		
		octree = null;
	}
	
	/**
	 * @return the isMonoMode
	 */
	public boolean isMonoMode() {
		return isMonoMode;
	}
	
	/**
	 * @return the canSetPalette
	 */
	public boolean canSetPalette() {
		return canSetPalette;
	}
	
	/**
	 * @return the octree
	 */
	public ColorOctree getOctree() {
		if (octree == null)
			octree = new ColorOctree(3, true, false);
		return octree;
	}
	/**
	 * @param octree the octree to set
	 */
	public void setOctree(ColorOctree octree) {
		this.octree = octree;
	}
}
