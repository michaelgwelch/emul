/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Dec 15, 2004
 *
 */
package v9t9.engine.memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import v9t9.base.properties.IPersistable;
import v9t9.base.settings.ISettingSection;
import v9t9.base.utils.ListenerList;
import v9t9.common.memory.IMemory;
import v9t9.common.memory.IMemoryAccess;
import v9t9.common.memory.IMemoryAccessListener;
import v9t9.common.memory.IMemoryDomain;
import v9t9.common.memory.IMemoryEntry;
import v9t9.common.memory.IMemoryWriteListener;

/**
 * @author ejs
 */
public class MemoryDomain implements IMemoryAccess, IPersistable, IMemoryDomain {
    static final int NUMAREAS = PHYSMEMORYSIZE >> AREASHIFT;
    
	public IMemoryAccessListener nullMemoryAccessListener = new IMemoryAccessListener() {

		public void access(IMemoryEntry entry) {
		}
    	
    };
    
    public IMemoryWriteListener nullMemoryWriteListener = new IMemoryWriteListener() {

		public void changed(IMemoryEntry entry, int addr, boolean isByte) {
		}
    	
    };
    
    
    private IMemoryAccessListener accessListener = nullMemoryAccessListener;
    private ListenerList<IMemoryWriteListener> writeListeners = new ListenerList<IMemoryWriteListener>();
    
    private IMemoryEntry entries[] = new IMemoryEntry[NUMAREAS];
    
    private Stack<IMemoryEntry> mappedEntries = new Stack<IMemoryEntry>();
	private IMemoryEntry zeroMemoryEntry;
	private final String name;
	public IMemory memory;
    
	
    public MemoryDomain(String name, int latency) {
    	this.name = name;
		zeroMemoryEntry = new MemoryEntry("Unmapped memory",
    			this,
    			0,
    			PHYSMEMORYSIZE,
    			new ZeroWordMemoryArea(latency));
    	
        //setArea(0, PHYSMEMORYSIZE, area);
    	mapEntry(zeroMemoryEntry);
    }
    
	public MemoryDomain(String name) {
    	this(name, 0);
    }
    
    /** For testing, create a RAM-accessible memory domain which spans
     * the size of data.
     * @param data populating data, length on AREASIZE boundary 
     * @return
     */
    public static MemoryDomain newFromArray(short[] data, boolean bWordAccess) {
        MemoryDomain domain = new MemoryDomain(IMemoryDomain.NAME_CPU);
        WordMemoryArea area = WordMemoryArea.newDefaultArea();
        area.bWordAccess = bWordAccess;
        area.memory = data;
        area.read = data;
        area.write = data;
        IMemoryEntry entry = new MemoryEntry("Test Entry",
        		domain, 0, data.length * 2,
        		area);
        domain.mapEntry(entry);
        return domain;
    }    

    public static IMemoryDomain newFromArray(byte[] data) {
        IMemoryDomain domain = new MemoryDomain(IMemoryDomain.NAME_CPU);
        ByteMemoryArea area = new ByteMemoryArea();
        area.memory = data;
        area.read = data;
        area.write = data;
        IMemoryEntry entry = new MemoryEntry("Test Entry",
        		domain, 0, data.length,
        		area);
        domain.mapEntry(entry);
        return domain;
    }  
    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#getEntryAt(int)
	 */
    @Override
	public final IMemoryEntry getEntryAt(int addr) {
        return entries[(addr & PHYSMEMORYSIZE - 1) >> AREASHIFT];
    }

