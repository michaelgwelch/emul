/*
  ImageLabel.java

  (c) 2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.gui.client.swt.imageimport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;


/**
 * Widget that presents an image, scaled in an aspect-ratio-preserving way
 * into the allocated client area.
 * 
 * This also supports an overlay clip rectangle.
 * @author ejs
 *
 */
public class ImageLabel extends Composite implements PaintListener {

	private Rectangle clip;
	private Image image;

	/**
	 * @param parent
	 * @param style
	 */
	public ImageLabel(Composite parent, int style) {
		super(parent, adjustStyle(style));
		
		addPaintListener(this);
	}

	/**
	 * @param style
	 * @return
	 */
	protected static int adjustStyle(int style) {
		if (SWT.getPlatform().equals("win32"))
			return style | SWT.DOUBLE_BUFFERED;
		return style;
	}
	
	public void setClip(Rectangle clip) {
		this.clip = clip;
		redraw();
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
		redraw();
	}

	@Override
	public void paintControl(PaintEvent e) {
		if (getImage() != null) {
			Rectangle rbounds = getClientArea();
			
			Rectangle imgbounds = getImage().getBounds();
			
			Point imgScaledSize = ImageUtils.scaleSizeToSize(
					new Point(imgbounds.width, imgbounds.height),
					new Point(rbounds.width, rbounds.height));

			e.gc.drawImage(image, 0, 0, imgbounds.width, imgbounds.height, 
					0, 0, imgScaledSize.x, imgScaledSize.y);
		}
	}

	public Rectangle getClip() {
		return clip;
	}
	
	

}
