/**
 * 
 */
package v9t9.tools.asm.assembler;

import java.util.ArrayList;
import java.util.List;

import v9t9.engine.cpu.Operand;

/**
 * Parse a 9900 operand.
 * @author ejs
 *
 */
public class OperandParser {

	private List<IOperandParserStage> stages = new ArrayList<IOperandParserStage>(1);
	
	/** Create a standard operand parser that understands
	 * raw operands (no symbols).
	 */
	public OperandParser() {
	}
	
	public void prependStage(IOperandParserStage stage) {
		stages.add(0, stage);
	}
	public void appendStage(IOperandParserStage stage) {
		stages.add(stage);
	}

	public Operand parse(AssemblerTokenizer tokenizer) throws ParseException {
		TokenizerState state = tokenizer.getState();
		for (IOperandParserStage stage : stages) {
			Operand op = stage.parse(tokenizer);
			if (op != null)
				return op;
			tokenizer.setState(state);
		}
		throw new ParseException("Unknown operand");
    }

}