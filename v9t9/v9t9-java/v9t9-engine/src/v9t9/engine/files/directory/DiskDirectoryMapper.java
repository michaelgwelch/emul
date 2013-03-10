/*
  DiskDirectoryMapper.java

  (c) 2010-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.files.directory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import v9t9.common.files.Catalog;
import v9t9.common.files.IFilesInDirectoryMapper;

import ejs.base.properties.IPersistable;
import ejs.base.properties.IProperty;
import ejs.base.properties.IPropertyListener;
import ejs.base.settings.ISettingSection;




public class DiskDirectoryMapper implements IFilesInDirectoryMapper, IPersistable {
	private Map<String, File> diskMap = new HashMap<String, File>();
	private Map<String, String> deviceToDiskMap = new HashMap<String, String>();
	private Map<String, String> diskToDeviceMap = new HashMap<String, String>();
	private Map<String, IProperty> diskSettingsMap = new HashMap<String, IProperty>();
	private Map<String, IPropertyListener> diskSettingsListenerMap = new HashMap<String, IPropertyListener>();
	
	public DiskDirectoryMapper() {
	}

	public void registerDiskSetting(String device, IProperty diskSetting) {
		diskMap.put(device, new File(diskSetting.getString()));
		
		deviceToDiskMap.put(device, diskSetting.getName());
		diskToDeviceMap.put(diskSetting.getName(), device);
		
		diskSettingsMap.put(device, diskSetting); 
		IPropertyListener listener = new IPropertyListener() {
			
			public void propertyChanged(IProperty setting) {
				diskMap.put(diskToDeviceMap.get(setting.getName()), new File(setting.getString()));
			}
		};
		diskSettingsListenerMap.put(device, listener);
		diskSetting.addListener(listener);
	}

	public void unregisterDiskSetting(String devname) {
		diskMap.remove(devname);
		IProperty diskSetting = diskSettingsMap.remove(devname);
		if (diskSetting != null) {
			IPropertyListener listener = diskSettingsListenerMap.remove(devname);
			diskSetting.removeListener(listener);
		}
	}

	public void setDiskPath(String device, File dir) {
		diskMap.put(device, dir);
		IProperty diskSetting = diskSettingsMap.get(device);
		if (diskSetting != null) {
			try {
				diskSetting.setString(dir.getCanonicalPath());
			} catch (IOException e) {
				diskSetting.setString(dir.getAbsolutePath());
			}
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.EmuDiskDsr.IFileMapper#getSetting()
	 */
	public IProperty[] getSettings() {
		ArrayList<? extends IProperty> list = new ArrayList<IProperty>(diskSettingsMap.values());
		Collections.sort(list);
		return (IProperty[]) list.toArray(new IProperty[list.size()]);
	}
	

	public synchronized void saveState(ISettingSection settings) {
		for (Map.Entry<String, IProperty> entry : diskSettingsMap.entrySet()) {
			entry.getValue().saveState(settings);
		}
	}

	public synchronized void loadState(ISettingSection settings) {
		if (settings == null) return;
		for (Map.Entry<String, IProperty> entry : diskSettingsMap.entrySet()) {
			entry.getValue().loadState(settings);
		}
	}


	public File getLocalRoot(File file) {
		while (file != null) {
			for (Map.Entry<String, File> entry : diskMap.entrySet()) {
				if (entry.getValue().equals(file)) {
					return file;
				}
			}

			file = file.getParentFile();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.EmuDiskDsr.IFileMapper#getLocalDevice(java.lang.String)
	 */
	public String getDeviceNamed(String dsrName) {
		String localName = getLocalFileName(dsrName);
		for (Map.Entry<String, File> entry : diskMap.entrySet()) {
			if (entry.getValue().getName().equals(localName)) {
				return entry.getKey();
			}
		}
		return null;
	}

	/*	In V9t9 6.0, we used 8.3 filenames, and these chars
		were converted by adding 0x80 to the name on disk. 
		In this version, we still have illegal chars, but
		they are replaced with the escape sequence '&#xx;' as
		in HTML. */
	private final String DOS_illegalchars       = "<>=,;:*?[]/\\";
	private final String DOS_illegalchars_linux = "<>=,;:*?[]\u00BB\\";

	private char FIAD_esc = '&';
	private final String FIAD_illegalchars = "<>,:*?/\\";

	/**	Convert a TI filename to a DOS 8.3 filename.
	 *
	 * We replace illegal chars with high-ASCII characters
	 */
	public String dsrToDOS(String tiname) {
		StringBuilder dosname = new StringBuilder();

		int max = 10;
		int ptr = 0;

		while (ptr < tiname.length() && max-- > 0) {
			char cur;

			cur = tiname.charAt(ptr);

			/* forced end-of-filename? */
			if (cur == ' ' || cur == 0)
				break;

			if (ptr == 8)
				dosname.append('.');

			/* offset illegal chars */
			if (DOS_illegalchars.indexOf(cur) >= 0)
				cur |= 0x80;

			/* force uppercase */
			if (cur >= 'a' && cur <= 'z')
				cur -= 0x20;

			dosname.append(cur);
			ptr++;
		}

		// fiad_logger(_L | L_2,
		// _("fiad_filename_ti2dos:  incoming = '%.*s', outgoing = '%s'\n"),
		// 10 - max, tiname,dosname);
		return dosname.toString();
	}


	/**	Convert a TI filename to a DOS 8.3 filename.
	 *
	 * We replace illegal chars with high-ASCII characters,
	 * and then replace those with what Linux sees (UTF-8
	 * charmapped variants)
	 */
	public String dsrToDOSLinux(String tiname) {
		StringBuilder dosname = new StringBuilder();

		int max = 10;
		int ptr = 0;

		while (ptr < tiname.length() && max-- > 0) {
			char cur;

			cur = tiname.charAt(ptr);

			/* forced end-of-filename? */
			if (cur == ' ' || cur == 0)
				break;

			if (ptr == 8)
				dosname.append('.');

			/* offset illegal chars */
			int illidx = DOS_illegalchars.indexOf(cur); 
			if (illidx >= 0) {
				cur = DOS_illegalchars_linux.charAt(illidx);
			}

			/* force uppercase */
			if (cur >= 'a' && cur <= 'z')
				cur -= 0x20;

			dosname.append(cur);
			ptr++;
		}

		// fiad_logger(_L | L_2,
		// _("fiad_filename_ti2dos:  incoming = '%.*s', outgoing = '%s'\n"),
		// 10 - max, tiname,dosname);
		return dosname.toString();
	}

	/** Convert a TI filename to the host OS, assuming a modern
	 * filesystem.  

	   We convert illegal chars in FIAD_illegalchars into HTML-like
	   encodings (&#xx;) so all possible filenames can be stored.
	*/
	public String dsrToHost(String tiname) {
		StringBuilder hostname = new StringBuilder();
		int max = 10;
		int tptr = 0;

		while (tptr < tiname.length() && max-- > 0) {
			char cur = tiname.charAt(tptr++);

			/* force lowercase */
			if (cur >= 'A' && cur <= 'Z')
				cur += 0x20;
			else
			// illegal chars
			if (cur == FIAD_esc || FIAD_illegalchars.indexOf(cur) >= 0) {
				char hex;

				hostname.append('&');
				hostname.append('#');
				hex = (char) ((cur & 0xf0) >> 4);
				if (hex > 9)
					hex += 'A' - 10;
				else
					hex += '0';
				hostname.append(hex);
				hex = (char) (cur & 0xf);
				if (hex > 9)
					hex += 'A' - 10;
				else
					hex += '0';
				hostname.append(hex);
				cur = ';';
			}

			hostname.append(cur);
		}
		// fiad_logger(_L | L_2,
		// _("fiad_filename_ti2host:  incoming = '%.*s', outgoing = '%s'\n"),
		// 10 - max, tiname, hostname);
		return hostname.toString();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.EmuDiskDsr.IFileMapper#getLocalFilePath(java.lang.String)
	 */
	public String getLocalFileName(String dsrPath) {
		return dsrToHost(dsrPath);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.EmuDiskDsr.IFileMapper#getLocalDottedFile(java.lang.String)
	 */
	public File getLocalDottedFile(String deviceFilename) {
		int idx = deviceFilename.indexOf('.');
		if (idx < 0)
			return getLocalFile(deviceFilename, null);
		else
			return getLocalFile(deviceFilename.substring(0, idx), deviceFilename.substring(idx + 1));
	}
	
	public File getLocalFile(String device, String filename) {
		if (!device.equals("DSK")) {
			File dir = diskMap.get(device);
			if (dir == null) {
				return null;
			}
			if (filename == null || filename.length() == 0)
				return dir;
			
			String[] cands = new String[] {
					getLocalFileName(filename),
					dsrToDOS(filename),
					dsrToDOSLinux(filename)
			};
			
			File preferred = new File(dir, cands[0]);
			
			String[] names = dir.list();
			if (names != null) {
				for (String candName : cands) {
					// do case-insensitive check
					for (String name : names) {
						if (name.equalsIgnoreCase(candName)
								|| name.equalsIgnoreCase(candName + ".bin")) {
							File cand = new File(dir, name);
							if (cand.exists())
								return cand;
						}
					}
				}
			}
			return preferred;
		} else {
			int idx = filename.indexOf('.');
			String diskName = filename.substring(0, idx >= 0 ? idx : filename.length());
			device = getDeviceNamed(diskName);
			if (device == null)
				return null;
			filename = filename.substring(diskName.length() + 1);
			return getLocalFile(device, filename);
		}
	}
	
	protected boolean isxdigit(char ch) {
		return (ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f') || (ch >= '0' && ch <= '9');
	}
	protected int hexval(char ch) {
		if (ch >= 'A' && ch <= 'F')
			return 10 + (ch - 'A');
		if (ch >= 'a' && ch <= 'f')
			return 10 + (ch - 'a');
		return ch - '0';
	}

	/** Convert a filename to TI format */
	public String hostToDSR(String hostname) {
		StringBuilder tiname = new StringBuilder();

		int hptr = 0, max = 10;

		while (hptr < hostname.length() && max > 0) {
			char cur = hostname.charAt(hptr);

			if (cur != '.') {
				/* force uppercase */
				if (Character.isLowerCase(cur))
					cur = Character.toUpperCase(cur);
				else if (cur == '&' && hptr + 4 < hostname.length()
						&& hostname.charAt(hptr + 1) == '#'
						&& isxdigit(hostname.charAt(hptr + 2))
						&& isxdigit(hostname.charAt(hptr + 3))
						&& hostname.charAt(hptr + 4) == ';') {
					int val;

					val = hexval(hostname.charAt(hptr + 2));
					cur = (char) (hexval(hostname.charAt(hptr + 3)) | (val << 4));
					hptr += 4;
				} else if ((cur & 0x80) != 0
						&& DOS_illegalchars.indexOf(cur & 0x7f) >= 0) {
					cur ^= 0x80;
				}

				tiname.append(cur);
				max--;
			}
			hptr++;
		}
		// fiad_logger(_L | L_2,
		// _("fiad_filename_host2ti:  incoming: '%s', outgoing: '%.10s'\n"),
		// hostname, tiname);

		return tiname.toString();
	}

	
	public String getDsrFileName(String filename) {
		return hostToDSR(filename);
	}
	
	public String getDsrDeviceName(File dir) {
		for (Map.Entry<String, File> entry : diskMap.entrySet()) {
			if (entry.getValue().equals(dir)) {
				return entry.getKey();
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.files.IFilesInDirectoryMapper#createCatalog(java.io.File)
	 */
	@Override
	public Catalog createCatalog(File dir) throws IOException {
		FileDirectory fileDir = new FileDirectory(dir, this);

		Catalog catalog = fileDir.readCatalog();

		return catalog;
	}

}