/**
 * 
 */
package v9t9.machine.ti99.dsr.pcode;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ejs.base.properties.IProperty;
import ejs.base.properties.IPropertyListener;
import ejs.base.settings.ISettingSection;
import ejs.base.settings.SettingProperty;


import v9t9.common.client.ISettingsHandler;
import v9t9.common.dsr.IDeviceIndicatorProvider;
import v9t9.common.dsr.IMemoryTransfer;
import v9t9.common.events.IEventNotifier.Level;
import v9t9.common.memory.IMemory;
import v9t9.common.memory.IMemoryDomain;
import v9t9.common.memory.IMemoryEntry;
import v9t9.common.memory.IMemoryEntryFactory;
import v9t9.common.modules.MemoryEntryInfo;
import v9t9.common.settings.IconSettingSchema;
import v9t9.common.settings.Settings;
import v9t9.engine.dsr.DeviceIndicatorProvider;
import v9t9.engine.dsr.IDevIcons;
import v9t9.engine.memory.GplMmio;
import v9t9.engine.memory.MemoryDomain;
import v9t9.engine.memory.MemoryEntry;
import v9t9.engine.memory.MemoryEntryInfoBuilder;
import v9t9.machine.EmulatorMachinesData;
import v9t9.machine.ti99.dsr.IDsrHandler9900;
import v9t9.machine.ti99.machine.TI99Machine;
import v9t9.machine.ti99.memory.mmio.ConsoleGramWriteArea;
import v9t9.machine.ti99.memory.mmio.ConsoleGromReadArea;

/**
 * @author ejs
 *
 */
public class PCodeDsr implements IDsrHandler9900 {
	private static URL pcodeIconPath = EmulatorMachinesData.getDataURL("icons/pcode_system.png");

	static public final IconSettingSchema settingPcodeCardEnabled = new IconSettingSchema(
			ISettingsHandler.WORKSPACE,
			"PCodeCardEnabled", "Enable P-Code Card", 
			"Enables the UCSD Pascal P-Code card.",
			new Boolean(false),
			pcodeIconPath);
	private PCodeDsrRomBankedMemoryEntry dsrMemoryEntry;
	private TI99Machine machine;
	private IMemoryDomain pcodeDomain;
	private GplMmio pcodeGromMmio;
	private IMemoryEntry readMmioEntry;
	private IMemoryEntry writeMmioEntry;
	private IMemoryEntry gromMemoryEntry;
	private IProperty pcodeActive;

	private IProperty pcodeCardEnabled;

