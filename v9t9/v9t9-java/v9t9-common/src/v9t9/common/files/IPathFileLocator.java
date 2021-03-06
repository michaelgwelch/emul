/*
  IPathFileLocator.java

  (c) 2011-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Map;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.memory.MemoryEntryInfo;

import ejs.base.properties.IProperty;
import ejs.base.utils.Pair;

/**
 * @author ejs
 *
 */
public interface IPathFileLocator {

	public class FileInfo {
		public final long length;
		public final String md5;
		public final URI uri;
		public FileInfo(URI uri, long length, String md5) {
			super();
			this.uri = uri;
			this.length = length;
			this.md5 = md5;
		}
		
	}
	
	void addReadOnlyPathProperty(IProperty property);

	void setReadWritePathProperty(IProperty property);

	void resetPathProperties();

	/**
	 * @param path
	 * @return
	 * @throws URISyntaxException 
	 */
	URI createURI(String path) throws URISyntaxException;

	/**
	 * Get all the active paths along the read-write and read-only
	 * path properties, with the read-write path (if any) first.
	 * @return
	 */
	URI[] getSearchURIs();

	void setConnectionTimeout(int timeoutMs);

	/**
	 * @return the timeoutMs
	 */
	int getTimeoutMs();

	/**
	 * Find an existing file along the search paths
	 * @param file
	 * @return <code>null</code> if no match is found
	 * @throws IllegalArgumentException if file contributes invalid URI segment
	 */
	URI findFile(String file);

	/**
	 * Find a file along the search paths with the given MD5 hash.
	 * @param md5
	 * @param offset TODO
	 * @param limit maximum number of bytes to read (or <= 0 for all)
	 * @return URI or <code>null</code> if no match is found
	 */
	URI findFileByMD5(String md5, int offset, int limit);

	/**
	 * Get the listing of entries in this URI 
	 * @param uri
	 * @return map of filenames to their info
	 */
	Map<String, FileInfo> getDirectoryListing(URI uri) throws IOException;

	/**
	 * Open a (new) input stream to the URI
	 * @param uri
	 * @return
	 * @throws IOException
	 */
	InputStream createInputStream(URI uri) throws IOException;

	/**
	 * Get the size of the content at the URI
	 * @param uri
	 * @return size in bytes
	 * @throws IOException
	 */
	int getContentLength(URI uri) throws IOException;

	/**
	 * Get the MD5 of the content, as a hex-encoded string
	 * @param uri
	 * @return String
	 */
	String getContentMD5(URI uri) throws IOException;
	
	/**
	 * Get the MD5 of the first given bytes of content, as a hex-encoded string
	 * @param uri
	 * @param offset TODO
	 * @param bytes number of bytes to consume (or <= 0 for all)
	 * @return String
	 */
	String getContentMD5(URI uri, int offset, int bytes) throws IOException;
	

	/**
	 * Attempt to open a file existing at the given URI 
	 * @param uri
	 * @return connection or <code>null</code>
	 */
	URLConnection openConnection(URI uri);

	/**
	 * Get a URI which supports writing the file.  This relies only on 
	 * the read/write path property.
	 * 
	 * @param file
	 * @return URI for file or <code>null</code> if none can be found
	 */
	URI getWriteURI(String file);

	/**
	 * Create an output stream for the given URI.
	 * Unlike a direct use of {@link URLConnection#getOutputStream()}, this
	 * will handle file: schemas.
	 * @param uri
	 * @return
	 */
	OutputStream createOutputStream(URI uri) throws IOException;

	/**
	 * @param uri
	 * @return
	 */
	boolean exists(URI uri);

	/**
	 * Get all the properties searched for files
	 * @return
	 */
	IProperty[] getSearchPathProperties();
	
	/**
	 * Resolve string against the scheme-specific part of the URI,
	 * for example, to descend into a jar file URI.
	 * @param uri
	 * @param string
	 * @return new URI
	 * @throw {@link IllegalArgumentException} if string is invalid
	 */
	URI resolveInsideURI(URI uri, String string);

	/**
	 * Get the non-filename and filename portion of the URI
	 * @param uri
	 * @return pair consisting of the URI up to the filename, 
	 * and the filename (or empty string if this represents a directory)
	 */
	Pair<URI, String> splitFileName(URI uri);

	/**
	 * Find a file according to the default content or user-defined filename
	 * (only works for main file, not banked file)
	 * @param info
	 * @param storedInfo
	 * @return URI or null
	 */
	URI findFile(ISettingsHandler settings, MemoryEntryInfo info);

	/**
	 * Get the modification date of the content at the URI
	 * @param uri
	 * @return timestamp in Java ms
	 * @throws IOException
	 */
	long getLastModified(URI uri) throws IOException;
	
	public interface IPathChangeListener {
		void pathsChanged();
	}

	void addListener(IPathChangeListener listener);
	void removeListener(IPathChangeListener listener);


}