/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Feb 21, 2006
 *
 */
package v9t9.machine.ti99.memory;



import v9t9.common.client.ISettingsHandler;
import v9t9.common.events.IEventNotifier;
import v9t9.common.machine.IBaseMachine;
import v9t9.common.machine.IMachine;
import v9t9.common.memory.IMemoryEntry;
import v9t9.common.settings.SettingSchema;
import v9t9.common.settings.Settings;
import v9t9.engine.memory.MemoryEntry;
import v9t9.machine.ti99.memory.mmio.ConsoleGramWriteArea;
import v9t9.machine.ti99.memory.mmio.ConsoleGromReadArea;
import v9t9.machine.ti99.memory.mmio.ConsoleSoundArea;
import v9t9.machine.ti99.memory.mmio.ConsoleSpeechReadArea;
import v9t9.machine.ti99.memory.mmio.ConsoleSpeechWriteArea;
import v9t9.machine.ti99.memory.mmio.ConsoleVdpReadArea;
import v9t9.machine.ti99.memory.mmio.ConsoleVdpWriteArea;

/**
 * The standard TI-99/4[A] console memory map.
 * @author ejs
 */
public class TI994AStandardConsoleMemoryModel extends BaseTI994AMemoryModel {
    static public final SettingSchema settingExpRam = new SettingSchema(
    		ISettingsHandler.WORKSPACE,
    		"MemoryExpansion32K", new Boolean(false));

    public TI994AStandardConsoleMemoryModel(IMachine machine) {
    	super(machine);
    }
    
    
    /* (non-Javadoc)
     * @see v9t9.engine.memory.MemoryModel#loadMemory()
     */
	@Override
    public void loadMemory(IEventNotifier eventNotifier) {
    	loadConsoleRom(eventNotifier, "994arom.bin");
    	loadConsoleGrom(eventNotifier, "994agrom.bin");    	
    }
    
    /* (non-Javadoc)
     * @see v9t9.engine.memory.MemoryModel#resetMemory()
     */
	@Override
    public void resetMemory() {
    	for (IMemoryEntry entry : CPU.getMemoryEntries()) {
    		if (entry.getAddr() == 0x4000 || entry.getAddr() == 0x6000)
    			CPU.unmapEntry(entry);
    	}
    	for (IMemoryEntry entry : GRAPHICS.getMemoryEntries()) {
    		if (entry.getAddr() >= 0x6000)
    			GRAPHICS.unmapEntry(entry);
    	}
    }
    

    protected void initSettings(ISettingsHandler settings) {
		settings.get(ExpRamArea.settingExpRam).setBoolean(true);
		settings.get(ConsoleRamArea.settingEnhRam).setBoolean(false);
	}
 
	@Override
	protected void defineConsoleMemory(IBaseMachine machine) {
		ISettingsHandler settings = Settings.getSettings(machine);
		memory.addAndMap(new MemoryEntry("Console RAM", CPU, 0x8000, 0x0400,
				new ConsoleRamArea(settings)));
	    MemoryEntry lowRam = new MemoryEntry("Low 8K expansion RAM", CPU, 0x2000,
	            0x2000, new ExpRamArea(settings, 0x2000));
	    lowRam.setVolatile(false);
		memory.addAndMap(lowRam);
	    MemoryEntry highRam = new MemoryEntry("High 24K expansion RAM", CPU, 0xA000,
	            0x6000, new ExpRamArea(settings, 0x6000));
	    highRam.setVolatile(false);
		memory.addAndMap(highRam);
		
	}

   @Override
   protected void defineMmioMemory(IBaseMachine machine) {
        
        this.memory.addAndMap(new MemoryEntry("Sound MMIO", CPU, 0x8400, 0x0400,
                new ConsoleSoundArea(soundMmio)));
        this.memory.addAndMap(new MemoryEntry("VDP Read MMIO", CPU, 0x8800, 0x0400,
                new ConsoleVdpReadArea(vdpMmio)));
        this.memory.addAndMap(new MemoryEntry("VDP Write MMIO", CPU, 0x8C00, 0x0400,
                new ConsoleVdpWriteArea(vdpMmio)));
        this.memory.addAndMap(new MemoryEntry("Speech Read MMIO", CPU, 0x9000, 0x0400,
                new ConsoleSpeechReadArea(speechMmio)));
        this.memory.addAndMap(new MemoryEntry("Speech Write MMIO", CPU, 0x9400, 0x0400,
                new ConsoleSpeechWriteArea(speechMmio)));
        this.memory.addAndMap(new MemoryEntry("GROM Read MMIO", CPU, 0x9800, 0x0400,
                new ConsoleGromReadArea(gplMmio)));
        this.memory.addAndMap(new MemoryEntry("GRAM Write MMIO", CPU, 0x9C00, 0x0400,
                new ConsoleGramWriteArea(gplMmio)));
		
	}
  
}
