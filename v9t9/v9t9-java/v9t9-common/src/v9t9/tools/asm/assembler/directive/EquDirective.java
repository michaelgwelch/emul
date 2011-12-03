/**
 * 
 */
package v9t9.tools.asm.assembler.directive;

import java.util.List;

import v9t9.engine.cpu.IInstruction;
import v9t9.tools.asm.assembler.IAssembler;
import v9t9.tools.asm.assembler.ResolveException;
import v9t9.tools.asm.assembler.Symbol;
import v9t9.tools.asm.assembler.operand.hl.AssemblerOperand;
import v9t9.tools.asm.assembler.operand.ll.LLImmedOperand;
import v9t9.tools.asm.assembler.operand.ll.LLOperand;

/**
 * @author Ed
 *
 */
public class EquDirective extends Directive {

	private AssemblerOperand op;

	public EquDirective(List<AssemblerOperand> ops) {
		this.op = ops.get(0);
	}
	
	@Override
	public String toString() {
		return "EQU " + op;
	}

	public IInstruction[] resolve(IAssembler assembler, IInstruction previous, boolean finalPass) throws ResolveException {
		// establish initial PC, for "equ $"
		setPc(assembler.getPc());
		
		LLOperand lop = op.resolve(assembler, this); 
		if (!(lop instanceof LLImmedOperand))
			throw new ResolveException(op, "Expected number");

		// don't change this directive's Pc, in case we depend on $ and get resolved multiple times
		
		if (previous != null && previous instanceof LabelDirective) {
			LabelDirective label = (LabelDirective) previous;
			Symbol symbol = label.getSymbol();
			label.setPc(lop.getImmediate());
			symbol.setDefined(true);
		}
		return new IInstruction[] { this };
	}
	


}