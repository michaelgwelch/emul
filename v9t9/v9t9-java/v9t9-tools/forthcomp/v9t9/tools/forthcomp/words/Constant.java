/**
 * 
 */
package v9t9.tools.forthcomp.words;

import v9t9.base.utils.HexUtils;
import v9t9.tools.forthcomp.AbortException;
import v9t9.tools.forthcomp.HostContext;

/**
 * @author ejs
 *
 */
public class Constant extends BaseStdWord {
	public Constant() {
	}

	/* (non-Javadoc)
	 * @see v9t9.forthcomp.IWord#execute(v9t9.forthcomp.IContext)
	 */
	public void execute(HostContext hostContext, TargetContext targetContext) throws AbortException {
		int val = hostContext.popData();
		
		String name = hostContext.readToken();

		System.out.println("CONSTANT " + name +" = " + HexUtils.toHex4(val));
		targetContext.defineConstant(name, val, 1);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.forthcomp.IWord#isImmediate()
	 */
	public boolean isImmediate() {
		return false;
	}

	@Override
	public boolean isCompilerWord() {
		return true;
	}
}
