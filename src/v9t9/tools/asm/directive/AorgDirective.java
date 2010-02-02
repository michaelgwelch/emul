/**
 * 
 */
package v9t9.tools.asm.directive;

import java.util.List;

import v9t9.engine.cpu.IInstruction;
import v9t9.tools.asm.Assembler;
import v9t9.tools.asm.ResolveException;
import v9t9.tools.asm.operand.hl.AssemblerOperand;
import v9t9.tools.asm.operand.ll.LLImmedOperand;
import v9t9.tools.asm.operand.ll.LLOperand;

/**
 * @author Ed
 *
 */
public class AorgDirective extends Directive {

	private AssemblerOperand op;

	public AorgDirective(List<AssemblerOperand> ops) {
		this.op = ops.get(0);
	}
	
	@Override
	public String toString() {
		return "AORG " + op;
	}

	public IInstruction[] resolve(Assembler assembler, IInstruction previous, boolean finalPass) throws ResolveException {
		LLOperand lop = op.resolve(assembler, this); 
		if (!(lop instanceof LLImmedOperand))
			throw new ResolveException(op, "Expected number");
		op = lop;
		assembler.setPc(lop.getImmediate());
		setPc(lop.getImmediate());
		return new IInstruction[] { this };
	}
}
