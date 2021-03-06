/*
  NullKeyboardHandler.java

  (c) 2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.keyboard;

import v9t9.common.client.IVideoRenderer;
import v9t9.common.events.IEventNotifier;
import v9t9.common.machine.IMachine;

/**
 * @author ejs
 *
 */
public class NullKeyboardHandler extends BaseKeyboardHandler {

	public NullKeyboardHandler(IKeyboardState keyboardState, IMachine machine) {
		super(keyboardState, machine);
	}

	/* (non-Javadoc)
	 * @see v9t9.common.client.IKeyboardHandler#init(v9t9.common.client.IVideoRenderer)
	 */
	@Override
	public void init(IVideoRenderer renderer) {

	}

	/* (non-Javadoc)
	 * @see v9t9.common.client.IKeyboardHandler#setEventNotifier(v9t9.common.events.IEventNotifier)
	 */
	@Override
	public void setEventNotifier(IEventNotifier notifier) {

	}

}
