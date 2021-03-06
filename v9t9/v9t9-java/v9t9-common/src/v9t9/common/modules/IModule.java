/*
  IModule.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.modules;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import v9t9.common.files.IPathFileLocator;
import v9t9.common.memory.MemoryEntryInfo;

/**
 * @author ejs
 *
 */
public interface IModule {
	URI getDatabaseURI();
	void setDatabaseURI(URI uri);
	
	String getName();
	void setName(String name);
	
//	String getImagePath();
//	void setImagePath(String imagePath);
	
	MemoryEntryInfo[] getMemoryEntryInfos();
	void addMemoryEntryInfo(MemoryEntryInfo info);
	
	List<String> getKeywords();

	ModuleInfo getInfo();
	void setInfo(ModuleInfo info);

	Collection<File> getUsedFiles(IPathFileLocator locator);
}
