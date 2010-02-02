/**
 * 
 */
package v9t9.tools.asm.operand.hl;

import v9t9.engine.cpu.IInstruction;
import v9t9.tools.asm.Assembler;
import v9t9.tools.asm.ResolveException;
import v9t9.tools.asm.operand.ll.LLForwardOperand;
import v9t9.tools.asm.operand.ll.LLImmedOperand;
import v9t9.tools.asm.operand.ll.LLJumpOperand;
import v9t9.tools.asm.operand.ll.LLOperand;

/**
 * An operand to be used as a jump target.  Its resolved value is
 * a jump operand indicating the offset from the PC.
 * @author ejs
 *
 */
public class JumpOperand implements AssemblerOperand {

	private final AssemblerOperand op;

	public JumpOperand(AssemblerOperand op) {
		this.op = op;
	}

	@Override
	public String toString() {
		if (op instanceof NumberOperand)
			return "$+" + op.toString();
		else
			return op.toString();
	}
	
	public LLOperand resolve(Assembler assembler, IInstruction inst)
			throws ResolveException {
		int pc = assembler.getPc();
		LLOperand opRes = op.resolve(assembler, inst);
		if (opRes instanceof LLImmedOperand) {
			// resolved
			return new LLJumpOperand(this, opRes.getImmediate() - pc);
		}
		else if (opRes instanceof LLForwardOperand) {
			return new LLForwardOperand(this, 0);
		}
		else
			throw new ResolveException(op, "Expected a number or forward");
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((op == null) ? 0 : op.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JumpOperand other = (JumpOperand) obj;
		if (op == null) {
			if (other.op != null) {
				return false;
			}
		} else if (!op.equals(other.op)) {
			return false;
		}
		return true;
	}
	
}
