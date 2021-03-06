/*
  StockMemoryModel.java

  (c) 2008-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.memory;


import v9t9.common.events.IEventNotifier;
import v9t9.common.machine.IBaseMachine;
import v9t9.common.memory.IMemory;
import v9t9.common.memory.IMemoryDomain;
import v9t9.common.memory.IMemoryModel;
import v9t9.common.memory.MemoryEntryInfo;

/**
 * @author ejs
 *
 */
public class StockMemoryModel implements IMemoryModel {

	private Memory memory;
	private MemoryDomain CPU;

	public StockMemoryModel() {
		memory = new Memory();
		CPU = new MemoryDomain(IMemoryDomain.NAME_CPU);
		memory.addDomain(IMemoryDomain.NAME_CPU, CPU);
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#resetMemory()
	 */
	@Override
	public void resetMemory() {
		
	}
	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getConsole()
	 */
	public IMemoryDomain getConsole() {
		return CPU;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getLatency(int)
	 */
	/**
	 * @param addr  
	 */
	public int getLatency(int addr) {
		return 0;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getMemory()
	 */
	public IMemory getMemory() {
		return memory;
	}

	public void initMemory(IBaseMachine machine) {
		
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#loadMemory(v9t9.emulator.clients.builtin.IEventNotifier)
	 */
	@Override
	public void loadMemory(IEventNotifier eventNotifier) {
		
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryModel#getOptionalRomProperties()
	 */
	@Override
	public MemoryEntryInfo[] getOptionalRomMemoryEntries() {
		return new MemoryEntryInfo[0];
	}
	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryModel#getRequiredRomProperties()
	 */
	@Override
	public MemoryEntryInfo[] getRequiredRomMemoryEntries() {
		return new MemoryEntryInfo[0];
	}
}
