/**
 * 
 */
package v9t9.engine.cpu;

import v9t9.base.properties.SettingProperty;
import v9t9.common.cpu.AbortedException;
import v9t9.common.cpu.ICpu;
import v9t9.engine.compiler.ICompilerStrategy;

/**
 * @author ejs
 *
 */
public interface IExecutor {

	static public final String sCompile = "Compile";
	static public final SettingProperty settingCompile = new SettingProperty(
			sCompile, new Boolean(false));
	static public final SettingProperty settingSingleStep = new SettingProperty(
			"SingleStep", new Boolean(false));

	void interpretOneInstruction();

	/** 
	 * Run an unbounded amount of code.  Some external factor
	 * tells the execution unit when to stop.  The interpret/compile
	 * setting is sticky until execution is interrupted.
	 * @throws AbortedException when interrupt or other machine event stops execution
	 */
	void execute();

	void recordMetrics();

	/**
	 * @return
	 */
	IInstructionListener[] getInstructionListeners();

	void addInstructionListener(IInstructionListener listener);

	void removeInstructionListener(IInstructionListener listener);

	/**
	 * @return
	 */
	ICompilerStrategy getCompilerStrategy();

	/**
	 * @param cpu the cpu to set
	 */
	void setCpu(ICpu cpu);

	/**
	 * @return the cpu
	 */
	ICpu getCpu();

	/**
	 * Record a context switch
	 */
	void recordSwitch();

	/**
	 * @param nInstructions
	 * @param nCycles
	 */
	void recordCompileRun(int nInstructions, int nCycles);

	/**
	 * 
	 */
	void recordCompilation();

	/**
	 * @param count 
	 * @return
	 */
	boolean breakAfterExecution(int count);

	/**
	 * @param i
	 */
	void debugCount(int i);

	/**
	 * 
	 */
	void vdpInterrupt();

	/**
	 * 
	 */
	void resetVdpInterrupts();

	/**
	 * 
	 */
	void interruptExecution();

}