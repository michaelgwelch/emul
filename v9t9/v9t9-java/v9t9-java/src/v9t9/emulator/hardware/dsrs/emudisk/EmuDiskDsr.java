/**
 * 
 */
package v9t9.emulator.hardware.dsrs.emudisk;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ejs.coffee.core.properties.SettingProperty;
import org.ejs.coffee.core.settings.ISettingSection;

import v9t9.emulator.Emulator;
import v9t9.emulator.clients.builtin.IconSetting;
import v9t9.emulator.clients.builtin.swt.IDevIcons;
import v9t9.emulator.clients.builtin.swt.IDeviceIndicatorProvider;
import v9t9.emulator.common.WorkspaceSettings;
import v9t9.emulator.hardware.dsrs.CatalogEntry;
import v9t9.emulator.hardware.dsrs.DeviceIndicatorProvider;
import v9t9.emulator.hardware.dsrs.DsrException;
import v9t9.emulator.hardware.dsrs.DsrHandler;
import v9t9.emulator.hardware.dsrs.DsrHandler9900;
import v9t9.emulator.hardware.dsrs.MemoryTransfer;
import v9t9.emulator.hardware.dsrs.PabConstants;
import v9t9.emulator.hardware.dsrs.emudisk.DiskDirectoryMapper.EmuDiskSetting;
import v9t9.emulator.hardware.dsrs.emudisk.EmuDiskPabHandler.PabInfoBlock;
import v9t9.emulator.hardware.dsrs.realdisk.StandardDiskImageDsr;
import v9t9.emulator.runtime.cpu.Executor;
import v9t9.engine.files.FDR;
import v9t9.engine.files.V9t9FDR;
import v9t9.engine.memory.DiskMemoryEntry;
import v9t9.engine.memory.MemoryDomain;

/**
 * This is a device handler which allows accessing files on the local filesystem.
 * Each directory is a disk.  The DSR instructions in ROM are "enhanced instructions"
 * that forward to the DSR manager and trigger this code.
 * @author ejs
 *
 */
public class EmuDiskDsr implements DsrHandler9900 {
	private static URL diskDirectoryIconPath = Emulator.getDataURL("icons/disk_directory.png");
	
	public static final SettingProperty emuDiskDsrEnabled = new IconSetting("EmuDiskDSREnabled", 
			"Disk Directory Support",
			"This implements a drive (like DSK1) in a single directory level on your host.",
			Boolean.FALSE,
			diskDirectoryIconPath);
	
	/* emudisk.dsr */
	/* this first group doubles as device codes */
	public static final int D_DSK = 0; 	// standard file operation on DSK.XXXX.[YYYY]
	public static final int D_DSK1 = 1;	// standard file operation on DSK1.[YYYY]
	public static final int D_DSK2 = 2;	// ...
	public static final int D_DSK3 = 3;	// ...
	public static final int D_DSK4 = 4;	// ...
	public static final int D_DSK5 = 5;	// ...

	public static final int MAXDRIVE = 5;
	public static final int D_INIT = 6;		// initialize disk DSR
	public static final int D_DSKSUB = 7;	// subroutines

	public static final int D_SECRW = 7;	// sector read/write    (10)
	public static final int D_FMTDISK = 8;	// format disk          (11)
	public static final int D_PROT = 9;		// file protection      (12)
	public static final int D_RENAME = 10;	// rename file          (13)
	public static final int D_DINPUT = 11;	// direct input file    (14)
	public static final int D_DOUTPUT = 12;	// direct output file   (15)
	public static final int D_16 = 13;		// set the VDP end of buffer (like call files) (16)

	public static final int D_FILES = 14;	
			
	/*	Error codes for subroutines */
	public static final byte es_okay = 0;
	public static final byte es_outofspace = 0x4;
	public static final byte es_cantopenfile = 0x1;
	public static final byte es_filenotfound = 0x1;
	public static final byte es_badfuncerr = 0x7;
	public static final byte es_fileexists = 0x7;
	public static final byte es_badvalerr = 0x1;
	public static final byte es_hardware = 0x6;
	
	private DiskMemoryEntry memoryEntry;
	private short vdpnamebuffer;
	private final IFileMapper mapper;

	private Map<String, SettingProperty> diskActivitySettings;
	private List<IDeviceIndicatorProvider> deviceIndicatorProviders;

	private SettingProperty emuDiskDsrActiveSetting;
	
	public static String getEmuDiskSetting(int i) {
		return "DSK" + i;
	}

