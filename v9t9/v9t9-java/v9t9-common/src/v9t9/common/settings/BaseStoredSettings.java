/*
  BaseStoredSettings.java

  (c) 2009-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.settings;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ejs.base.properties.AbstractProperty;
import ejs.base.properties.IProperty;
import ejs.base.properties.IPropertyListener;
import ejs.base.settings.ISettingSection;
import ejs.base.settings.ISettingStorage;
import ejs.base.settings.SettingsSection;
import ejs.base.settings.XMLSettingStorage;
import ejs.base.utils.StringUtils;

import v9t9.common.client.ISettingsHandler;

/**
 * @author ejs
 *
 */
public abstract class BaseStoredSettings implements IStoredSettings {
	private static final Logger logger = Logger.getLogger(BaseStoredSettings.class);
	
	/**
	 * 
	 */
	private static final String HISTORY = "History";
	private static class SyntheticProperty extends AbstractProperty implements IReadOnlyProperty {

		private Object value;

		public SyntheticProperty(String name, Object value) {
			super(null, value.getClass(), name);
			this.value = value;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public void setValue(Object value) {
			this.value = value;
		}

		@Override
		public void setValueFromString(String value) {
			throw new UnsupportedOperationException();
		}
		
	}
	private Map<String, SyntheticProperty> syntheticSettings;
	protected Map<String, IProperty> registeredSettings;
	protected Map<String, SettingSchema> settingSchemas;
	
	private static final String ROOT = "root";
	protected ISettingSection section;
	
	
	//protected List<SettingProperty> trackedSettings;
	protected boolean isLoading;
	protected IPropertyListener trackedSettingListener;
	protected boolean isLoaded;
	protected boolean needsSave;
	private final String context;
	private ISettingsHandler owner;

	private String configDir;

