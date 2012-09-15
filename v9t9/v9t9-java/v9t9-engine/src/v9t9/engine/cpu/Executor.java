/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Dec 17, 2004
 *
 */
package v9t9.engine.cpu;

import ejs.base.properties.IProperty;
import ejs.base.properties.IPropertyListener;
import ejs.base.settings.Logging;
import ejs.base.utils.ListenerList;

import v9t9.common.compiler.ICompiledCode;
import v9t9.common.compiler.ICompiler;
import v9t9.common.compiler.ICompilerStrategy;
import v9t9.common.cpu.AbortedException;
import v9t9.common.cpu.BreakpointManager;
import v9t9.common.cpu.ICpu;
import v9t9.common.cpu.ICpuMetrics;
import v9t9.common.cpu.IExecutor;
import v9t9.common.cpu.IInstructionListener;
import v9t9.common.cpu.MetricEntry;
import v9t9.common.hardware.IVdpChip;
import v9t9.common.machine.IMachine;
import v9t9.common.settings.Settings;
import v9t9.engine.interpreter.IInterpreter;


/**
 * Handle executing instructions, either in interpret mode or compile mode.
 * 
 * @author ejs
 */
public class Executor implements IExecutor {

    private ICpu cpu;

    public IInterpreter interp;
    ICompilerStrategy compilerStrategy;

    public long nInstructions;
    public long nCompiledInstructions;
    public long nSwitches;
    public long nCompiles;

	//private ICpuController cpuController;

	public int nVdpInterrupts;

	/** counter for DBG/DBGF instructions */
    public int debugCount;

	public volatile Boolean interruptExecution;
    
	private ListenerList<IInstructionListener> instructionListeners = new ListenerList<IInstructionListener>();

	private long lastCycleCount;

	private final ICpuMetrics cpuMetrics;

	private IProperty compile;

	private IProperty singleStep;

	private IProperty vdpInterruptRate;

	private IProperty pauseMachine;

	private BreakpointManager breakpointManager;

    public Executor(ICpu cpu, ICpuMetrics cpuMetrics, 
    		IInterpreter interpreter, ICompiler compiler, 
    		ICompilerStrategy compilerStrategy,
    		final IInstructionListener dumpFullReporter, final IInstructionListener dumpReporter) {
    	
    	compile = Settings.get(cpu, settingCompile);
    	singleStep = Settings.get(cpu, settingSingleStep);
    	pauseMachine = Settings.get(cpu, IMachine.settingPauseMachine);
    	vdpInterruptRate = Settings.get(cpu, IVdpChip.settingVdpInterruptRate);
    	
        this.cpu = cpu;
		this.cpuMetrics = cpuMetrics;
        this.interp = interpreter;
        this.compilerStrategy = compilerStrategy;
        
        compilerStrategy.setup(this, compiler);
        
        breakpointManager = new BreakpointManager((IMachine) cpu.getMachine());
        
        final Object lock = Executor.this.cpu.getMachine().getExecutionLock();
        cpu.settingDumpFullInstructions().addListenerAndFire(new IPropertyListener() {

			public void propertyChanged(IProperty setting) {
				synchronized (lock) {
					Settings.get(Executor.this.cpu, 
							IMachine.settingThrottleInterrupts).setBoolean(setting.getBoolean());
					
					if (setting.getBoolean()) {
						Executor.this.addInstructionListener(dumpFullReporter);
					} else {
						Executor.this.removeInstructionListener(dumpFullReporter);
					}
					interruptExecution = Boolean.TRUE;
					lock.notifyAll();
				}
			}
        	
        });
        cpu.settingDumpInstructions().addListenerAndFire(new IPropertyListener() {
			public void propertyChanged(IProperty setting) {
				synchronized (lock) {
					if (setting.getBoolean()) {
						Executor.this.addInstructionListener(dumpReporter);
					} else {
						Executor.this.removeInstructionListener(dumpReporter);
					}
					interruptExecution = Boolean.TRUE;
					lock.notifyAll();
				}
			}
        	
        });
        
        pauseMachine.addListener(new IPropertyListener() {

			public void propertyChanged(IProperty setting) {
				interruptExecution = Boolean.TRUE;
			}
        	
        });
        
        singleStep.addListener(new IPropertyListener() {
        	
        	public void propertyChanged(IProperty setting) {
        		synchronized (lock) {
        			interruptExecution = Boolean.TRUE;
        			lock.notifyAll();
        		}
        	}
        	
        });
        cpu.settingRealTime().addListener(new IPropertyListener() {

			public void propertyChanged(IProperty setting) {
				interruptExecution = Boolean.TRUE;
				//synchronized (lock) {
				//	lock.notifyAll();
				// }
			}
        	
        });
        


        pauseMachine.addListener(new IPropertyListener() {

			public void propertyChanged(IProperty setting) {
				lastCycleCount = 0;				
			}
        	
        });
        
        Logging.registerLog(cpu.settingDumpInstructions(), "instrs.txt");
        Logging.registerLog(cpu.settingDumpFullInstructions(), "instrs_full.txt");
    }

	public IProperty settingCompile() {
		return compile;
	}
	public IProperty settingSingleStep() {
		return singleStep;
	}

    /* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#interpretOneInstruction()
	 */
    @Override
	public synchronized void interpretOneInstruction() {
        interp.executeChunk(1, this);
    }

