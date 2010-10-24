package org.ejs.v9t9.forthcomp.words;

import org.ejs.v9t9.forthcomp.AbortException;
import org.ejs.v9t9.forthcomp.HostContext;
import org.ejs.v9t9.forthcomp.IWord;
import org.ejs.v9t9.forthcomp.TargetContext;

/**
 * @author ejs
 *
 */
public class Here implements IWord {
	public void execute(HostContext hostContext,
			TargetContext targetContext) throws AbortException {
		hostContext.pushData(targetContext.getDP());
	}

	public boolean isImmediate() {
		return false;
	}
}