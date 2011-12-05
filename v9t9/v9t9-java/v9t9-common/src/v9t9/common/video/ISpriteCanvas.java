/**
 * 
 */
package v9t9.common.video;


/**
 * @author ejs
 *
 */
public interface ISpriteCanvas {
	/**
	 * @return
	 */
	VdpSprite[] getSprites();

	/**
	 * @param numchars
	 */
	void setNumSpriteChars(int numchars);

	/**
	 * @param isMag
	 */
	void setMagnified(boolean isMag);
	int updateSpriteCoverage(ICanvas canvas, byte[] screen,
			boolean forceRedraw);
	/**
	 * @param canvas
	 */
	void drawSprites(IVdpCanvas canvas);
}
