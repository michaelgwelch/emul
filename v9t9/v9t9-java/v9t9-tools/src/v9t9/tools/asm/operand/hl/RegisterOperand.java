/*
  RegisterOperand.java

  (c) 2008-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.asm.operand.hl;

import v9t9.common.asm.IInstruction;
import v9t9.common.asm.ResolveException;
import v9t9.tools.asm.IAssembler;
import v9t9.tools.asm.operand.ll.LLImmedOperand;
import v9t9.tools.asm.operand.ll.LLOperand;
import v9t9.tools.asm.operand.ll.LLRegisterOperand;


/**
 * @author ejs
 * 
 */
public class RegisterOperand extends BaseOperand implements IRegisterOperand {
	private final AssemblerOperand reg;

	public RegisterOperand(AssemblerOperand reg) {
		this.reg = reg;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((reg == null) ? 0 : reg.hashCode());
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegisterOperand other = (RegisterOperand) obj;
		if (reg == null) {
			if (other.reg != null)
				return false;
		} else if (!reg.equals(other.reg))
			return false;
		return true;
	}


	@Override
	public String toString() {
		if (getReg() instanceof NumberOperand)
			return "R" + ((NumberOperand)getReg()).getValue();
		else if (getReg() instanceof IRegisterOperand)
			return "[" + getReg().toString() + "]";
		else
			return "R(" + getReg().toString() + ")";
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#isMemory()
	 */
	@Override
	public boolean isMemory() {
		return false;
	}
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#isRegister()
	 */
	@Override
	public boolean isRegister() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#isConst()
	 */
	@Override
	public boolean isConst() {
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see v9t9.engine.cpu.AssemblerOperand#resolve(v9t9.tools.asm.Assembler,
	 * v9t9.engine.cpu.Instruction)
	 */
	public LLOperand resolve(IAssembler assembler, IInstruction inst) throws ResolveException {
		LLOperand op = reg.resolve(assembler, inst);
		if (op instanceof LLRegisterOperand) {
			return op;
		}
		if (op instanceof LLImmedOperand) {
			return new LLRegisterOperand(op.getImmediate());
		}
		throw new ResolveException(op);
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.IRegisterOperand#getReg()
	 */
	public AssemblerOperand getReg() {
		return reg;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.IRegisterOperand#isReg(int)
	 */
	public boolean isReg(int reg) {
		if (getReg() instanceof NumberOperand) {
			return ((NumberOperand) getReg()).getValue() == reg;
		} else if (getReg() instanceof IRegisterOperand) {
			return ((IRegisterOperand) getReg()).isReg(reg);
		} else {
			return false;
		}
	}


	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.BaseOperand#replaceOperand(v9t9.tools.asm.operand.hl.AssemblerOperand, v9t9.tools.asm.operand.hl.AssemblerOperand)
	 */
	@Override
	public AssemblerOperand replaceOperand(AssemblerOperand src,
			AssemblerOperand dst) {
		if (src.equals(this))
			return dst;
		AssemblerOperand newReg = reg.replaceOperand(src, dst);
		if (newReg != reg) {
			return new RegisterOperand(newReg);
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#getChildren()
	 */
	@Override
	public AssemblerOperand[] getChildren() {
		return new AssemblerOperand[] { reg };
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#addOffset(int)
	 */
	@Override
	public AssemblerOperand addOffset(int i) {
		return null;
	}
}
