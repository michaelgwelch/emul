/*
  SerialInstructionRangeCompiler.java

  (c) 2008-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.compiler;

import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;

import v9t9.common.asm.IDecompileInfo;
import v9t9.common.asm.RawInstruction;
import v9t9.common.client.ISettingsHandler;
import v9t9.common.compiler.ICompiler;

/**
 * Compile each instruction, assuming every one is an entry point.
	 * This must finalize info.sw.
 * @author ejs
 *
 */
public class SerialInstructionRangeCompiler implements InstructionRangeCompiler {

	private ISettingsHandler settings;

	public SerialInstructionRangeCompiler(ISettingsHandler settings) {
		this.settings = settings;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.runtime.Compiler.InstructionRangeCompiler#compileInstructionRange(v9t9.emulator.runtime.Compiler, int, int, v9t9.emulator.runtime.HighLevelCodeInfo, org.apache.bcel.generic.InstructionList, v9t9.emulator.runtime.CompileInfo)
	 */
	public void compileInstructionRange(ICompiler compiler, RawInstruction[] insts,
			IDecompileInfo highLevel, InstructionList ilist, CompileInfo info) {
	    // discover the instructions for the block
		int numinsts = insts.length;
	    int addr = insts[0].pc;
	    //int size = insts[numinsts - 1].pc + insts[numinsts - 1].size - addr;
	    
	    /*
	    // pare down instructions to only those that will be hit
	    for (int i = 0; i < numinsts; ) { 
	        Instruction ins = insts[i];
	        MachineOperand mop1 = (MachineOperand) ins.op1;
	        //MachineOperand mop2 = (MachineOperand) ins.op2;
	        int skip = ins.size / 2 - 1;
	        if (Compiler.settingCompileOptimizeCallsWithData.getBoolean()
	                && (ins.inst == Instruction.Ibl || ins.inst == Instruction.Iblwp)
	                && mop1.isConstant()) {
	            short target = mop1.getEA(compiler.cpu.console, ins.pc, compiler.cpu.getWP());
	            
	            FunctionInfo fi = highLevel.getFunctionInfoFromCall(ins, target);
	            if (fi != null) {
	            	skip += fi.paramWords;
	            }
	        }
	        i++;
	        while (--skip > 0 && i < numinsts) {
	        	//block.instructions.remove(insts[i]);
	            insts[i++] = null;
	        }
	    }
	    
	    // remove spurious status setters
	    if (Compiler.settingOptimize.getBoolean() && Compiler.settingOptimizeStatus.getBoolean()) {
	    	LLInstructionOptimizer.peephole_status(insts, numinsts);
		}
		*/
	    // generate all the code for each addr
	    CompiledInstInfo[] chunks = new CompiledInstInfo[numinsts];
	    for (int i = 0; i < numinsts; i++) {
	        if (insts[i] != null) {
	            chunks[i] = new CompiledInstInfo();
	            info.ilist = new InstructionList();
	            ((CompilerBase) compiler).generateInstruction((short) (addr + i * 2),
	                    insts[i], info, chunks[i]);
	
	            // not compiled?
	            if (chunks[i].chunk == null)
	            	chunks[i] = null;
	            
	            // lifetime calculations
	            if (settings.get(ICompiler.settingOptimize).getBoolean() && chunks[i] != null) {
	            	BytecodeOptimizer.peephole(info, chunks[i]);
				}
	        }
	    }
	
	    // have each chunk branch to appropriate instruction in list
	    for (int i = 0; i < numinsts; i ++) {
	        CompiledInstInfo ii = chunks[i];
	        if (ii != null) {
	            if (ii.ins.getInfo().jump == v9t9.common.asm.InstInfo.INST_JUMP_FALSE) {
	            	// not a jump, goto the next code block 
	            	//
	            	// note: we insert a GOTO in case the instruction is greater than 2 bytes,
	            	// since the (garbage) words in the instruction are also compiled
	            	if (ii.ins.getSize() > 2 || (i + 1 < numinsts && chunks[i+1] == null)) {
	                    short target = (short) (ii.ins.pc + ii.ins.getSize());
	                    int index = (target - addr) / 2;
	                    //if (target < addr + size && index >= 0
	                    //        && index < size && chunks[index / 2] != null) {
	                    if (index >= 0 && index < numinsts && chunks[index] != null) {
	                        ii.chunk.append(new GOTO(chunks[index].chunk
	                                .getStart()));
	                    } else {
	                    	ii.chunk.append(new GOTO(info.doneInst));
	                    }
	            	}
	            } else {
	            	// the jump has updated the PC, so re-switch 
	                ii.chunk.append(new GOTO(info.doneInst));
	            }
	        }
	    }
	
	    // Complete switch table.  It must handle every value from
	    // [addr, addr+size), but we can point to a "null handler" and
	    // return to force interpretation if an errant instruction is reached.
	    InstructionHandle firstNullInstructionHandler = null;
	    for (int i = 0; i < numinsts; i++) {
            InstructionHandle ih = null;
            if (chunks[i] != null) {
				ih = chunks[i].chunk.getStart();
			}
            if (ih != null) {
                ilist.append(chunks[i].chunk);
                info.sw.setTarget(i, ih);
            } else {
                if (firstNullInstructionHandler == null) {
                    ih = ilist.append(InstructionConstants.ICONST_0);
                    ilist.append(new GOTO(info.breakInst));
                    firstNullInstructionHandler = ih;
                } else {
                    ih = firstNullInstructionHandler;
                }
                info.sw.setTarget(i, ih);

            }
	    }

	}

}
