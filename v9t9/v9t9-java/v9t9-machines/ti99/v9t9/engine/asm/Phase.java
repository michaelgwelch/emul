/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Feb 23, 2006
 *
 */
package v9t9.engine.asm;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ejs.coffee.core.utils.Check;
import org.ejs.coffee.core.utils.HexUtils;

import v9t9.emulator.runtime.cpu.CpuState;
import v9t9.engine.asm.Block;
import v9t9.engine.asm.IDecompileInfo;
import v9t9.engine.asm.IHighLevelInstruction;
import v9t9.engine.asm.Label;
import v9t9.engine.asm.MemoryRange;
import v9t9.engine.asm.Routine;
import v9t9.engine.cpu.Inst9900;
import v9t9.engine.cpu.InstTableCommon;
import v9t9.engine.cpu.InstructionWorkBlock9900;
import v9t9.engine.cpu.MachineOperand;
import v9t9.engine.cpu.MachineOperand9900;
import v9t9.engine.cpu.RawInstruction;
import v9t9.engine.memory.MemoryDomain;

public abstract class Phase {
	protected Map<Integer, Block> blocks;
	protected MemoryDomain mainMemory;
	public IDecompileInfo decompileInfo;
	protected Map<Block, Label> labels;
	protected Map<Label, Routine> routines;
	private CpuState state;
	
	public Phase(CpuState state, IDecompileInfo info) {
		this.decompileInfo = info;
		this.state = state;
		this.mainMemory = state.getConsole();
		//this.setBlocks(new TreeSet<Block>());
		blocks = info.getBlockMap();
		labels = info.getLabelMap();
		routines = info.getRoutineMap();
		//blocks = new TreeMap<Integer, Block>();
		//labels = new TreeMap<Block, Label>();
		//this.labels = codeProvider.getLabelMap();
		//labels = info.getLabelMap();
		//this.routines = codeProvider.getRoutineMap();
		//this.routines = info.getRoutines();
		//routines = new TreeMap<Label, Routine>();
	}

	public Map<Block, Label> getLabels() {
		return labels;
	}

	/**
	 * Disassemble all the code.  Required after adding ranges.
	 *
	 */
	public Collection<MemoryRange> disassemble() {
		List<MemoryRange> ranges = new ArrayList<MemoryRange>();
		MemoryRange prev = null;
		MemoryRange range = null;
		for (Iterator<MemoryRange> iter = decompileInfo.getMemoryRanges().rangeIterator(); iter
				.hasNext();) {
			range = iter.next();
			if (prev != null && prev.isCode()) {
				IHighLevelInstruction first = decompileInfo.disassemble(prev.from, range.from - prev.from);
				prev.setCode(first);
				ranges.add(prev);
			}
			prev = range;
		}
		return ranges;
	}
/*
 public MemoryDomain getCPUMemory() {
        return CPU;
    }

    public void addInstruction(LLInstruction inst) {
        instructionMap.put(new Integer(inst.pc), inst);
    }

    public LLInstruction getInstruction(int addr) {
        return instructionMap.get(new Integer(addr));
    }

    public Iterator<LLInstruction> instructionIterator() {
        return instructionMap.values().iterator();
    }

    public MemoryRanges getRanges() {
        return ranges;
    }
 */
	

	public void dumpInstructions(PrintStream os) {
		for (Iterator<MemoryRange> iter = decompileInfo.getMemoryRanges().rangeIterator(); iter
				.hasNext();) {
			MemoryRange range = iter.next();
			for (IHighLevelInstruction inst = (IHighLevelInstruction) range.getCode(); inst != null; inst = inst.getNext()) {
				dumpInstruction(os, inst);
			}
		}
	}

	public void dumpInstructions(PrintStream os, Collection<MemoryRange> ranges) {
		for (MemoryRange range : ranges) {
			for (IHighLevelInstruction inst = range.getCode(); inst != null; inst = inst.getNext()) {
				dumpInstruction(os, inst);
			}
		}
	}

	public void dumpInstruction(PrintStream os, IHighLevelInstruction inst) {
		if (inst.getBlock() != null && inst.getBlock().getFirst() == inst) {
			os.println(inst.getBlock().format());
		}
		Label label = getLabel(inst.getInst().pc);
		if (label != null) {
			os.println(label);
		}
		os.print('\t');
		//        os.println("WP="+ Utils.toHex4(inst.wp) +" " + inst.format(true, true));
		os.println(inst.format(true, true));
	}

