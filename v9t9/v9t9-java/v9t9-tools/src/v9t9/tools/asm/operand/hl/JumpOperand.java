/*
  JumpOperand.java

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
import v9t9.tools.asm.operand.ll.LLForwardOperand;
import v9t9.tools.asm.operand.ll.LLImmedOperand;
import v9t9.tools.asm.operand.ll.LLOperand;
import v9t9.tools.asm.operand.ll.LLPCRelativeOperand;

/**
 * An operand to be used as a jump target.  Its resolved value is
 * a jump operand indicating the offset from the PC.
 * @author ejs
 *
 */
public class JumpOperand extends BaseOperand {

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
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#isConst()
	 */
	@Override
	public boolean isConst() {
		return op.isConst();
	}
	
	public LLOperand resolve(IAssembler assembler, IInstruction inst)
			throws ResolveException {
		int pc = assembler.getPc();
		LLOperand opRes = op.resolve(assembler, inst);
		if (opRes instanceof LLImmedOperand) {
			// resolved
			return new LLPCRelativeOperand(this, opRes.getImmediate() - pc);
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
	

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.BaseOperand#replaceOperand(v9t9.tools.asm.operand.hl.AssemblerOperand, v9t9.tools.asm.operand.hl.AssemblerOperand)
	 */
	@Override
	public AssemblerOperand replaceOperand(AssemblerOperand src,
			AssemblerOperand dst) {
		if (src.equals(this))
			return dst;
		AssemblerOperand newAddr = op.replaceOperand(src, dst);
		if (newAddr != op) {
			return new JumpOperand(newAddr);
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#getChildren()
	 */
	@Override
	public AssemblerOperand[] getChildren() {
		return new AssemblerOperand[] { op };
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#addOffset(int)
	 */
	@Override
	public AssemblerOperand addOffset(int i) {
		return new JumpOperand(op.addOffset(i));
	}
}
