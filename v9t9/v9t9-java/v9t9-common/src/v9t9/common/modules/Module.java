/*
  Module.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.modules;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import v9t9.common.files.IPathFileLocator;
import v9t9.common.memory.MemoryEntryInfo;
import ejs.base.utils.TextUtils;

/**
 * @author ejs
 *
 */
public class Module implements IModule {

	private List<MemoryEntryInfo> entries = new ArrayList<MemoryEntryInfo>();
	private String name;
	private URI databaseURI;
	
	// not used for equality
	private List<String> keywords = new ArrayList<String>(1);
	private ModuleInfo info;
//	private String imagePath;
	
	public Module(URI uri, String name) {
		this.databaseURI = uri;
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((databaseURI == null) ? 0 : databaseURI.hashCode());
		result = prime * result + ((entries == null) ? 0 : entries.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Module other = (Module) obj;
		if (databaseURI == null) {
			if (other.databaseURI != null)
				return false;
		} else if (!databaseURI.equals(other.databaseURI))
			return false;
		if (entries == null) {
			if (other.entries != null)
				return false;
		} else if (!entries.equals(other.entries))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Module: " + name + " (" + entries.size() + " entries)\n" + entries;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.modules.IModule#getDatabaseURI()
	 */
	@Override
	public URI getDatabaseURI() {
		return databaseURI;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.modules.IModule#setDatabaseURI(java.net.URI)
	 */
	@Override
	public void setDatabaseURI(URI uri) {
		databaseURI = uri;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.modules.IModule#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.modules.IModule#getEntries()
	 */
	public MemoryEntryInfo[] getMemoryEntryInfos() {
		return (MemoryEntryInfo[]) entries.toArray(new MemoryEntryInfo[entries.size()]);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.modules.IModule#addMemoryEntryInfo(v9t9.common.memory.MemoryEntryInfo)
	 */
	@Override
	public void addMemoryEntryInfo(MemoryEntryInfo info) {
		entries.add(info);
	}
	
	public void setMemoryEntryInfos(List<MemoryEntryInfo> entries) {
		this.entries = entries;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.modules.IModule#getKeywords()
	 */
	@Override
	public List<String> getKeywords() {
		return keywords;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.modules.IModule#getInfo()
	 */
	@Override
	public ModuleInfo getInfo() {
		return info;
	}
	/* (non-Javadoc)
	 * @see v9t9.common.modules.IModule#getInfo(v9t9.common.modules.ModuleInfo)
	 */
	@Override
	public void setInfo(ModuleInfo info) {
		this.info = info;
		
	}
	
//	/* (non-Javadoc)
//	 * @see v9t9.common.modules.IModule#getImagePath()
//	 */
//	@Override
//	public String getImagePath() {
//		return imagePath;
//	}
//	/* (non-Javadoc)
//	 * @see v9t9.common.modules.IModule#setImagePath(java.lang.String)
//	 */
//	@Override
//	public void setImagePath(String imagePath) {
//		this.imagePath = imagePath;
//		
//	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.modules.IModule#getUsedFiles()
	 */
	@Override
	public Collection<File> getUsedFiles(IPathFileLocator locator) {
		Set<File> files = new TreeSet<File>();
		for (MemoryEntryInfo info : getMemoryEntryInfos()) {
			File file;
			file = getFileFrom(locator, info.getFilename());
			if (file != null)
				files.add(file);
			file = getFileFrom(locator, info.getFilename2());
			if (file != null)
				files.add(file);
		}
		return files;
	}

	/**
	 * @param locator
	 * @param filename2
	 * @return
	 */
	private File getFileFrom(IPathFileLocator locator, String filename) {
		if (TextUtils.isEmpty(filename))
			return null;
		URI uri = locator.findFile(filename);
		if (uri == null)
			return null;
		try {
			return new File(uri);
		} catch (IllegalArgumentException e) {
			if (uri.getScheme().equals("jar")) {
				uri = URI.create(uri.getSchemeSpecificPart());
				if ("file".equals(uri.getScheme())) {
					String ssp = uri.getSchemeSpecificPart();
					int idx = ssp.lastIndexOf('!');
					if (idx >= 0)
						ssp = ssp.substring(0, idx);
					return new File(ssp);
				}
			}
			return null;
		}
	}
}