    /* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#execute()
	 */
    @Override
	public void execute() {
    	if (cpu.isIdle() && cpu.settingRealTime().getBoolean()) {
    		if (cpu.isThrottled())
    			return;
    		/*
    		long start = System.currentTimeMillis();
    		try {
    			// short sleep
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
			long end = System.currentTimeMillis();
			//System.out.print((end - start) + " ");
			cpu.addCycles(cpu.getBaseCyclesPerSec() * (int)(end - start + 500) / 1000);
    		cpu.checkAndHandleInterrupts();
			*/
    		while (!cpu.isThrottled() && nVdpInterrupts < vdpInterruptRate.getInt()) {
    			try {
    				//long start = System.currentTimeMillis();
    				Thread.yield();
    				
    				//long end = System.currentTimeMillis();
    				cpu.addCycles(1);
    				cpu.checkInterrupts();
    			} catch (AbortedException e) {
    				cpu.handleInterrupts();
    				break;
    			}
    		}
    	} else {
			if (compile.getBoolean()) {
				executeCompilableCode();
			} else if (singleStep.getBoolean()) {
				interpretOneInstruction();
			} else {
				interruptExecution = Boolean.FALSE;
				if (cpu.settingRealTime().getBoolean()) {
					while (!cpu.isThrottled() && !interruptExecution) {
						interp.executeChunk(10, this);
					}
				} else {
					interp.executeChunk(100, this);
				}
			}
    	}
    }
    

    private void executeCompilableCode() {
    	try {
	    	boolean interpreting = false;
			if (compile.getBoolean()) {
				/* try to make or run native code, which may fail */
				ICompiledCode code = compilerStrategy.getCompiledCode();
			    if (code == null || !code.run()) {
			    	// Returns false if an instruction couldn't be executed
			    	// because it did not look like real code (or was not expected to be directly invoked).
			    	// Returns true if fell out of the code block.
			    	//System.out.println("Switch  branching to >" + HexUtils.toHex4(cpu.getPC()));
			    	interpreting = true;
			    	nSwitches++;
				}
			} else {
				interpreting = true;
			}
			
			if (interpreting) {
			    interpretOneInstruction();
			    cpu.checkInterrupts();
			}
    	} catch (AbortedException e) {
            cpu.handleInterrupts();
		}
	}
 
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#recordMetrics()
	 */
	@Override
	public final void recordMetrics() {
		
		long totalCycleCount = cpu.getTotalCycleCount();
		if (totalCycleCount == lastCycleCount)
			return;
		
		MetricEntry entry = new MetricEntry(
				(int) nInstructions,
				(int) (totalCycleCount - lastCycleCount), 
				(int) cpu.settingCyclesPerSecond().getInt(),
				nVdpInterrupts, cpu.getAndResetInterruptCount(), 
				nCompiledInstructions, (int) nSwitches, (int) nCompiles);

		cpuMetrics.log(entry);
		
        nInstructions = 0;
        nCompiledInstructions = 0;
        nSwitches = 0;
        nCompiles = 0;
        
        lastCycleCount = totalCycleCount;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#getInstructionListeners()
	 */
	@Override
	public final ListenerList<IInstructionListener> getInstructionListeners() {
		return instructionListeners;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#addInstructionListener(v9t9.engine.cpu.InstructionListener)
	 */
	@Override
	public void addInstructionListener(IInstructionListener listener) {
		instructionListeners.add(listener);
	}
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#removeInstructionListener(v9t9.engine.cpu.InstructionListener)
	 */
	@Override
	public void removeInstructionListener(IInstructionListener listener) {
		instructionListeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#getCompilerStrategy()
	 */
	@Override
	public final ICompilerStrategy getCompilerStrategy() {
		return compilerStrategy;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#setCpu(v9t9.common.cpu.ICpu)
	 */
	@Override
	public void setCpu(ICpu cpu) {
		this.cpu = cpu;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#getCpu()
	 */
	@Override
	public final ICpu getCpu() {
		return cpu;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#recordSwitch()
	 */
	@Override
	public final void recordSwitch() {
		nSwitches++;		
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#recordCompileRun(int, int)
	 */
	@Override
	public final void recordCompileRun(int nInstructions, int nCycles) {
        this.nInstructions += nInstructions;
        nCompiledInstructions += nInstructions;
        getCpu().addCycles(nCycles);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#recordCompilation()
	 */
	@Override
	public final void recordCompilation() {
		 nCompiles++;		
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#breakAfterExecution()
	 */
	@Override
	public final boolean breakAfterExecution(int count) {
		nInstructions += count;
		cpu.checkAndHandleInterrupts();
		return interruptExecution;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#debugCount(int)
	 */
	@Override
	public final void debugCount(int i) {
    	int oldCount = debugCount; 
    	debugCount += i;
    	if ((oldCount == 0) != (debugCount == 0))
    		cpu.settingDumpFullInstructions().setBoolean(i > 0);
		
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#vdpInterrupt()
	 */
	@Override
	public final void vdpInterrupt() {
		nVdpInterrupts++;		
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#resetVdpInterrupts()
	 */
	@Override
	public final void resetVdpInterrupts() {
		nVdpInterrupts = 0;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.cpu.IExecutor#interruptExecution()
	 */
	@Override
	public final void interruptExecution() {
		interruptExecution = Boolean.TRUE;		
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.cpu.IExecutor#getBreakpoints()
	 */
	@Override
	public BreakpointManager getBreakpoints() {
		return breakpointManager;
	}

}