	protected Block getLabelKey(int addr) {
		IHighLevelInstruction inst = decompileInfo.getLLInstructions().get(addr);
		if (inst == null)
			return null;
		if (inst.getBlock() == null)
			return null;
		if (inst.getBlock().getFirst().getInst().pc == addr)
			return inst.getBlock();
		return null;
	}

	public Label getLabel(int addr) {
		Block block = getLabelKey(addr);
		if (block == null)
			return null;
		Label label = labels.get(block);
		return label;
	}

	/**
	 * Add a label for the given address.
	 * The label is created with the given parameters,
	 * and a routine is realized by setting its label to this.
	 * If a label already exists, an exception is emitted.
	 * @param bufaddr
	 * @param path
	 * @param routine
	 * @return
	 */
	/*
	public Label addLabel(int addr, boolean rel, int pc, String name) {
		Integer key = getLabelKey(addr);
		Label label = labels.get(key);
		Check.checkArg(label == null);

		
		label = new Label((short) addr, name);
		label.llll(null);
		label.rel = rel;
		label.rels = 0;
		labels.put(key, label);

		return label;
	}
*/
	

	private void addProgramList(int list) {
		int addr, link;
		char[] nameChars = new char[256];
		int len;
		System.out.printf("Scanning program list at >%04X\n", list);
		while (list != 0) {
			link = mainMemory.readWord(list);
			addr = mainMemory.readWord(list + 2);
			if (validCodeAddress(addr)) {
				len = mainMemory.readByte(list + 4);
				String name = null;
				if (len > 0) {
					for (int i = 0; i < len; i++) {
						nameChars[i] = (char) mainMemory.readByte(list + 5 + i);
					}
					name = new String(nameChars, 0, len);
				}
				System.out.printf("Adding routine %s at >%04X\n",
						name != null ? name : "<unnamed>", addr);
				addRoutine(addr, name, new LinkedRoutine());
			}
			list = link;
		}

	}

	public Routine getRoutine(int addr) {
		Label label = getLabel(addr);
		if (label == null) {
			return null;
		}
		return routines.get(label);
	}

	/**
	 * Add a routine at the given address.  Any label already existing
	 * here is renamed.
	 * @param addr
	 * @param name
	 * @param routine
	 * @return same incoming routine, updated with a label and added to the routines
	 */
	public Routine addRoutine(int addr, String name, Routine routine) {
		Check.checkState(validCodeAddress(addr));
		
		Label label = decompileInfo.findOrCreateLabel(addr);
		if (name != null && label.getName() == null) {
			label.setName(name);
		}
		routine.addEntry(label);
		routines.put(label, routine);
		return routine;
	}

	/**
	 * Add REF/DEF tables, where each entry points to the END of the table
	 *
	 */
	public void addRefDefTables(List<Integer> refDefTables) {

		// Get explicit symbol tables
		for (Object element : refDefTables) {
			int addr = ((Integer) element).intValue();
			MemoryRange range = decompileInfo.getMemoryRanges().getRangeContaining(addr - 1);
			if (range == null) {
				System.err.println("!!! Can't find range containing >"
						+ HexUtils.toHex4((addr - 1)));
				continue;
			}

			int ptr = addr;
			char[] nameChars = new char[6];
			while (true) {
				ptr -= 2;
				addr = mainMemory.readWord(ptr);
				if (addr == 0) {
					break;
				}
				int length = 6;
				for (int i = 0; i < 6; i++) {
					int pos = 5 - i;
					nameChars[pos] = (char) mainMemory.readByte(--ptr);
					if (nameChars[pos] == ' ') {
						length = pos;
					}
				}

				// now, these are almost always vectors, so take the PC
				String name = new String(nameChars, 0, length);
				short wp = mainMemory.readWord(addr);
				addr = mainMemory.readWord(addr + 2);
				if (validCodeAddress(addr)) {
					System.out.println("Adding label " + name + " at >"
							+ HexUtils.toHex4(addr));
					addRoutine(addr, name, new ContextSwitchRoutine(wp));
				}
			}
		}
	}

