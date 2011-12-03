/**
 * 
 */
package v9t9.tools.asm.assembler.operand.ll;

import org.ejs.coffee.core.utils.HexUtils;

import v9t9.engine.asm.ResolveException;
import v9t9.engine.cpu.MachineOperand;
import v9t9.engine.cpu.Operand;
import v9t9.tools.asm.assembler.operand.hl.AssemblerOperand;

/**
 * @author Ed
 *
 */
public class LLPCRelativeOperand extends LLNonImmediateOperand implements Operand {

	int offset;
	
	public LLPCRelativeOperand(AssemblerOperand original, int offset) {
		super(original);
		setOffset(offset);
	}
	
	@Override
	public String toString() {
		return "$+>" + HexUtils.toHex4(offset);
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
		LLPCRelativeOperand other = (LLPCRelativeOperand) obj;
		if (offset != other.offset)
			return false;
		return true;
	}

	@Override
	public boolean isMemory() {
		return false;
	}
	@Override
	public boolean isRegister() {
		return false;
	}
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.hl.AssemblerOperand#isConst()
	 */
	@Override
	public boolean isConst() {
		return true;
	}


	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	@Override
	public MachineOperand createMachineOperand(IAsmMachineOperandFactory opFactory) throws ResolveException {
		return opFactory.createPCRelativeOperand(this);
	}
}