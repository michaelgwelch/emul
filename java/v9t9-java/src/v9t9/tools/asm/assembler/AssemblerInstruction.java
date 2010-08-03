/**
 * 
 */
package v9t9.tools.asm.assembler;

import v9t9.engine.cpu.ICPUInstruction;
import v9t9.engine.cpu.IInstruction;
import v9t9.engine.cpu.Inst9900;
import v9t9.engine.cpu.InstTable9900;
import v9t9.engine.cpu.InstTableCommon;
import v9t9.tools.asm.assembler.directive.LabelDirective;
import v9t9.tools.asm.assembler.operand.hl.AssemblerOperand;
import v9t9.tools.asm.assembler.operand.ll.LLEmptyOperand;
import v9t9.tools.asm.assembler.operand.ll.LLOperand;

/**
 * @author Ed
 *
 */
public abstract class AssemblerInstruction extends BaseAssemblerInstruction implements ICPUInstruction {

	private static final AssemblerOperand[] NO_OPS = new AssemblerOperand[0];
	private int inst;
	private AssemblerOperand op1;
	private AssemblerOperand op2;
	private AssemblerOperand op3;
	
	
	public IInstruction[] resolve(Assembler assembler, IInstruction previous, boolean finalPass)
	throws ResolveException {
		int pc = assembler.getPc();
		// instructions and associated labels are bumped when following uneven data
		if ((pc & 1) != 0 && getInst() != InstTableCommon.Ibyte) {
			pc = (pc + 1) & 0xfffe;
			assembler.setPc(pc);
		
			if (previous instanceof LabelDirective) {
				((LabelDirective) previous).setPc(assembler.getPc());
				
			}
		}
		
		setPc(pc);
		LLOperand lop1 = getOp1() != null ? getOp1().resolve(assembler, this) : null;
		LLOperand lop2 = getOp2() != null ? getOp2().resolve(assembler, this) : null;
		LLOperand lop3 = getOp3() != null ? getOp3().resolve(assembler, this) : null;
		
		if (lop3 != null)
			throw new ResolveException(lop3, "cannot resolve third operand");
		
		LLInstruction target = new LLInstruction();
		target.setPc(pc);
		target.setInst(getInst());
		target.setOp1(lop1);
		target.setOp2(lop2);
		//target.completeInstruction(pc);
	
		assembler.setPc((short) (target.getPc() + target.getSize()));
		return new LLInstruction[] { target };
	}
	

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(InstTable9900.getInstName(inst));
		if (op1 != null && !(op1 instanceof LLEmptyOperand)) {
			builder.append(' ');
			builder.append(op1);
			if (op2 != null && !(op2 instanceof LLEmptyOperand)) {
				builder.append(',');
				builder.append(op2);
				if(op3 != null && !(op3 instanceof LLEmptyOperand)) {
					builder.append(',');
					builder.append(op3);
				}
			}
		}
		return builder.toString();
	}
	
	public int getInst() {
		return inst;
	}

	public void setInst(int inst) {
		this.inst = inst;
	}

	public AssemblerOperand getOp1() {
		return op1;
	}

	public void setOp1(AssemblerOperand op1) {
		this.op1 = op1;
	}

	public AssemblerOperand getOp2() {
		return op2;
	}

	public void setOp2(AssemblerOperand op2) {
		this.op2 = op2;
	}

	public AssemblerOperand getOp3() {
		return op3;
	}
	
	public void setOp3(AssemblerOperand op3) {
		this.op3 = op3;
	}
	
	public boolean isJumpInst() {
		return getInst() >= Inst9900.Ijmp && getInst() <= Inst9900.Ijop;
	}

	public boolean isByteOp() {
		return inst == Inst9900.Isocb || inst == Inst9900.Icb || inst == Inst9900.Iab 
		|| inst == Inst9900.Isb || inst == Inst9900.Iszcb || inst == Inst9900.Imovb;
	}

	/** Get an operand by traditional number (1,2,3) */
	public AssemblerOperand getOp(int i) {
		if (i == 1)
			return op1;
		else if (i == 2)
			return op2;
		else if (i == 3)
			return op3;
		throw new IllegalArgumentException();
	}
	public void setOp(int i, AssemblerOperand op) {
		if (i == 1)
			op1 = op;
		else if (i == 2)
			op2 = op;
		else if (i == 3)
			op3 = op;
		else
			throw new IllegalArgumentException();
	}
	public AssemblerOperand[] getOps() {
		if (op3 != null && !(op3 instanceof LLEmptyOperand))
			return new AssemblerOperand[] { op1, op2, op3 };
		if (op2 != null && !(op2 instanceof LLEmptyOperand))
			return new AssemblerOperand[] { op1, op2  };
		if (op1 != null && !(op1 instanceof LLEmptyOperand))
			return new AssemblerOperand[] { op1 };
		return NO_OPS;
	}

}
