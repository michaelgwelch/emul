/*
  CpuInstructionComposite.java

  (c) 2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.gui.client.swt.shells.debugger;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

import v9t9.common.asm.RawInstruction;
import v9t9.common.cpu.ICpu;
import v9t9.common.cpu.IExecutor;
import v9t9.common.cpu.InstructionWorkBlock;
import v9t9.common.machine.IMachine;
import v9t9.common.settings.Settings;
import ejs.base.properties.IProperty;

/**
 * @author ejs
 *
 */
public abstract class CpuInstructionComposite extends Composite {

	protected static final int MAX_INST_HISTORY = 250000;
	protected IMachine machine;
	protected IProperty pauseMachine;
	protected IProperty debugging;
	protected IProperty singleStep;
	private Runnable refreshTask;
	private boolean isDirty;
	protected final List<InstRow> instHistory = new ArrayList<InstRow>();
	
	public CpuInstructionComposite(Composite parent, int style, IMachine machine) {
		super(parent, style);
		this.machine = machine;
		
		pauseMachine = Settings.get(machine, IMachine.settingPauseMachine);
		debugging = Settings.get(machine, ICpu.settingDebugging);
		singleStep = Settings.get(machine, IExecutor.settingSingleStep);

	}
	
	protected void start() {

		refreshTask = new Runnable() {
			volatile boolean busy = false;
			//volatile long nextTime;
			
			@Override
			public void run() {
				if (busy || isDisposed()) // || System.currentTimeMillis() < nextTime)
					return;
				
				busy = true;

				getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (!isDisposed() && isDirty) {
							flush();
							isDirty = false;
							//nextTime = System.currentTimeMillis() + 1000 / 10;
						}
						busy = false;
					}
				});
				
			}
		};
		machine.getFastMachineTimer().scheduleTask(refreshTask, 20);
		
		addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				machine.getFastMachineTimer().cancelTask(refreshTask);
			}
		});
	}
	
	abstract public void flush();
	
	abstract public void setupEvents();

	/**
	 * 
	 */
	abstract public void go();

	/**
	 * @param before
	 * @param after_
	 */
	public void executed(InstructionWorkBlock before,
			InstructionWorkBlock after_) {

		InstructionWorkBlock after = after_.copy();
        
        InstRow row = new InstRow(before, after);
        
        synchronized (instHistory) {
        	if (instHistory.size() >= MAX_INST_HISTORY) {
        		instHistory.subList(0, MAX_INST_HISTORY / 2).clear();
        	}
	        if (instHistory.size() > 0 && instHistory.get(instHistory.size() - 1).isGeneric()) {
	        	instHistory.remove(instHistory.size() - 1);
	        }
	        instHistory.add(row);
	        
	        isDirty = true;
        }
	}

	/**
	 * 
	 */
	public void refresh() {
		RawInstruction inst = machine.getCpu().getCurrentInstruction();
		
		InstructionWorkBlock before = new InstructionWorkBlock(machine.getCpu().getState());
		before.inst = inst;
		before.pc = (short) (inst.pc + inst.getSize());
		
		InstRow row = new InstRow(before);
		synchronized (instHistory) {
			InstRow last = instHistory.size() > 0 ? instHistory.get(instHistory.size() - 1) : null;
			if (last == null || last.getBeforeInst().pc != inst.pc) {
				instHistory.add(row);
				isDirty = true;
			}
		}
	}

	/**
	 * 
	 */
	public void clear() {
		synchronized (instHistory) {
			instHistory.clear();
			flush();
		}
	}
}
