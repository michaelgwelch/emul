/**
 * 
 */
package v9t9.tools.asm.assembler;

import v9t9.engine.cpu.InstMFP201;
import v9t9.engine.cpu.InstPatternMFP201;
import v9t9.engine.cpu.InstTableMFP201;
import v9t9.engine.cpu.InstructionMFP201;
import v9t9.engine.cpu.MachineOperandMFP201;
import v9t9.engine.cpu.RawInstruction;
import v9t9.tools.asm.assembler.operand.hl.AssemblerOperand;
import v9t9.tools.asm.assembler.operand.hl.IRegisterOperand;
import v9t9.tools.asm.assembler.operand.hl.NumberOperand;
import v9t9.tools.asm.assembler.operand.hl.PcRelativeOperand;
import v9t9.tools.asm.assembler.operand.ll.LLImmedOperand;
import v9t9.tools.asm.assembler.operand.ll.LLOperand;
import v9t9.tools.asm.assembler.operand.ll.LLRegDecOperand;
import v9t9.tools.asm.assembler.operand.ll.LLRegIncOperand;
import v9t9.tools.asm.assembler.operand.ll.LLRegIndOperand;
import v9t9.tools.asm.assembler.operand.ll.LLRegisterOperand;

/**
 * @author Ed
 *
 */
public class InstructionFactoryMFP201 implements IInstructionFactory {

