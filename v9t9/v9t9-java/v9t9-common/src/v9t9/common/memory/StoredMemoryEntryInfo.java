/*
  StoredMemoryEntryInfo.java

  (c) 2011-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.memory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.files.IPathFileLocator;

/**
 * @author ejs
 *
 */
public class StoredMemoryEntryInfo {
	public final MemoryEntryInfo info;
	
	public final IMemory memory;
	public final ISettingsHandler settings;
	public final IPathFileLocator locator;
	/** current URI -- may be <code>null</code> */
	public final URI uri;
	public String md5;
	public final String fileName;
	public final String name;
	public final int fileoffs;
	public final int size;


	
	private StoredMemoryEntryInfo(MemoryEntryInfo info, ISettingsHandler settings, IMemory memory,
			IPathFileLocator locator, URI uri, String filePath, String md5, String name, int fileoffs, int size) {
		this.info = info;
		this.settings = settings;
		this.memory = memory;
		this.locator = locator;
		this.uri = uri;
		this.fileName = filePath;
		this.md5 = md5;
		this.name = name;
		this.fileoffs = fileoffs;
		this.size = size;
	}
	
	public static StoredMemoryEntryInfo createStoredMemoryEntryInfo(
			IPathFileLocator locator, ISettingsHandler settings, IMemory memory,
			MemoryEntryInfo info) throws IOException {
		
		int size = info.getSize();
		if (size == 0)
			throw new IOException("size is zero");

		String name = info.getName();
		String filename = info.getResolvedFilename(settings);
		int fileoffs = info.getOffset();
		
        boolean isStored = info.isStored();
        
		if (isStored && size <= 0
                || isStored && fileoffs != 0
                ) {
			throw new IOException("size or offset is incompatible with the memory model for '" + name + "')");
		}
        
		int filesize = size;
		
		URI uri = null;
    	if (!info.isStored()) {
    		uri = locator.findFile(settings, info);
    		if (uri == null) {
    			throw new FileNotFoundException(filename);
    		}
    		
    		filesize = locator.getContentLength(uri);
    		if (info.getSize() > 0) {
    			if (filesize < info.getSize()) {
    				throw new IOException("file '" + filename + "'found for '" + name + "' is not the expected size (" + info.getSize() +" bytes); found " + filesize + " bytes at " + uri);
    			}
    		} else {
    			if (!info.isBanked() && filesize > -info.getSize()) {
    				throw new IOException("file '" + filename + "'found for '" + name + "' is too large (>= " + -info.getSize() +" bytes); found " + filesize + " bytes at " + uri);
    			}
    		}
    	} else {
    		uri = locator.getWriteURI(filename);
    		if (info.getSize() < 0)
    			throw new IOException("negative size not allowed for stored files (in file '" + filename +"' for '" + name + "')");
    	}
		String realMD5 = locator.getContentMD5(uri,  
				info.getFileMd5Offset(),
				info.getFileMd5Limit() != 0 ? info.getFileMd5Limit() : info.getSize());
    	
        return new StoredMemoryEntryInfo(info, settings, memory, locator, 
        		uri, filename, realMD5, name, fileoffs, filesize);
	}

	/**
	 * Create the memory info reflecting this stored memory
	 * (overriding bank-ignorant basic MemoryEntryInfo)
	 * @return
	 */
	public MemoryEntryInfo createMemoryEntryInfo() {
		MemoryEntryInfo info = new MemoryEntryInfo(new HashMap<String, Object>(this.info.getProperties()));
		info.getProperties().put(MemoryEntryInfo.FILE_MD5, md5);
		info.getProperties().put(MemoryEntryInfo.FILENAME, fileName);
		info.getProperties().put(MemoryEntryInfo.OFFSET, fileoffs);
		return info;
	}
}
