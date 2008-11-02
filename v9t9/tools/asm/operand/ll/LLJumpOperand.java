/**
 * 
 */
package v9t9.tools.asm.operand.ll;

import v9t9.engine.cpu.MachineOperand;
import v9t9.engine.cpu.Operand;
import v9t9.tools.asm.ResolveException;
import v9t9.tools.asm.operand.hl.AssemblerOperand;
import v9t9.utils.Utils;

/**
 * @author Ed
 *
 */
public class LLJumpOperand extends LLNonImmediateOperand implements Operand {

	int offset;
	
	public LLJumpOperand(AssemblerOperand original, int offset) {
		super(original);
		setOffset(offset);
	}
	
	@Override
	public String toString() {
		return "$+>" + Utils.toHex4(offset);
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + offset;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LLJumpOperand other = (LLJumpOperand) obj;
		if (offset != other.offset)
			return false;
		return true;
	}


	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	@Override
	public MachineOperand createMachineOperand() throws ResolveException {
		MachineOperand mop = new MachineOperand(MachineOperand.OP_JUMP);
		mop.val = offset;
		return mop;
	}
}
