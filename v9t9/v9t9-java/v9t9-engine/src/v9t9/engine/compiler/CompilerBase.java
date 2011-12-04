/**
 * 
 */
package v9t9.engine.compiler;

import java.util.HashMap;
import java.util.Map;


import v9t9.base.properties.SettingProperty;
import v9t9.common.asm.IDecompileInfo;
import v9t9.common.asm.IInstructionFactory;
import v9t9.common.asm.RawInstruction;
import v9t9.common.cpu.ICpuState;
import v9t9.common.memory.IMemoryArea;
import v9t9.common.memory.IMemoryEntry;

/**
 * @author Ed
 *
 */
public abstract class CompilerBase {

	static public final SettingProperty settingOptimize = new SettingProperty("CompilerOptimize",
	new Boolean(false));
	static public final SettingProperty settingOptimizeRegAccess = new SettingProperty(
	"CompilerOptimizeRegAccess", new Boolean(false));
	static public final SettingProperty settingOptimizeStatus = new SettingProperty(
	"CompilerOptimizeStatus", new Boolean(false));
	static public final SettingProperty settingCompileOptimizeCallsWithData = new SettingProperty(
	"CompilerOptmizeCallsWithData", new Boolean(false));
	static public final SettingProperty settingDebugInstructions = new SettingProperty(
	"DebugInstructions", new Boolean(false));
	static public final SettingProperty settingCompileFunctions = new SettingProperty(
	"CompilerCompileFunctions", new Boolean(false));
	

    public Map<IMemoryArea, IDecompileInfo> highLevelCodeInfoMap = new HashMap<IMemoryArea, IDecompileInfo>();
	private IInstructionFactory instructionFactory;
	private ICpuState cpuState;

	/**
	 * 
	 */
	public CompilerBase(ICpuState cpuState, IInstructionFactory instructionFactory) {
		this.cpuState = cpuState;
		this.instructionFactory = instructionFactory;
	}
    /**
	 * Compile the instructions into bytecode.
	 * @param uniqueClassName
	 * @param baseName
	 * @param highLevel
	 * @param insts
	 * @param entries
	 * @return
	 */
	abstract public byte[] compile(String uniqueClassName, String baseName,
			IDecompileInfo highLevel, RawInstruction[] insts, short[] entries);
	/**
	 * Tell if the CPU is coherent and compilation makes sense
	 * @return
	 */
	abstract public boolean validCpuState();

	abstract public void generateInstruction(int pc, RawInstruction rawins,
            CompileInfo info, CompiledInstInfo ii);
	
    /** Currently, only gather high-level info for one memory entry at a time */
    public IDecompileInfo getHighLevelCode(IMemoryEntry entry) {
    	IMemoryArea area = entry.getArea();
    	IDecompileInfo highLevel = highLevelCodeInfoMap.get(area);
    	if (highLevel == null) {
    		System.out.println("Initializing high level info for " + entry + " / " + area);
    		highLevel = instructionFactory.createDecompileInfo(cpuState);
    		highLevel.disassemble(entry.getAddr(), entry.getSize());
    		highLevelCodeInfoMap.put(area, highLevel);
    	}
    	return highLevel;
    }
}
