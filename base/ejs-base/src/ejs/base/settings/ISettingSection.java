/*
  ISettingSection.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package ejs.base.settings;

/**
 * @author ejs
 *
 */
public interface ISettingSection extends Iterable<ISettingSection.SettingEntry> {
	enum Type {
		/** ISettingStorage */
		Section("section"),
		Int("int"),
		Double("double"),
		Boolean("bool"),
		String("str"),
		/** String[] */
		StringArray("strs"),
		Object("object");
		
		private final java.lang.String xmlName;

		private Type(String xmlName) {
			this.xmlName = xmlName;
		}
		
		public java.lang.String getXmlName() {
			return xmlName;
		}
	}
	
	static class SettingEntry {
		public Type type;
		public String name;
		public Object value;
	}

	String getName();
	
	/** Get the existing section with this name, or <code>null</code> */ 
	ISettingSection getSection(String sectionName);
	/** Add a section with the given name, <b>erasing</b> any existing section */
	ISettingSection addSection(String sectionName);
    /** Find an existing section with the given name, or create and add a new section */
    ISettingSection findOrAddSection(String sectionName);
    String[] getSectionNames();
    ISettingSection[] getSections();
    
    String[] getSettingNames();

	String get(String name);
	int getInt(String name);
	boolean getBoolean(String name);
	double getDouble(String name);
	String[] getArray(String name);
	Object getObject(String name);
	
	void put(String name, String value);
	void put(String name, boolean value);
	void put(String name, int value);
	void put(String name, double value);
	void put(String name, String[] array);
	void put(String name, Object value);

	/**
	 * @param entry
	 */
	void addEntry(SettingEntry entry);

	/**
	 * @param value
	 */
	void mergeFrom(ISettingSection other);
}