	public BaseStoredSettings(String context) {
		setConfigDirectory(null);
		
		this.context = context;
		section = new SettingsSection(null);
		syntheticSettings = new HashMap<String, SyntheticProperty>();
		registeredSettings = new HashMap<String, IProperty>();
		settingSchemas = new HashMap<String, SettingSchema>();
		//trackedSettings = new ArrayList<SettingProperty>();
		trackedSettingListener = new IPropertyListener() {
			
			@Override
			public void propertyChanged(IProperty setting) {
				if (!isLoading)
					needsSave = true;
			}
		};
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(ISettingsHandler owner) {
		this.owner = owner;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.settings.IStoredSettings#getOwner()
	 */
	@Override
	public ISettingsHandler getOwner() {
		return owner;
	}
	
	public synchronized void load() throws IOException {
		File settingsConfigurationFile = new File(getConfigFilePath());
		if (logger.isDebugEnabled()) logger.debug("*** Loading from " + settingsConfigurationFile);
		
		InputStream fis = null;
		try {
			ISettingStorage storage = new XMLSettingStorage(ROOT);
			fis = new BufferedInputStream(new FileInputStream(settingsConfigurationFile));
			section = storage.load(fis);
		} catch (IOException e) {
			needsSave = true;
			section = new SettingsSection(null);
			throw e;
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
			
		isLoaded = true;
		needsSave = false;
		isLoading = true;
		try {
			load(section);
		} finally {
			isLoading = false;
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IStoredSettings#load(v9t9.base.core.settings.ISettingSection)
	 */
	@Override
	public void load(ISettingSection settings) {
		//for (IProperty setting : registeredSettings.values()) {
		//	setting.loadState(settings);
		//}
		for (String name : settings.getSettingNames()) {
			IProperty property = registeredSettings.get(name);
			
			if (getOwner() != null) {
				IStoredSettings store = getOwner().findSettingStorage(name);
				if (store != this && store != null) {
					if (logger.isDebugEnabled()) logger.debug("*** Tossing: " + context + "::" + name + " which lives in " + store.getConfigFileName());
					property = null;
					remove(name);
					continue;
				}
			}
			
			if (property == null || property instanceof SyntheticProperty) {
				if (logger.isDebugEnabled()) logger.debug("Synthesizing: " + context + "::" + name + " = " 
							+ format(settings.getObject(name)));
				Object value = deduceObject(settings.getObject(name));
				SyntheticProperty synProperty = new SyntheticProperty(name, value);
				registeredSettings.put(name, synProperty);
				syntheticSettings.put(name, synProperty);
			} else {
				if (logger.isDebugEnabled()) logger.debug("Loading: "+ context + "::"  + name + " = " 
							+ format(settings.getObject(name)));
				property.loadState(settings);
			}
		}
	}

	/**
	 * @param object
	 * @return
	 */
	private Object deduceObject(Object val) {
		if (val == null)
			return null;
		
		String valStr = val.toString();
		
		if ("true".equals(valStr)) {
			return Boolean.TRUE;
		}
		if ("false".equals(valStr)) {
			return Boolean.FALSE;
		}
		try {
			return Integer.parseInt(valStr);
		} catch (NumberFormatException e) {
			try {
				return Long.parseLong(valStr);
			} catch (NumberFormatException e2) {
				try {
					return Double.parseDouble(valStr);
				}
				catch (NumberFormatException e3) {
				
				}
				return val;
			}
		}
	}

	public synchronized void save() throws IOException {
		String path = getConfigFilePath();
		File file = new File(path);
		file.getParentFile().mkdirs();
		//if (file.exists() && !needsSave)
		//	return;
		
		ISettingSection saveSection = section;
		
		// toss unrecognized keys
		//if (true)
		//	saveSection = new SettingsSection();
		
		save(saveSection);
		
		ISettingStorage storage = new XMLSettingStorage(ROOT);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		storage.save(bos, saveSection);
		
		// see if contents changed
		boolean shouldWrite = true;
		if (file.exists()) {
			InputStream fis = null;
			try {
				int size = (int) file.length();
				if (size == bos.size()) {
					ByteArrayOutputStream bosold = new ByteArrayOutputStream(size);
					byte[] arr = new byte[size];
					fis = new BufferedInputStream(new FileInputStream(file));
					fis.read(arr);
					bosold.write(arr);
					if (Arrays.equals(bos.toByteArray(), bosold.toByteArray())) {
						shouldWrite = false;
					}
				}
			} catch (IOException e) {
				// ignore
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		}

		if (shouldWrite) {
			File backup = new File(path + "~");
			file.renameTo(backup);
			
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(bos.toByteArray());
			fos.close();
		}
		
		needsSave = false;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IStoredSettings#save(v9t9.base.core.settings.ISettingSection)
	 */
	@Override
	public void save(ISettingSection settings) {
		for (IProperty setting : registeredSettings.values()) {
			if (setting instanceof SyntheticProperty) {
				if (logger.isDebugEnabled()) logger.debug("*** Still unknown " + context + "::" + setting.getName());
			}
			SettingSchema schema = settingSchemas.get(setting.getName());
			if (schema != null) {
				Object cur = setting.getValue();
				Object def = schema.getDefaultValue();
				if ((cur == null && def == null) || (cur != null && (cur != def) && cur.equals(def))) {
					settings.put(setting.getName(), (Object)null);
					continue;
				}
			} else {
				if (logger.isDebugEnabled()) logger.debug("*** No schema for " + context + "::" + setting.getName());
			}
			setting.saveState(settings);
		}
	}

	/**
	 * @param schema
	 * @return
	 */
	private IProperty findOrRealize(SettingSchema schema) {
		IProperty prop = getRealSetting(schema.getName(), schema, null);
		syntheticSettings.remove(schema.getName());
		settingSchemas.put(schema.getName(), schema);
		return prop;
	}

	/**
	 * @param name
	 * @return
	 */
	private IProperty getRealSetting(String name, SettingSchema schema, IProperty actual) {
		IProperty prop = registeredSettings.get(name);
		if (prop instanceof SyntheticProperty) {
			if (logger.isDebugEnabled()) logger.debug("*** Replacing synthetic " + context + "::" + name + " with actual");
			SyntheticProperty synProp = (SyntheticProperty) prop;
			prop = schema != null ? schema.createSetting() : actual;
			prop.setValue(synProp.getValue());
			registeredSettings.put(name, prop);
		}

		return prop;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.settings.IStoredSettings#register(v9t9.common.settings.SettingDefinition)
	 */
	@Override
	public IProperty findOrCreate(SettingSchema schema) {
		IProperty prop = findOrRealize(schema);
		if (prop == null) {
			prop = schema.createSetting();
			if (logger.isDebugEnabled()) logger.debug("Creating: "+ context + "::" + prop.getName() + 
						" = " + format(prop.getValue()));
			registeredSettings.put(schema.getName(), prop);
		}
		return prop;
	}
	

	/* (non-Javadoc)
	 * @see v9t9.common.settings.IStoredSettings#register(v9t9.common.settings.SettingDefinition)
	 */
	@Override
	public IProperty findOrCreate(SettingSchema schema, Object defaultOverride) {
		IProperty prop = findOrRealize(schema);
		if (prop == null) {
			if (logger.isDebugEnabled()) logger.debug("Creating: "+ context + "::" + schema.getName());
			prop = schema.createSetting();
			prop.setValue(defaultOverride);
			registeredSettings.put(schema.getName(), prop);
		}
		return prop;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.settings.IStoredSettings#register(v9t9.common.settings.SettingDefinition)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IProperty> T findOrCreate(T defaultProperty) {
		IProperty prop = getRealSetting(defaultProperty.getName(), null, defaultProperty);
		if (prop == null) {
			if (logger.isDebugEnabled()) logger.debug("Creating default: "+ context + "::" + defaultProperty.getName());

			prop = defaultProperty;
			registeredSettings.put(defaultProperty.getName(), prop);
			settingSchemas.remove(defaultProperty.getName());
		} else if (prop instanceof SyntheticProperty) {
			
		}
		return (T) prop;
	}

	public void clearConfigVar(String configVar) {
		section.put(configVar, (String) null);
	}

	public ISettingSection getSettings() {
		if (section == null) {
			try {
				load();
			} catch (IOException e) {
				assert section != null;
			}
		}
		return section;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.settings.IStoredSettings#setConfigDirectory(java.lang.String)
	 */
	@Override
	public void setConfigDirectory(String configdir) {
		String oldDir = configDir;
		if (configdir == null) {
			configDir = System.getProperty("user.home") + File.separatorChar + ".v9t9j" + File.separatorChar;
		} else {
			configDir = configdir;
			if (!configDir.endsWith("" +  + File.separatorChar))
				configDir += File.separatorChar;
		}
		setDirty(oldDir != null && !configDir.equals(oldDir));
	}
	
	public String getConfigDirectory() {
		return configDir;
	}
	
	public String getConfigFilePath() {
		return getConfigDirectory() + getConfigFileName();
	}

	public ISettingSection getHistorySettings() {
		ISettingSection historySection = section.findOrAddSection(HISTORY);
		IProperty history = registeredSettings.get(HISTORY);
		if (history != null) {
			registeredSettings.remove(HISTORY);
			syntheticSettings.remove(HISTORY);
			setDirty(true);
		}
		return historySection;
	}
	public void setDirty(boolean b) {
		needsSave = b;
	}
	/* (non-Javadoc)
	 * @see v9t9.common.settings.IStoredSettings#isDirty()
	 */
	@Override
	public boolean isDirty() {
		return needsSave;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.settings.IStoredSettings#find(java.lang.String)
	 */
	@Override
	public IProperty find(String settingsName) {
		IProperty property = registeredSettings.get(settingsName);
		if (property != null) {
			if (syntheticSettings.containsKey(settingsName))
				return null;
		}
		return property;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.settings.IStoredSettings#remove(java.lang.String)
	 */
	@Override
	public void remove(String name) {
		if (registeredSettings.remove(name) != null) {
			if (logger.isDebugEnabled()) logger.debug("*** Removed setting " + context + "::" + name);
			setDirty(true);
		}
		settingSchemas.remove(name);
		syntheticSettings.remove(name);
		getSettings().put(name, (Object) null);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.settings.IStoredSettings#getAll()
	 */
	@Override
	public Map<IProperty, SettingSchema> getAll() {
		Map<IProperty, SettingSchema> ret = new HashMap<IProperty, SettingSchema>();
		for (Map.Entry<String, SettingSchema> entry : settingSchemas.entrySet()) {
			ret.put(registeredSettings.get(entry.getKey()), entry.getValue());
		}
		for (Map.Entry<String, SyntheticProperty> entry : syntheticSettings.entrySet()) {
			ret.put(syntheticSettings.get(entry.getKey()), null);
		}
		return ret;
	}
	
	private String format(Object val) {
		if (val instanceof Collection<?>)
			return StringUtils.catenate((Collection<?>) val, ", ");
		if (val instanceof Object[] || val instanceof String[])
			return StringUtils.catenate((Object[]) val, ", ");
		return String.valueOf(val);
	}
}