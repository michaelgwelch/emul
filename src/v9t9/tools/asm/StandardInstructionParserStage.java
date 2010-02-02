/**
 * 
 */
package v9t9.tools.asm;

import static v9t9.engine.cpu.InstEncodePattern.CNT;
import static v9t9.engine.cpu.InstEncodePattern.GEN;
import static v9t9.engine.cpu.InstEncodePattern.IMM;
import static v9t9.engine.cpu.InstEncodePattern.NONE;
import static v9t9.engine.cpu.InstEncodePattern.OFF;
import static v9t9.engine.cpu.InstEncodePattern.REG;
import v9t9.engine.cpu.IInstruction;
import v9t9.engine.cpu.InstEncodePattern;
import v9t9.engine.cpu.InstructionTable;
import v9t9.tools.asm.operand.hl.AssemblerOperand;
import v9t9.tools.asm.operand.hl.JumpOperand;
import v9t9.tools.asm.operand.hl.NumberOperand;
import v9t9.tools.asm.operand.hl.RegisterOperand;
import v9t9.tools.asm.operand.hl.SymbolOperand;
import v9t9.tools.asm.operand.ll.LLCountOperand;
import v9t9.tools.asm.operand.ll.LLEmptyOperand;
import v9t9.tools.asm.operand.ll.LLImmedOperand;
import v9t9.tools.asm.operand.ll.LLJumpOperand;
import v9t9.tools.asm.operand.ll.LLOffsetOperand;
import v9t9.tools.asm.operand.ll.LLOperand;
import v9t9.tools.asm.operand.ll.LLRegIndOperand;
import v9t9.tools.asm.operand.ll.LLRegisterOperand;
import v9t9.tools.llinst.ParseException;

/**
 * Parse low-level instructions (no symbols or anything).
 * @author ejs
 *
 */
public class StandardInstructionParserStage implements IInstructionParserStage {

	private OperandParser operandParser;
    
	public StandardInstructionParserStage() {
		this.operandParser = OperandParser.STANDARD;
	}
	public StandardInstructionParserStage(OperandParser operandParser) {
		this.operandParser = operandParser;
	}
	 
    /**
     * Create an instruction from a string.
     */
    public IInstruction[] parse(String descr, String string) throws ParseException {
        //this(pc);
    	AssemblerTokenizer tokenizer = new AssemblerTokenizer(string);
    	
    	int t = tokenizer.nextToken();
    	if (t != AssemblerTokenizer.ID) {
    		return null;
    	}
    	
    	HLInstruction inst = new HLInstruction();
    	String name = tokenizer.currentToken().toUpperCase();
        //inst.name = tokenizer.currentToken().toUpperCase();
        //inst.size = 2;
    	AssemblerOperand op1 = null, op2 = null;
        if (name.equals("RT")) {
        	//inst.name = "B";
        	inst.setInst(InstructionTable.Ib);
        	//inst.size = 2;
            op1 = new LLRegIndOperand(11);
        } else if (name.equals("NOP")) {
        	//inst.name = "JMP";
        	inst.setInst(InstructionTable.Ijmp);
        	//inst.size = 2;
            op1 = new LLJumpOperand(null, 2);
        } else {
        	inst.setInst(InstructionTable.lookupInst(name));
            if (inst.getInst() < 0) {
            	return null;
            }
            
            InstEncodePattern pattern = InstructionTable.lookupEncodePattern(inst.getInst());
            if (pattern == null)
            	throw new IllegalStateException("Missing instruction pattern: " + inst.getInst());
            
            if (pattern.op1 != InstEncodePattern.NONE) {
            	op1 = (AssemblerOperand) operandParser.parse(tokenizer);
            	op1 = coerceType(inst, op1, pattern.op1);
            	if (pattern.op2 != InstEncodePattern.NONE) {
            		t = tokenizer.nextToken();
            		if (t != ',') {
            			throw new ParseException("Missing second operand: " + tokenizer.currentToken());
            		}
            		op2 =  (AssemblerOperand) operandParser.parse(tokenizer);
            		op2 = coerceType(inst, op2, pattern.op2);
            	}
            }
        }
        
        // ensure EOL
        t = tokenizer.nextToken();
        if (t != AssemblerTokenizer.EOF && t != ';') {
        	throw new ParseException("Trailing text on line: " + tokenizer.currentToken());
        }
        
        inst.setOp1(op1 != null ? op1 : new LLEmptyOperand());
        inst.setOp2(op2 != null ? op2 : new LLEmptyOperand());
        
        return new IInstruction[] { inst };
    }

	/** 
	 * Ensure that any ambiguously parsed operands have the expected type, modifying
	 * operands as needed.
	 * @param inst
	 */
	private AssemblerOperand coerceType(AssemblerInstruction inst, AssemblerOperand op, int optype) {
		if (op instanceof LLOperand)
			return coerceLLOperandType(inst, (LLOperand) op, optype);
		if (op instanceof AssemblerOperand)
			return coerceAssemblerOperandType(inst, (AssemblerOperand) op, optype);
		
		return op;
	}
	
	private LLOperand coerceLLOperandType(AssemblerInstruction inst, LLOperand lop, int op) {
		switch (op) {
		case NONE:
			break;
		case IMM:
			if (lop instanceof LLRegisterOperand) {
				lop = new LLImmedOperand(((LLRegisterOperand) lop).getRegister());
			}
			break;
		case CNT:
			if (lop instanceof LLRegisterOperand)
				lop = new LLCountOperand(((LLRegisterOperand) lop).getRegister());
			else if (lop instanceof LLImmedOperand)
				lop = new LLCountOperand(lop.getImmediate());
			break;
		case OFF: {
			int val = 0; 
			if (lop instanceof LLRegisterOperand) {
				val = ((LLRegisterOperand) lop).getRegister();
			} else if (lop instanceof LLImmedOperand) {
				val = lop.getImmediate();
			} else {
				break;
			}
			if (inst.isJumpInst()) {
				// for a parsed inst, we don't know our pc or size,
				// so this is an absolute address
				lop = new LLImmedOperand(lop.getOriginal(), val);
			} else {
				lop = new LLOffsetOperand(val);
			}
			break;
		}
		case REG:
		case GEN:
			if (lop instanceof LLImmedOperand)
				lop = new LLRegisterOperand(lop.getImmediate());
			break;
		}
		return lop;
	}
	
	private AssemblerOperand coerceAssemblerOperandType(AssemblerInstruction inst, AssemblerOperand op, int optype) {
		if (optype == InstEncodePattern.REG
    			|| optype == InstEncodePattern.GEN) {
    		if (op instanceof NumberOperand
    				|| op instanceof SymbolOperand)
    			return new RegisterOperand((AssemblerOperand) op);
    	}
		else if (optype == InstEncodePattern.OFF) {
			if (inst.isJumpInst()) {
				if (op instanceof AssemblerOperand) {
					return new JumpOperand((AssemblerOperand) op);
				}
					
			}
		}
		return op;
	}
	public OperandParser getOperandParser() {
		return operandParser;
	}
		    


}
