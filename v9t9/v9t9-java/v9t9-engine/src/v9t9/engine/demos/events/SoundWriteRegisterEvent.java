/*
  SoundWriteRegisterEvent.java

  (c) 2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.demos.events;

import v9t9.common.demos.IDemoEvent;

/**
 * @author ejs
 *
 */
public class SoundWriteRegisterEvent extends WriteRegisterEvent implements
		IDemoEvent {

	public static final String ID = "SoundWriteRegister";

	public SoundWriteRegisterEvent(int reg, int val) {
		super(reg, val);
	}

	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoEvent#getIdentifier()
	 */
	@Override
	public String getIdentifier() {
		return ID;
	}
	
}