	public static final String PCODE = "PCODE";
	/**
	 * @param machine
	 */
	public PCodeDsr(TI99Machine machine_) {
		this.machine = machine_;
		pcodeActive = new SettingProperty("pcodeActive", Boolean.FALSE);
		pcodeCardEnabled = Settings.get(machine, settingPcodeCardEnabled);
		pcodeActive.addEnablementDependency(pcodeCardEnabled);
		
		pcodeCardEnabled.addListener(new IPropertyListener() {
			
			@Override
			public void propertyChanged(IProperty property) {
				Settings.get(machine, IDeviceIndicatorProvider.settingDevicesChanged).firePropertyChange();
			}
		});
		
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler9900#getCruBase()
	 */
	@Override
	public short getCruBase() {
		return 0x1f00;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#activate(v9t9.engine.memory.MemoryDomain)
	 */
	@Override
	public void activate(IMemoryDomain console, IMemoryEntryFactory memoryEntryFactory) throws IOException {
		// DSR ROM
		if (!pcodeCardEnabled.getBoolean())
			return;
		
		pcodeActive.setBoolean(true);
		
		IMemory memory = console.getMemory();

		ensureSetup(memoryEntryFactory);
		
		// pCode GROMs are accessed specially
		memory.addAndMap(dsrMemoryEntry);
		memory.addAndMap(readMmioEntry);
		memory.addAndMap(writeMmioEntry);
		
		memory.addAndMap(gromMemoryEntry);
	}

	private static MemoryEntryInfo pcodeDsrRomMemoryEntryInfo = MemoryEntryInfoBuilder
		.wordMemoryEntry()
		.withAddress(0x4000)
		.withSize(0x2000)
		.withFilename("pCodeRomA.bin")
		.withFilename2("pCodeRomB.bin")
		.withBankClass(PCodeDsrRomBankedMemoryEntry.class)
		.create("P-Code DSR ROM");
	
	private static MemoryEntryInfo pcodeGromMemoryEntryInfo = MemoryEntryInfoBuilder
		.byteMemoryEntry()
		.withDomain(IMemoryDomain.NAME_GRAPHICS)
		.withAddress(0x0)
		.withSize(0x10000)
		.withFilename("pCodeGroms.bin")
		.create("P-Code GROM");
	
	private void ensureSetup(IMemoryEntryFactory memoryEntryFactory) throws IOException {
		IMemory memory = machine.getMemory();
		IMemoryDomain console = machine.getConsole();

		if (console.getEntryAt(0x4000) instanceof PCodeDsrRomBankedMemoryEntry)
			dsrMemoryEntry = (PCodeDsrRomBankedMemoryEntry) console.getEntryAt(0x4000);
		
		if (dsrMemoryEntry == null) {
			this.dsrMemoryEntry = (PCodeDsrRomBankedMemoryEntry) 
				memoryEntryFactory.newMemoryEntry(pcodeDsrRomMemoryEntryInfo);
		}
		pcodeDomain = memory.getDomain(PCODE);
		if (pcodeDomain == null) {
			// P-Code GROMs are completely private to the card
			pcodeDomain = new MemoryDomain(PCODE);
			
			memory.addDomain(PCODE, pcodeDomain);
		}
		if (gromMemoryEntry == null) {
			gromMemoryEntry = memoryEntryFactory.newMemoryEntry(
					pcodeGromMemoryEntryInfo);
		}
		
		if (pcodeGromMmio == null) {
			pcodeGromMmio = new GplMmio(machine, pcodeDomain);
			readMmioEntry = null;
			writeMmioEntry = null;
		}
		
		dsrMemoryEntry.setup(machine, pcodeGromMmio);
		
		if (readMmioEntry == null) {
			readMmioEntry = new MemoryEntry("PCode Read MMIO", pcodeDomain, 0x5800, 0x0400,
					new ConsoleGromReadArea(pcodeGromMmio));
	        writeMmioEntry = new MemoryEntry("PCode Write MMIO", pcodeDomain, 0x5C00, 0x0400,
	                new ConsoleGramWriteArea(pcodeGromMmio));
		}
		
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#deactivate(v9t9.engine.memory.MemoryDomain)
	 */
	@Override
	public void deactivate(IMemoryDomain console) {
		IMemory memory = console.getMemory();
		
		if (dsrMemoryEntry != null) {
			memory.removeAndUnmap(gromMemoryEntry);
			memory.removeAndUnmap(dsrMemoryEntry);
			memory.removeAndUnmap(readMmioEntry);
			memory.removeAndUnmap(writeMmioEntry);
		}
		
		pcodeActive.setBoolean(false);
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#dispose()
	 */
	@Override
	public void dispose() {

	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#getEditableSettingGroups()
	 */
	@Override
	public Map<String, Collection<IProperty>> getEditableSettingGroups() {
		return Collections.<String, Collection<IProperty>>singletonMap("UCSD P-System",
				Collections.<IProperty>singletonList(pcodeCardEnabled));
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#getName()
	 */
	@Override
	public String getName() {
		return "UCSD P-System";
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#handleDSR(v9t9.emulator.hardware.dsrs.MemoryTransfer, short)
	 */
	@Override
	public boolean handleDSR(IMemoryTransfer xfer, short code) {
		return false;
	}

	/* (non-Javadoc)
	 * @see v9t9.base.core.properties.IPersistable#loadState(v9t9.base.core.settings.ISettingSection)
	 */
	@Override
	public void loadState(ISettingSection section) {
		if (section == null)
			return;
		
		ISettingSection sub = section.getSection("P-Code");
		if (sub == null)
			return;
		
		pcodeCardEnabled.loadState(sub);
		
		try {
			ensureSetup(machine.getMemoryEntryFactory());
		} catch (IOException e) {
			machine.notifyEvent(Level.ERROR, e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.base.core.properties.IPersistable#saveState(v9t9.base.core.settings.ISettingSection)
	 */
	@Override
	public void saveState(ISettingSection section) {
		ISettingSection sub = section.addSection("P-Code");
		pcodeCardEnabled.saveState(sub);
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#getDeviceIndicatorProviders()
	 */
	@Override
	public List<IDeviceIndicatorProvider> getDeviceIndicatorProviders() {
		if (!Settings.get(machine, settingPcodeCardEnabled).getBoolean())
			return Collections.emptyList();
		
		IDeviceIndicatorProvider provider= new DeviceIndicatorProvider(
				pcodeActive, "USCD P-System Activity", 
				IDevIcons.DSR_USCD, IDevIcons.DSR_LIGHT);
		return Collections.singletonList(provider);
	}
}
