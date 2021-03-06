/*
  TextModeRedrawHandler.java

  (c) 2008-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.video.tms9918a;

import v9t9.common.video.RedrawBlock;
import v9t9.video.BaseRedrawHandler;
import v9t9.video.IVdpModeRedrawHandler;
import v9t9.video.VdpRedrawInfo;
import v9t9.video.common.VdpModeInfo;

/**
 * @author ejs
 *
 */
public class TextModeRedrawHandler extends BaseRedrawHandler implements
		IVdpModeRedrawHandler {

	public TextModeRedrawHandler(VdpRedrawInfo info, VdpModeInfo modeInfo) {
		super(info, modeInfo);

		info.touch.patt = modify_patt_default;
		info.touch.sprite = info.touch.sprpat = null;
		info.touch.screen = modify_screen_default;
		info.touch.color = null;
	}

	public void prepareUpdate() {
		propagatePatternTouches();
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.InternalVdp.VdpModeRedrawHandler#updateCanvas(v9t9.emulator.clients.builtin.VdpCanvas, v9t9.emulator.clients.builtin.InternalVdp.RedrawBlock[])
	 */
	public int updateCanvas(RedrawBlock[] blocks) {
		/*  Redraw changed chars  */

		int count = 0;
		int screenBase = modeInfo.screen.base;
		int pattBase = modeInfo.patt.base;
		
		byte fg, bg;
		
		bg = (byte) (info.vdpregs[7] & 0xf);
		fg = (byte) ((info.vdpregs[7] >> 4) & 0xf);

		for (int i = info.changes.screen.nextSetBit(0); 
			i >= 0 && i < modeInfo.screen.size; 
			i = info.changes.screen.nextSetBit(i+1)) 
		{

			int currchar = info.vdp.readAbsoluteVdpMemory(screenBase + i) & 0xff;	/* char # to update */

			RedrawBlock block = blocks[count++];
			
			block.r = (i / 40) << 3;	
			block.c = (i % 40) * 6 + (256 - 240) / 2;

			int pattOffs = pattBase + (currchar << 3);
			info.canvas.draw8x6TwoColorBlock(block.r, block.c, 
					info.vdp.getByteReadMemoryAccess(pattOffs), fg, bg);
		}

		return count;
	}

}
