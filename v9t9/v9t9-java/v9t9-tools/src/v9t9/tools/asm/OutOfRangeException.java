/*
  OutOfRangeException.java

  (c) 2008-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.asm;


import ejs.base.utils.HexUtils;
import v9t9.common.asm.IInstruction;
import v9t9.common.asm.IOperand;
import v9t9.common.asm.ResolveException;

/**
 * @author ejs
 *
 */
public class OutOfRangeException extends ResolveException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7132296027636428230L;
	private final Symbol symbol;

	/**
	 * @param inst
	 * @param op
	 * @param string
	 */
	public OutOfRangeException(IInstruction inst, IOperand op, Symbol symbol, int range) {
		super(op, "Jump out of range (>" + HexUtils.toHex4((range / 2)) + " words)");
		this.symbol = symbol;
	}
	
	public Symbol getSymbol() {
		return symbol;
	}

}
