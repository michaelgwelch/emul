/*
  DiskImageSetting.java

  (c) 2011-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.files.image;

import java.net.URL;

import ejs.base.properties.IProperty;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.settings.IconSettingProperty;
import v9t9.common.settings.SettingSchema;
import v9t9.engine.files.directory.EmuDiskSettings;

public class DiskImageSetting extends IconSettingProperty {
	private IProperty realDsrEnabled;
	private IProperty emuDsrEnabled;

	public DiskImageSetting(ISettingsHandler settings, String name, Object storage, URL iconPath) {
		super(new SettingSchema(ISettingsHandler.TRANSIENT,
				name, "DSK" + name.charAt(name.length() - 1) + " Image",
				"Specify the full path of the image for this disk.\n\n"+
				"The extension selects the image type when creating a new image.\n\nUse *.dsk for sector-image disks and *.trk for track image disks.",
				storage), iconPath);
		
		realDsrEnabled = settings.get(RealDiskDsrSettings.diskImageDsrEnabled);
		emuDsrEnabled = settings.get(EmuDiskSettings.emuDiskDsrEnabled);
		addEnablementDependency(emuDsrEnabled);
		addEnablementDependency(realDsrEnabled);
		addEnablementDependency(settings.get(RealDiskDsrSettings.diskImageRealTime));
		addEnablementDependency(settings.get(RealDiskDsrSettings.diskImageDebug));
	}

	@Override
	public boolean isEnabled() {
		if (!realDsrEnabled.getBoolean())
			return false;
		if (!emuDsrEnabled.getBoolean())
			return true;
		
		// only DSK1 and DSK2 are real disks if emu disk also enabled
		return getName().compareTo(RealDiskDsrSettings.getDiskImageSetting(3)) < 0;
	}
}