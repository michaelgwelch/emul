/*
  CatalogEntry.java

  (c) 2011-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.files;


/**
 * @author ejs
 *
 */
public class CatalogEntry {
	static byte drcTrans[][] = new byte[][] { 
		{0, 1}, 
		{FDR.ff_program, 5},
		{FDR.ff_internal, 3}, 
		{(byte) FDR.ff_variable, 2},
		{(byte) (FDR.ff_variable + FDR.ff_internal), 4}
	};


	public final String fileName;
	public final int secs;
	public final String type;
	public final int recordLength;
	public final int typeCode;
	public final boolean isProtected;


	private EmulatedFile file;

	/**
	 * @param fileName
	 * @param sz
	 * @param type
	 * @param recordLength
	 */
	public CatalogEntry(String fileName, EmulatedFile file) {
		this.fileName = fileName;
		this.file = file;
		this.secs = file.getSectorsUsed() + 1;
		int flags = file.getFlags();
		this.isProtected = (flags & FDR.ff_protected) != 0;
		
		String ttype = "???";
		if ((flags & FDR.ff_program) != 0)
			ttype = "PROGRAM";
		else {
			if ((flags & FDR.ff_internal) != 0)
				ttype = "INT";
			else
				ttype = "DIS";
			if ((flags & FDR.ff_variable) != 0)
				ttype += "/VAR";
			else
				ttype += "/FIX";
		}
		this.type = ttype;
		
		int idx;
		int code = 0;
		for (idx = 0; idx < drcTrans.length; idx++)
			if (drcTrans[idx][0] ==
				(flags & (FDR.ff_internal | FDR.ff_program | FDR.ff_variable))) {
				code = drcTrans[idx][1];
				break;
			}
		// no match == program
		if (idx >= drcTrans.length) {
			code = 1;
		}
		this.typeCode = code;
		
		this.recordLength = file.getRecordLength();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + (isProtected ? 1231 : 1237);
		result = prime * result + recordLength;
		result = prime * result + secs;
		result = prime * result + typeCode;
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
		CatalogEntry other = (CatalogEntry) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (isProtected != other.isProtected)
			return false;
		if (recordLength != other.recordLength)
			return false;
		if (secs != other.secs)
			return false;
		if (typeCode != other.typeCode)
			return false;
		return true;
	}

	/**
	 * @return the file
	 */
	public EmulatedFile getFile() {
		return file;
	}
	
}