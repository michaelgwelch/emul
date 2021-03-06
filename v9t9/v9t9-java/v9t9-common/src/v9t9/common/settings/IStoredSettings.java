/*
  IStoredSettings.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.settings;

import java.io.IOException;
import java.util.Map;

import ejs.base.properties.IProperty;
import ejs.base.settings.ISettingSection;

import v9t9.common.client.ISettingsHandler;

/**
 * @author ejs
 *
 */
public interface IStoredSettings {

	ISettingsHandler getOwner();
	void setOwner(ISettingsHandler settingsHandler);
	
	void load() throws IOException;
	void load(ISettingSection settings);

	void save() throws IOException;
	void save(ISettingSection settings);


	IProperty find(String settingsName);
	
	Map<IProperty, SettingSchema> getAll();
	
	//void register(IProperty setting);
	IProperty findOrCreate(SettingSchema schema);
	IProperty findOrCreate(SettingSchema schema, Object defaultOverride);

	<T extends IProperty> T findOrCreate(T defaultProperty);

	void clearConfigVar(String configVar);

	ISettingSection getSettings();

	/** Set (override) directory for saving the file,
	 * pass <code>null</code> to reset */
	void setConfigDirectory(String configdir);

	/** Get directory for saving the file */
	String getConfigDirectory();
	
	/** Get full path for saving the file */
	String getConfigFilePath();

	/** Get base filename for saving the file */
	String getConfigFileName();

	/**
	 * Storage for UI preferences, not necessarily tied to the
	 * state of the machine or emulator
	 * @return
	 */
	ISettingSection getHistorySettings();
	

	void setDirty(boolean b);
	boolean isDirty();
	
	void remove(String name);

}