    void mapEntryAreas(int addr, int size, IMemoryEntry entry) {
        if (size == 0)
        	return;
        	
        if (size < AREASIZE
                || (addr & AREASIZE - 1) != 0) {
			throw new AssertionError(
                    "attempt made to set a memory handler on an illegal boundary\n"
                            + "(" + Integer.toHexString(addr) + "..."
                            + Integer.toHexString(addr + size - 1)
                            + "), the minimum granularity is "
                            + Integer.toHexString(AREASIZE)
                            + " bytes");
		}

        if (size > PHYSMEMORYSIZE || addr >= PHYSMEMORYSIZE
                || addr + size > PHYSMEMORYSIZE) {
			throw new AssertionError("illegal address or size (64k limit)");
		}

        size = size + AREASIZE - 1 >> AREASHIFT;
        addr >>= AREASHIFT;
        while (size-- != 0) {
            entries[addr++] = entry;
        }
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#flatReadWord(int)
	 */
    @Override
	public final short flatReadWord(int addr) {
        IMemoryEntry entry = getEntryAt(addr);
        accessListener.access(entry);
        return entry.flatReadWord(addr);
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#flatReadByte(int)
	 */
    @Override
	public final byte flatReadByte(int addr) {
    	IMemoryEntry entry = getEntryAt(addr);
        accessListener.access(entry);
        return entry.flatReadByte(addr);
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#flatWriteByte(int, byte)
	 */
    @Override
	public final void flatWriteByte(int addr, byte val) {
    	IMemoryEntry entry = getEntryAt(addr);
        accessListener.access(entry);
        entry.flatWriteByte(addr, val);
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#flatWriteWord(int, short)
	 */
    @Override
	public final void flatWriteWord(int addr, short val) {
    	IMemoryEntry entry = getEntryAt(addr);
        accessListener.access(entry);
        entry.flatWriteWord(addr, val);
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#readByte(int)
	 */
    @Override
	public final byte readByte(int addr) {
    	IMemoryEntry entry = getEntryAt(addr);
        accessListener.access(entry);
        return entry.readByte(addr);
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#readWord(int)
	 */
    @Override
	public final short readWord(int addr) {
    	IMemoryEntry entry = getEntryAt(addr);
        accessListener.access(entry);
        return entry.readWord(addr);
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#writeByte(int, byte)
	 */
    @Override
	public final void writeByte(int addr, byte val) {
    	IMemoryEntry entry = getEntryAt(addr);
        accessListener.access(entry);
        entry.writeByte(addr, val);
        if (!writeListeners.isEmpty())
        	fireWriteEvent(entry, addr & 0xffff, true);
    }

    private void fireWriteEvent(final IMemoryEntry entry, final int addr, final boolean isByte) {
    	for (Object listenerObj : writeListeners.toArray()) {
    		((IMemoryWriteListener) listenerObj).changed(entry, addr, isByte);
    	}
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#writeWord(int, short)
	 */
	@Override
	public final void writeWord(int addr, short val) {
        IMemoryEntry entry = getEntryAt(addr);
        accessListener.access(entry);
        entry.writeWord(addr, val);
        if (!writeListeners.isEmpty())
        	fireWriteEvent(entry, addr & 0xfffe, false);
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#hasRamAccess(int)
	 */
    @Override
	public final boolean hasRamAccess(int addr) {
    	IMemoryEntry entry = getEntryAt(addr);
        return entry != null && entry.hasWriteAccess();
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#hasRomAccess(int)
	 */
    @Override
	public final boolean hasRomAccess(int addr) {
        IMemoryEntry entry = getEntryAt(addr);
        return entry != null && entry.hasReadAccess();
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#isVolatile(int)
	 */
    @Override
	public final boolean isVolatile(int addr) {
    	IMemoryEntry entry = getEntryAt(addr);
        return entry != null && entry.isVolatile();
    }


    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#isStatic(int)
	 */
    @Override
	public final boolean isStatic(int addr) {
    	IMemoryEntry entry = getEntryAt(addr);
        return entry != null && entry.isStatic();
    }

    /* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#zero()
	 */
    @Override
	public void zero() {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = zeroMemoryEntry;
        }
    }

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#setAccessListener(v9t9.common.memory.IMemoryAccessListener)
	 */
	@Override
	public void setAccessListener(IMemoryAccessListener listener) {
		if (listener == null)
			listener = nullMemoryAccessListener;
		this.accessListener = listener;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#addWriteListener(v9t9.common.memory.IMemoryWriteListener)
	 */
	@Override
	public synchronized void addWriteListener(IMemoryWriteListener listener) {
		writeListeners.add(listener);
	}
	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#removeWriteListener(v9t9.common.memory.IMemoryWriteListener)
	 */
	@Override
	public synchronized void removeWriteListener(IMemoryWriteListener listener) {
		writeListeners.remove(listener);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#getLatency(int)
	 */
	@Override
	public int getLatency(int addr) {
		IMemoryEntry entry = getEntryAt(addr);
		return entry.getLatency();
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#isEntryMapped(v9t9.common.memory.IMemoryEntry)
	 */
	@Override
	public boolean isEntryMapped(IMemoryEntry memoryEntry) {
		return mappedEntries.contains(memoryEntry);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#isEntryFullyMapped(v9t9.common.memory.IMemoryEntry)
	 */
	@Override
	public boolean isEntryFullyMapped(IMemoryEntry memoryEntry) {
		for (int addr = memoryEntry.getAddr(); addr < memoryEntry.getAddr() + memoryEntry.getSize(); addr += AREASIZE) {
			IMemoryEntry theEntry = getEntryAt(addr);
			if (theEntry != memoryEntry)
				return false;
		}
        return true;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#isEntryFullyUnmapped(v9t9.common.memory.IMemoryEntry)
	 */
	@Override
	public boolean isEntryFullyUnmapped(IMemoryEntry memoryEntry) {
		for (int addr = memoryEntry.getAddr(); addr < memoryEntry.getAddr() + memoryEntry.getSize(); addr += AREASIZE) {
			IMemoryEntry theEntry = getEntryAt(addr);
			if (theEntry == memoryEntry)
				return false;
		}
        return true;
	}


	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#mapEntry(v9t9.common.memory.IMemoryEntry)
	 */
	@Override
	public void mapEntry(IMemoryEntry memoryEntry) {
		if (!mappedEntries.contains(memoryEntry))
			mappedEntries.add(memoryEntry);
		((MemoryEntry) memoryEntry).memory = memory;
		mapEntryAreas(memoryEntry);
		memoryEntry.onMap();
	}

	private void mapEntryAreas(IMemoryEntry memoryEntry) {
		mapEntryAreas(memoryEntry.getAddr(), memoryEntry.getSize(), memoryEntry);
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#unmapEntry(v9t9.common.memory.IMemoryEntry)
	 */
	@Override
	public void unmapEntry(IMemoryEntry memoryEntry) {
		if (memoryEntry == null)
			return;
		
		// TODO: remove from end?
		mappedEntries.remove(memoryEntry);
		
		for (IMemoryEntry entry : mappedEntries) {
			mapEntryAreas(entry.getAddr(), entry.getSize(), entry);
		}
		memoryEntry.onUnmap();
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#switchBankedEntry(v9t9.common.memory.IMemoryEntry, v9t9.common.memory.IMemoryEntry)
	 */
	@Override
	public void switchBankedEntry(IMemoryEntry currentBank,
			IMemoryEntry newBankEntry) {
		if (currentBank != null && newBankEntry != null && isEntryMapped(currentBank)) {
			if (currentBank != newBankEntry) {
				mappedEntries.remove(currentBank);
				currentBank.onUnmap();
				mappedEntries.add(newBankEntry);
				newBankEntry.onMap();
				mapEntryAreas(newBankEntry);
			}
		} else {
			if (currentBank != null) 
				unmapEntry(currentBank);
			if (newBankEntry != null)
				mapEntry(newBankEntry);
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#saveState(v9t9.base.settings.ISettingSection)
	 */
	@Override
	public void saveState(ISettingSection section) {
		int idx = 0;
		for (IMemoryEntry entry : mappedEntries) {
			if (entry != zeroMemoryEntry && !isEntryFullyUnmapped(entry)) {
				entry.saveState(section.addSection(""+ idx));
				idx++;
			}
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#loadState(v9t9.base.settings.ISettingSection)
	 */
	@Override
	public void loadState(ISettingSection section) {
		//unmapAll();
		if (section == null) {
			return;
		}

		for (ISettingSection entryStore : section.getSections()) {
			String name = entryStore.get("Name");
			IMemoryEntry entry = findMappedEntry(name);
			if (entry != null) {
				entry.loadState(entryStore);
			} else {
				entry = MemoryEntry.createEntry(this, entryStore);
				if (entry != null)
					mapEntry(entry);
				else
					System.err.println("Cannot find memory entry: " + name);
			}
		}
		
	}


	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#findFullyMappedEntry(java.lang.String)
	 */
	@Override
	public IMemoryEntry findFullyMappedEntry(String name) {
		for (IMemoryEntry entry : mappedEntries) {
			if (entry.getName().equals(name) && isEntryFullyMapped(entry)) {
				return entry;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#findMappedEntry(java.lang.String)
	 */
	@Override
	public IMemoryEntry findMappedEntry(String name) {
		for (IMemoryEntry entry : mappedEntries) {
			if (entry.getName().equals(name) && !isEntryFullyUnmapped(entry)) {
				return entry;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#unmapAll()
	 */
	@Override
	public void unmapAll() {
		mappedEntries.clear();
		mapEntry(zeroMemoryEntry);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#getMemoryEntries()
	 */
	@Override
	public IMemoryEntry[] getMemoryEntries() {
		return entries;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#getFlattenedMemoryEntries()
	 */
	@Override
	public IMemoryEntry[] getFlattenedMemoryEntries() {
		List<IMemoryEntry> entryList = new ArrayList<IMemoryEntry>();
		for (IMemoryEntry entry : mappedEntries) {
			if (entry == zeroMemoryEntry  || !isEntryMapped(entry))
				continue;
			if (entry instanceof MultiBankedMemoryEntry) {
				MultiBankedMemoryEntry banked = (MultiBankedMemoryEntry) entry;
				entryList.addAll(Arrays.asList(banked.getBanks()));
			} else if (entry instanceof BankedMemoryEntry) {
				BankedMemoryEntry banked = (BankedMemoryEntry) entry;
				for (int i = 0; i < banked.getBankCount(); i++) {
					entryList.add(new BankedMemoryProxyEntry(banked, i));
				}
			} else {
				entryList.add(entry);
			}
		}
		return (IMemoryEntry[]) entryList.toArray(new IMemoryEntry[entryList.size()]);
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#writeMemory(int)
	 */
	@Override
	public void writeMemory(int addr) {
		if (!writeListeners.isEmpty())
			fireWriteEvent(getEntryAt(addr), addr, true);
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#save()
	 */
	@Override
	public void save() {
		for (IMemoryEntry entry : mappedEntries) {
			try {
				entry.save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.common.memory.IMemoryDomain#getMemory()
	 */
	@Override
	public IMemory getMemory() {
		return memory;
	}
}