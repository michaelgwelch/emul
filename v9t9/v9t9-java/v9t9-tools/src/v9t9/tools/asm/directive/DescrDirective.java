/*
  DescrDirective.java

  (c) 2008-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.asm.directive;

import v9t9.common.asm.IInstruction;
import v9t9.common.asm.ResolveException;
import v9t9.tools.asm.IAssembler;

/**
 * @author Ed
 *
 */
public class DescrDirective extends Directive {

	private final String content;
	private final int line;
	private final String filename;

	public DescrDirective(String filename, int line, String content) {
		this.filename = filename;
		this.line = line;
		this.content = content;
	}

	@Override
	public String toString() {
		return filename + ":" + line;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IInstruction#resolve(v9t9.tools.asm.Assembler, v9t9.engine.cpu.IInstruction)
	 */
	public IInstruction[] resolve(IAssembler assembler, IInstruction previous, boolean finalPass)
			throws ResolveException {
		return new IInstruction[] { this };
	}

	public String getFilename() {
		return filename;
	}
	public int getLine() {
		return line;
	}
	public String getContent() {
		return content;
	}


}
