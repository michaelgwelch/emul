/**
 * 
 */
package v9t9.tools.asm.directive;

import v9t9.engine.cpu.Instruction;
import v9t9.tools.asm.BaseAssemblerInstruction;
import v9t9.tools.asm.ResolveException;

/**
 * @author Ed
 *
 */
public abstract class Directive extends BaseAssemblerInstruction {
	protected static Instruction[] NO_INSTRUCTIONS = new Instruction[0];
	public Directive() {
	}
	
	@Override
	public byte[] getBytes() throws ResolveException {
		return NO_BYTES;
	}
	
	public boolean isByteOp() {
		return false;
	}
}
