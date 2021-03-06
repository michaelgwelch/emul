/*
  DebugConditionListener9900.java

  (c) 2010-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.machine.ti99.cpu;

import java.io.PrintWriter;
import java.util.LinkedList;

import ejs.base.utils.HexUtils;


import v9t9.common.cpu.ICpu;
import v9t9.common.cpu.IInstructionListener;
import v9t9.common.cpu.InstructionWorkBlock;

/**
 * @author ejs
 *
 */
public class DebugConditionListener9900 implements IInstructionListener {

	private LinkedList<InstructionWorkBlock9900> blocks = new LinkedList<InstructionWorkBlock9900>();
	private ICpu cpu;
	
	public DebugConditionListener9900(ICpu cpu)  {
		this.cpu = cpu;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.cpu.IInstructionListener#preExecute(v9t9.common.cpu.InstructionWorkBlock)
	 */
	@Override
	public boolean preExecute(InstructionWorkBlock before) {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.runtime.InstructionListener#executed(v9t9.engine.cpu.InstructionWorkBlock, v9t9.engine.cpu.InstructionWorkBlock)
	 */
	@Override
	public void executed(InstructionWorkBlock before_, InstructionWorkBlock after_) {
		InstructionWorkBlock9900 before = (InstructionWorkBlock9900) before_;
		InstructionWorkBlock9900 after = (InstructionWorkBlock9900) after_;
		if (blocks.size() > 1024)
			blocks.remove(0);
		blocks.add(before);
		
		int sp = before.cpu.getConsole().readWord(before.wp + 10 * 2) & 0xffff;
		if (before.ea1 == before.wp + 10 * 2 &&
				sp < 0xf740 - 0x40) {
			DumpFullReporter9900 dfp = new DumpFullReporter9900((Cpu9900) cpu);
			PrintWriter pw = new PrintWriter(System.err);
			for (InstructionWorkBlock9900 block : blocks) {
				dfp.dumpFullStart(block, block.inst, pw);
				pw.println();
			}
			dfp.dumpFullStart(before, before.inst, pw);
			dfp.dumpFullEnd(after, 0, (MachineOperand9900)before.inst.getOp1(), (MachineOperand9900)before.inst.getOp2(), pw);
			pw.println();
			System.err.println("stack underflow at " + before.inst + ": " + HexUtils.toHex4(sp));
		}
	}

}
