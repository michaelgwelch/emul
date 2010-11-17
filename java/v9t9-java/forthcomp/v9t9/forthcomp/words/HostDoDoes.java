/**
 * 
 */
package v9t9.forthcomp.words;

import v9t9.forthcomp.AbortException;
import v9t9.forthcomp.HostContext;
import v9t9.forthcomp.ITargetWord;

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
		targetContext.getDictionary().put(lastWord.getName(), new TargetDoesWord(lastWord, redirectDp));
		
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
