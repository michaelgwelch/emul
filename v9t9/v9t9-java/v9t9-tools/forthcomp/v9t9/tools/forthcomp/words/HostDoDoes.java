/*
  HostDoDoes.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.forthcomp.words;

import v9t9.tools.forthcomp.AbortException;
import v9t9.tools.forthcomp.HostContext;
import v9t9.tools.forthcomp.ITargetWord;

/**
 * @author ejs
 *
 */
public class HostDoDoes extends BaseStdWord {

	private final int redirectDp;
	private final int targetDP;

	/**
	 * @param redirectDp
	 * @param targetDP 
	 * @param targetOnly TODO
	 * 
	 */
	public HostDoDoes(int redirectDp, int targetDP, boolean targetOnly) {
		this.redirectDp = redirectDp;
		this.targetDP = targetDP;
	}
	

	/* (non-Javadoc)
	 * @see v9t9.forthcomp.IWord#execute(v9t9.forthcomp.HostContext, v9t9.forthcomp.words.TargetContext)
	 */
	public void execute(HostContext hostContext, TargetContext targetContext)
			throws AbortException {
		hostContext.popCall();
		hostContext.compile(new HostBranch(redirectDp));
		
		ITargetWord lastWord = (ITargetWord) targetContext.getLatest();
		targetContext.redefine(lastWord, new TargetDoesWord(lastWord, redirectDp));
		
		lastWord.getEntry().setDoesWord(true);

		targetContext.compileDoes(hostContext, lastWord.getEntry(), targetDP);
		
		//hostContext.setHostPc(redirectDp);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.forthcomp.IWord#isImmediate()
	 */
	public boolean isImmediate() {
		return false;
	}
	
}
