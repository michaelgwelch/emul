/**
 * 
 */
package v9t9.tools.asm;

import static v9t9.engine.cpu.InstructionTable.Iab;
import static v9t9.engine.cpu.InstructionTable.Icb;
import static v9t9.engine.cpu.InstructionTable.Imovb;
import static v9t9.engine.cpu.InstructionTable.Isb;
import static v9t9.engine.cpu.InstructionTable.Isocb;
import static v9t9.engine.cpu.InstructionTable.Iszcb;
import v9t9.engine.cpu.IInstruction;
import v9t9.engine.cpu.InstructionTable;
import v9t9.tools.asm.directive.LabelDirective;
import v9t9.tools.asm.operand.hl.AssemblerOperand;
import v9t9.tools.asm.operand.ll.LLEmptyOperand;
import v9t9.tools.asm.operand.ll.LLOperand;

/**
 * @author Ed
 *
 */
public abstract class AssemblerInstruction extends BaseAssemblerInstruction {

	private int inst;
	private AssemblerOperand op1;
	private AssemblerOperand op2;
	
	
	public IInstruction[] resolve(Assembler assembler, IInstruction previous, boolean finalPass)
	throws ResolveException {
		int pc = assembler.getPc();
		// instructions and associated labels are bumped when following uneven data
		if ((pc & 1) != 0 && getInst() != InstructionTable.Ibyte) {
			pc = (pc + 1) & 0xfffe;
			assembler.setPc(pc);
		
			if (previous instanceof LabelDirective) {
				((LabelDirective) previous).setPc(assembler.getPc());
				
			}
		}
		
		setPc(pc);
		LLOperand lop1 = getOp1() != null ? getOp1().resolve(assembler, this) : null;
		LLOperand lop2 = getOp2() != null ? getOp2().resolve(assembler, this) : null;
		
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
		builder.append("AsmInst ");
		builder.append(InstructionTable.getInstName(inst));
		if (op1 != null && !(op1 instanceof LLEmptyOperand)) {
			builder.append(' ');
			builder.append(op1);
			if (op2 != null && !(op2 instanceof LLEmptyOperand)) {
				builder.append(',');
				builder.append(op2);
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

	public boolean isJumpInst() {
		return getInst() >= InstructionTable.Ijmp && getInst() <= InstructionTable.Ijop;
	}

	public boolean isByteOp() {
		return inst == Isocb || inst == Icb || inst == Iab 
		|| inst == Isb || inst == Iszcb || inst == Imovb;
	}

}