	public static final IInstructionFactory INSTANCE = new InstructionFactoryMFP201();
	MachineOperandFactoryMFP201 opFactory = new MachineOperandFactoryMFP201();
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.IInstructionFactory#createRawInstruction(v9t9.tools.asm.assembler.LLInstruction)
	 */
	@Override
	public RawInstruction createRawInstruction(LLInstruction ins)
			throws ResolveException {
		InstructionMFP201 rawInst = new InstructionMFP201();
		rawInst.pc = ins.pc;
		int inst = ins.getInst();
		rawInst.setInst(inst);
		rawInst.setName(InstTableMFP201.getInstName(inst));
		
		LLOperand op1 = ins.getOp1();
		LLOperand op2 = ins.getOp2();
		LLOperand op3 = ins.getOp3();
		
		// convert 2-op to 3-op
		if (InstTableMFP201.canBeThreeOpInst(inst)) {
			if (op3 == null) {
				// if a writing inst with two normal registers, change A,B to B,A,B
				if (op1 instanceof LLRegisterOperand && op2 instanceof LLRegisterOperand) {
					if (! InstTableMFP201.isNonWritingInst(inst)) {
						op3 = op2;
						op2 = op1;
						op1 = op3;
					} else {
						// third operand is SR -- go ahead and set it here
						rawInst.setOp3(
								MachineOperandMFP201.createNonWritingSROperand());
					}
				}
				
				// If the inst has memory in the first operand,
				// we can convert A,B to A,B,B.
				else if (op1 != null && op1.isMemory() && op2 instanceof LLRegisterOperand) {
					if (!InstTableMFP201.isNonWritingInst(inst)) {
						op3 = op2;
					} else {
						// third operand is SR -- go ahead and set it here
						rawInst.setOp3(MachineOperandMFP201
								.createNonWritingSROperand());
					}
				}
		        
				// If the inst has memory in the second operand and is commutative
				// or reversable, we can convert reg,mem to mem,reg.
				else if (op2 != null && op2.isMemory() && op1 instanceof LLRegisterOperand) {
					
					if (!InstTableMFP201.isNonWritingInst(inst)) {
						// interpret, e.g. SUB R0,*SP  as SUB *SP, R0, *SP
						LLOperand t = op1;
						op1 = op2;
						op2 = t;
						
						op3 = op1;
						
						// don't double inc/decrement
						if (op1 instanceof LLRegIncOperand)
							op1 = new LLRegIndOperand(op1.getOriginal(), ((LLRegIncOperand) op1).getRegister());
						else if (op1 instanceof LLRegDecOperand)
							op1 = new LLRegIndOperand(op1.getOriginal(), ((LLRegDecOperand) op1).getRegister());
					} else {
						// reverse the comparison
						int revinst = InstTableMFP201.getReversedInst(inst);
						if (revinst > 0) {
							rawInst.setInst(revinst);
							LLOperand t = op1;
							op1 = op2;
							op2 = t;
						} else {
							// just throw error later
						}
						
						// third operand is SR -- go ahead and set it here
						rawInst.setOp3(MachineOperandMFP201
								.createNonWritingSROperand());
					}
				}
			}
			
			// full 3-op instructions
			else {
				// SUB Rx, const, Ry --> ADD -const, Rx, Ry
				if ((inst == InstMFP201.Isub || inst == InstMFP201.Isubb)
						&& op1 instanceof LLRegisterOperand 
						&& op2 != null && op2 instanceof LLImmedOperand) {
					rawInst.setInst(inst - InstMFP201.Isub + InstMFP201.Iadd);
					LLOperand tmp = op1;
					op1 = op2;
					op2 = tmp;
					op1 = new LLImmedOperand(-op1.getImmediate());
				}
				
				// If the inst has memory in the second operand and is commutative
				// or reversable, we can convert reg,mem to mem,reg.
				else if (op2 != null && op2.isMemory() && op1 instanceof LLRegisterOperand) {
					int revinst = InstTableMFP201.getReversedInst(inst);
					if (revinst > 0) {
						rawInst.setInst(revinst);
						LLOperand t = op1;
						op1 = op2;
						op2 = t;
					} else {
						// just throw error later
					}
				}
			}
			// see if src1 is a constant and replace with implicit constant
			if (op1 != null && op1.isConst()) {
				int immed = op1.getImmediate();
				int[] consts;
				if (InstTableMFP201.isLogicalOpInst(inst)) {
					consts = InstTableMFP201.LOGICAL_INST_CONSTANTS[inst & 1];
				} else {
					consts = InstTableMFP201.ARITHMETIC_INST_CONSTANTS;
				}
				for (int r = 0; r < 3; r++) {
					if (consts[r] == immed) {
						op1 = null;
						rawInst.setOp1(MachineOperandMFP201.createImplicitConstantReg(
								r + 13, consts[r]));
						break;
					}
				}
			}
		}
		
		
		if (op1 != null)
			rawInst.setOp1(op1.createMachineOperand(opFactory));
		if (op2 != null)
			rawInst.setOp2(op2.createMachineOperand(opFactory));
		if (op3 != null)
			rawInst.setOp3(op3.createMachineOperand(opFactory));
		
		InstTableMFP201.coerceOperandTypes(rawInst);
		//InstTableMFP201.calculateInstructionSize(rawInst);
		return rawInst;
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.IInstructionFactory#encodeInstruction(v9t9.engine.cpu.RawInstruction)
	 */
	@Override
	public byte[] encodeInstruction(RawInstruction instruction) {
		return InstTableMFP201.encode(instruction);
	}
	
	public boolean supportsOp(int inst, int i, AssemblerOperand op) {
		InstPatternMFP201[] patterns = InstTableMFP201.lookupEncodePatterns(inst);
		if (patterns == null)
			return true;	// will fail eventually

		for (InstPatternMFP201 pattern : patterns) {
			int opType = i == 1 ? pattern.op1 : i == 2 ? pattern.op2 : pattern.op3;
			
			switch (opType) {
			case InstPatternMFP201.CNT:
				if ((op instanceof IRegisterOperand)) {
					if (((IRegisterOperand) op).isReg(0))
						return true;
					break;
				}
				// fall through
			case InstPatternMFP201.IMM:
			case InstPatternMFP201.OFF:
				if (op instanceof NumberOperand || op.isConst())
					return true;
				break;
			case InstPatternMFP201.REG:
				if (op.isRegister())
					return true;
				break;
			case InstPatternMFP201.GEN:
				// can be *PC+
				if (op instanceof NumberOperand || op.isConst())
					return true;
				
				// can be @x(PC)
				if (op instanceof PcRelativeOperand)
					return true;
					
				if (op.isRegister() || op.isMemory()) {
					if (op instanceof IRegisterOperand) {
						AssemblerOperand reg = ((IRegisterOperand) op).getReg();
						if (reg.isRegister() || reg instanceof NumberOperand)
							return true;
					} else {
						return true;
					}
				}
			}
			
			// not supported; try next pattern
		}
		
		// no patterns support it
		return false;
	}

	public boolean isByteInst(int inst) {
		return InstTableMFP201.isByteInst(inst);
	}
	
	@Override
	public boolean isJumpInst(int inst) {
		return InstTableMFP201.isJumpInst(inst);
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.IInstructionFactory#getInstName(int)
	 */
	@Override
	public String getInstName(int inst) {
		return InstTableMFP201.getInstName(inst);
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.IInstructionFactory#getInstSize(v9t9.tools.asm.assembler.LLInstruction)
	 */
	@Override
	public int getInstSize(LLInstruction ins) {
		try {
			byte[] bytes = ins.getBytes(INSTANCE);
			return bytes.length;
		} catch (ResolveException e) {
			return 1;
		}
	}
}
