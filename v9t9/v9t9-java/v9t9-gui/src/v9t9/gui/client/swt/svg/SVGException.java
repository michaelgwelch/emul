/*
  SVGException.java

  (c) 2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.gui.client.swt.svg;

/**
 * @author Ed
 *
 */
public class SVGException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6278322295563712598L;

	public SVGException(String message, Throwable t) {
		super(message, t);
	}
}