	public EmuDiskDsr(IFileMapper mapper) {
		//emuDiskDsrEnabled.setBoolean(true);
		WorkspaceSettings.CURRENT.register(emuDiskDsrEnabled);
		
		this.mapper = mapper;
		
		
    	String diskRootPath = WorkspaceSettings.CURRENT.getConfigDirectory() + "disks";
    	File diskRootDir = new File(diskRootPath);
    	File dskdefault = new File(diskRootDir, "default");
    	dskdefault.mkdirs();
    	
    	deviceIndicatorProviders = new ArrayList<IDeviceIndicatorProvider>();
    	diskActivitySettings = new HashMap<String, SettingProperty>();

    	// one setting for entire DSR
		emuDiskDsrActiveSetting = new SettingProperty(getName(), Boolean.FALSE);
		emuDiskDsrActiveSetting.addEnablementDependency(emuDiskDsrEnabled);
		DeviceIndicatorProvider deviceIndicatorProvider = new DeviceIndicatorProvider(
				emuDiskDsrActiveSetting, 
				"Disk directory activity",
				IDevIcons.DSR_DISK_DIR, IDevIcons.DSR_LIGHT);
		deviceIndicatorProviders.add(deviceIndicatorProvider);

		
    	for (int dev = 1; dev <= 5; dev++) {
    		String devname = getEmuDiskSetting(dev);
    		
    		EmuDiskSetting diskSetting = new EmuDiskSetting(devname, dskdefault.getAbsolutePath(),
    				diskDirectoryIconPath);
    		WorkspaceSettings.CURRENT.register(diskSetting);
			
			DiskDirectoryMapper.INSTANCE.registerDiskSetting(devname, diskSetting);

			// one setting per disk
			SettingProperty diskActiveSetting = new SettingProperty(devname, Boolean.FALSE);
			diskActiveSetting.addEnablementDependency(emuDiskDsrEnabled);
			diskActivitySettings.put(devname, diskActiveSetting);
			/*
			deviceIndicatorProvider = new DeviceIndicatorProvider(
					diskActiveSetting, 
					devname + " activity",
					20, 21);
			deviceIndicatorProviders.add(deviceIndicatorProvider);
			*/
    	}
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#dispose()
	 */
	public void dispose() {
		
	}
	
	public String getName() {
		return "Emulated Disk DSR";
				
	}
	public short getCruBase() {
		return 0x1000;
	}
	public void activate(MemoryDomain console) throws IOException {
		if (!emuDiskDsrEnabled.getBoolean())
			return;
		
		emuDiskDsrActiveSetting.setBoolean(true);

		if (memoryEntry == null)
			this.memoryEntry = DiskMemoryEntry.newWordMemoryFromFile(
					0x4000, 0x2000, "File Stream DSR ROM", console,
					"emudisk.bin", 0, false);
		
		console.mapEntry(memoryEntry);
	}
	
	public void deactivate(MemoryDomain console) {
		console.unmapEntry(memoryEntry);
		emuDiskDsrActiveSetting.setBoolean(false);
	}

	public boolean handleDSR(MemoryTransfer xfer, short code) {
		if (!emuDiskDsrEnabled.getBoolean())
			return false;
		
		try {
			return doHandleDSR(xfer, code);
		} finally {
			
		}
	}

	private boolean doHandleDSR(MemoryTransfer xfer, short code) {
		switch (code) {
		// PAB file operation on DSKx 
		case D_DSK:
			// find disk
		case D_DSK1:
		case D_DSK2:
		case D_DSK3:
		case D_DSK4:
		case D_DSK5:
		{
			EmuDiskPabHandler handler = new EmuDiskPabHandler(getCruBase(), xfer, mapper);
			
			if (handler.devname.equals("DSK1")
					|| handler.devname.equals("DSK2")
					|| (handler.devname.equals("DSK") && handler.mapper.getLocalFile(handler.devname, handler.fname) == null)) {
				if (StandardDiskImageDsr.diskImageDsrEnabled.getBoolean())
					return false;
			}
			
			diskActivitySettings.get(handler.devname).setBoolean(true);
			
			info(handler.toString());
			try {
				handler.run();
			} catch (DsrException e) {
				handler.error(e);
			}
			handler.store();
	
			diskActivitySettings.get(handler.devname).setBoolean(false);
			
			//  return, indicating that the DSR handled the operation 
			return true;
		}
			/* init disk dsr */
		case D_INIT:
		{
			for (SettingProperty property : diskActivitySettings.values()) {
				property.setBoolean(true);
			}
			
			EmuDiskPabHandler.getPabInfoBlock(getCruBase()).reset();
			DirectDiskHandler.getDiskInfoBlock(getCruBase()).reset();
			
			// also steal some RAM for the name compare buffer,
			//  so dependent programs can function 
			vdpnamebuffer = (short) (xfer.readParamWord(0x70) - 9);
			xfer.writeParamWord(0x70, (short) (vdpnamebuffer - 1));
			
			// ???
			xfer.writeParamWord(0x6c, (short) 0x404);
			
			for (SettingProperty property : diskActivitySettings.values()) {
				property.setBoolean(false);
			}
			
			return false;  // does not bump return
		}
	
			/* ???? */
		/*
		case D_16:
		{
			console.writeByte(rambase+0x50, 0);	// no error 
			bumpReturnAddress(cpu);
			break;
		}*/
	
			/* call files(x) */
		case D_FILES:
			PabInfoBlock block = EmuDiskPabHandler.getPabInfoBlock(getCruBase());
			
			int cnt = xfer.readParamWord(0x4c);
			if (block.openFiles.size() > cnt) {
				xfer.writeParamWord(0x50, (short) -1);
			} else if (cnt < 1 || cnt >= 16) { 
				xfer.writeParamWord(0x50, (short) -1);
			} else {
				xfer.writeParamWord(0x50, (short) 0);
				block.maxOpenFileCount = cnt;
			}
			return true;
	
		//case D_FMTDISK:
		//case D_PROT:
		//case D_RENAME:
		case D_DINPUT:
		case D_DOUTPUT:
		case D_SECRW:
		{
			DirectDiskHandler handler = new DirectDiskHandler(getCruBase(), xfer, mapper, code);
	
			if (handler.dev <= 2 && StandardDiskImageDsr.diskImageDsrEnabled.getBoolean()) {
				//Executor.settingDumpFullInstructions.setBoolean(true);
				return false;
			}
			
			if (handler.getDevice() <= MAXDRIVE) {
				
				SettingProperty activity = diskActivitySettings.get(getEmuDiskSetting(handler.dev));
				if (activity != null)
					activity.setBoolean(true);
				
				try {
					handler.run();
				} catch (DsrException e) {
					handler.error(e);
				} finally {
					if (activity != null)
						activity.setBoolean(false);
				}
				return true;
			} else {
				// unhandled device
				return false;
			}
		}
	
		default:
			info("EmuDiskDSR: ignoring code = " + code);
			return false;
		}
	}

	/**
	 * @param string
	 */
	static void info(String string) {
		if (Executor.settingDumpFullInstructions.getBoolean())
			Executor.getDumpfull().println(string);
		System.out.println(string);
		
	}

	public static FDR createNewFDR(String dsrFile) throws DsrException {
		// make a FDR file for it
		V9t9FDR fdr = new V9t9FDR();
		try {
			fdr.setFileName(dsrFile);
		} catch (IOException e2) {
			throw new DsrException(PabConstants.e_badfiletype, e2);
		}
		return fdr;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#getEditableSettingGroups()
	 */
	public Map<String, Collection<SettingProperty>> getEditableSettingGroups() {
		Map<String, Collection<SettingProperty>> map = new LinkedHashMap<String, Collection<SettingProperty>>();
		
		Collection<SettingProperty> settings = new ArrayList<SettingProperty>();
		settings.add(emuDiskDsrEnabled);
		map.put(DsrHandler.GROUP_DSR_SELECTION, settings);
		
		settings = Arrays.asList(mapper.getSettings());
		map.put(DsrHandler.GROUP_DISK_CONFIGURATION, settings);
		
		return map;
	}
	public void saveState(ISettingSection section) {
		emuDiskDsrEnabled.saveState(section);
		mapper.saveState(section.addSection("Mappings"));
	}
	
	public void loadState(ISettingSection section) {
		if (section == null) return;
		emuDiskDsrEnabled.loadState(section);
		mapper.loadState(section.getSection("Mappings"));
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.DsrHandler#getDeviceIndicatorProvider()
	 */
	@Override
	public List<IDeviceIndicatorProvider> getDeviceIndicatorProviders() {
		return deviceIndicatorProviders;
	}
	

	public List<CatalogEntry> readCatalog(File file) {
		File dir = file.isDirectory() ? file : file.getParentFile();
		FileLikeDirectoryInfo info = new FileLikeDirectoryInfo(dir, mapper);
		return info.readCatalog();
	}
}
