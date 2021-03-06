/*
  HostVariable.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.forthcomp.words;

import v9t9.tools.forthcomp.HostContext;

/**
 * @author ejs
 *
 */
public class HostVariable extends BaseStdWord {

	private int val;
	/**
	 * 
	 */
	public HostVariable(int val) {
		this.val = val;
	}
	/* (non-Javadoc)
	 * @see v9t9.forthcomp.IWord#getValue()
	 */
	public int getValue() {
		return val;
	}

	/* (non-Javadoc)
	 * @see v9t9.forthcomp.IWord#execute(v9t9.forthcomp.IContext)
	 */
	public void execute(HostContext hostContext, TargetContext targetContext) {
		hostContext.pushData(val);
	}
	/**
	 * @param i
	 */
	public void setValue(int i) {
		this.val = i;
		
	}
	/* (non-Javadoc)
	 * @see v9t9.forthcomp.IWord#isImmediate()
	 */
	public boolean isImmediate() {
		return false;
	}
}