	public void addStandardROMRoutines() {
		// Get standard entries
		for (int addr = 0; addr < 0x10000; addr += 0x2000) {
			if (mainMemory.readByte(addr) == (byte) 0xaa) {
				System.out.println("Scanning standard header at >"
						+ HexUtils.toHex4(addr));
				int paddr = mainMemory.readWord(addr + 4);
				addProgramList(paddr);
				paddr = mainMemory.readWord(addr + 6);
				addProgramList(paddr);
				paddr = mainMemory.readWord(addr + 8);
				addProgramList(paddr);
				paddr = mainMemory.readWord(addr + 10);
				addProgramList(paddr);
			}

			if (addr == 0) {

				addPossibleContextSwitch(0, "RESET");

				// int1
				addPossibleContextSwitch(4, "INT1");

				// int2
				addPossibleContextSwitch(8, "INT2");

				for (int xop = 0; xop < 2; xop++) {
					// XOP
					addPossibleContextSwitch(0x40 + xop * 4, "XOP" + xop);
				}
			}
		}
	}

	protected Routine addPossibleContextSwitch(int ctx, String name) {
		short wp = mainMemory.readWord(ctx);
		int addr = mainMemory.readWord(ctx + 2);
		if (wp == (short) addr || wp == ctx) {
			return null;
		}
		if (/*mainMemory.hasRamAccess(wp) && mainMemory.hasRamAccess(wp + 31)
				&&*/ (addr & 1) == 0
				&& validCodeAddress(addr)) {
			System.out.println("Adding " + name + " vector at >"
					+ HexUtils.toHex4(addr));
			Routine routine = addRoutine(addr, name, new ContextSwitchRoutine(
					wp));
			return routine;
		}
		return null;
	}

	public short operandEffectiveAddress(IHighLevelInstruction inst, MachineOperand mop) {
		InstructionWorkBlock9900 block = new InstructionWorkBlock9900(state);
		block.inst = inst.getInst();
		return mop.getEA(block);
	}

	public boolean operandIsLabel(IHighLevelInstruction inst, MachineOperand mop) {
		return mop.isLabel()
				&& decompileInfo.getMemoryRanges().getRangeContaining(operandEffectiveAddress(
						inst, mop)) != null;
	}

	//  operand is relocatable if it's in our memory
	//  and is a direct address, a jump target, or
	//  a nontrivial register indirect (a likely lookup table)
	public boolean operandIsRelocatable(IHighLevelInstruction inst, MachineOperand9900 mop) {
		if (inst.getInst().getInst() == Inst9900.Ilwpi) {
			return true;
		}
		if (!(mop instanceof MachineOperand)) {
			return false;
		}
		return (mop.type == MachineOperand9900.OP_ADDR
				&& (mop.val == 0 || mop.immed >= 0x20) || mop.type == MachineOperand9900.OP_JUMP)
				&& decompileInfo.getMemoryRanges().getRangeContaining(operandEffectiveAddress(
						inst, mop)) != null;

	}

	protected boolean validCodeAddress(int addr) {
		MemoryRange range = decompileInfo.getMemoryRanges().getRangeContaining(addr);
		if (range == null) {
			return false;
		}
		if (!range.isCode()) {
			return false;
		}
		RawInstruction inst = decompileInfo.getInstruction(new Integer(addr));
		if (inst == null) {
			return false;
		}
		if (inst.getInst() == InstTableCommon.Idata) {
			return false;
		}
		return true;
	}

	public void dumpLabels(PrintStream os) {
		for (Object element : labels.values()) {
			Label label = (Label) element;
			os.println(label);
		}
	}

	public void dumpBlocks(PrintStream os) {
		for (Object element : getBlocks()) {
			Block block = (Block) element;
			dumpBlock(os, block);
		}
	}

	public void dumpRoutines(PrintStream os) {
		for (Object element : getRoutines()) {
			Routine routine = (Routine) element;
			os.print("routine: " + routine);
			if ((routine.flags & Routine.fSubroutine) != 0)
				os.print(" [subroutine]");
			if ((routine.flags & Routine.fUnknownExit) != 0)
				os.print(" [unknownExit]");
			os.println();

			Collection<Block> blocks = routine.getSpannedBlocks();
			os.print("blocks = [");
			for (Block block : blocks) {
				os.print(block.getId() + " ");
			}
			os.println("]");
			for (Block block : blocks) {
				dumpBlock(os, block);
			}
			os.println("-------------------");

		}
	}

	public void dumpBlock(PrintStream os, Block block) {
		for (Iterator<IHighLevelInstruction> iter = block.iterator(); iter.hasNext();) {
			IHighLevelInstruction inst = iter.next();
			dumpInstruction(os, inst);
		}
		System.out.println();
	}

	public void addBlock(Block block) {
		blocks.put(block.getFirst().getInst().pc, block);
	}
	public Set<Block> getBlocks() {
		return new TreeSet<Block>(blocks.values());
	}

	public Collection<Routine> getRoutines() {
		return routines.values();
	}

}