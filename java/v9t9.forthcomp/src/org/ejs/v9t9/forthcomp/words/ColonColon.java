/**
 * 
 */
package org.ejs.v9t9.forthcomp.words;

import java.util.Map;

import org.ejs.v9t9.forthcomp.AbortException;
import org.ejs.v9t9.forthcomp.HostContext;
import org.ejs.v9t9.forthcomp.ITargetWord;
import org.ejs.v9t9.forthcomp.IWord;
import org.ejs.v9t9.forthcomp.TargetContext;

/**
 *  :: word ( local1 local2 ... -- ignored ... ) 
 * @author ejs
 *
 */
public class ColonColon implements IWord {

	/* (non-Javadoc)
	 * @see org.ejs.v9t9.forthcomp.IWord#execute(org.ejs.v9t9.forthcomp.HostContext, org.ejs.v9t9.forthcomp.TargetContext)
	 */
	public void execute(HostContext hostContext, TargetContext targetContext)
			throws AbortException {
		
		
		targetContext.ensureLocalSupport(hostContext);
		
		new Colon().execute(hostContext, targetContext);

		// set up
		
		targetContext.compileSetupLocals();
		
		// now parse locals
		
		String token = hostContext.readToken();
		if (!"(".equals(token))
			throw hostContext.abort("Expected (");
		
		ITargetWord theWord = (ITargetWord) targetContext.getLatest();

		theWord.getEntry().allocateLocals();
		
		Map<String, LocalVariable> locals = theWord.getEntry().getLocals();
		
		while (true) {
			String name = hostContext.readToken();
			if ("--".equals(name)) 
				break;

			if (locals.containsKey(name))
				throw hostContext.abort(name +" already used");
					
			LocalVariable local = new LocalVariable(locals.size());
			locals.put(name, local);
			
			targetContext.compileInitLocal(local.getIndex());
		}
		
		while (true) {
			String tok = hostContext.readToken();
			if (tok == null)
				throw hostContext.abort("expected )");
			if (")".equals(tok))
				break;
		}
		
	}

	/* (non-Javadoc)
	 * @see org.ejs.v9t9.forthcomp.IWord#isImmediate()
	 */
	public boolean isImmediate() {
		return false;
	}
}
