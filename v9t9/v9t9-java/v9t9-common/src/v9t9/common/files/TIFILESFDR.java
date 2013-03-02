/*
  TIFILESFDR.java

  (c) 2008-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class TIFILESFDR extends FDR {

    private byte[] sig;

    private byte[] unused;

	private String name;

    /**
    u8          sig[8];     // '\007TIFILES'
    u16         secsused;   // [big-endian]:  # sectors in file
    u8          flags;      // filetype flags
    u8          recspersec; // # records per sector, 
                               256/reclen for FIXED,
                               255/(reclen+1) for VAR,
                               0 for program 
    u8          byteoffs;   // last byte used in file 
                               (0 = no last empty sector)
    u8          reclen;     // record length, 0 for program
    u16         numrecs;    // [little-endian]:  # records for FIXED file,
                               # sectors for VARIABLE file,
                               0 for program
    u8          unused[112];    // zero 
     */

    /**
	 * 
	 */
	public TIFILESFDR() {
		super(128);
	}
	
    public static final byte[] SIGNATURE = { 7, 'T', 'I', 'F', 'I', 'L', 'E', 'S' };
    
    public static FDR readFDR(File file) throws IOException, InvalidFDRException {
        TIFILESFDR fdr = new TIFILESFDR();
        
        FileInputStream stream = new FileInputStream(file);
        try {
        	fdr.name = file.getName().toUpperCase();
	        fdr.sig = new byte[8];
	        stream.read(fdr.sig, 0, 8);
	        if (!Arrays.equals(fdr.sig, SIGNATURE)) {
				throw new InvalidFDRException("No TIFILES signature found");
			}
	        fdr.secsused = (stream.read() << 8 | stream.read());
	        fdr.flags = stream.read();
	        fdr.recspersec = stream.read();
	        fdr.byteoffs = stream.read();
	        fdr.reclen = stream.read();
	        fdr.numrecs = (stream.read() | stream.read() << 8);
	        fdr.unused = new byte[112];
	        stream.read(fdr.unused, 0, 112);
        } finally {
        	stream.close();
        }
        return fdr;
    }
    
    public void writeFDR(File file) throws IOException {
    	file.setWritable(true);
    	
    	RandomAccessFile raf = new RandomAccessFile(file, "rw");
    	raf.seek(0);
    	
    	raf.write(SIGNATURE);
    	raf.write(secsused >> 8);
    	raf.write(secsused & 0xff);
    	raf.write(flags);
    	raf.write(recspersec);
    	raf.write(byteoffs);
    	raf.write(reclen);
    	raf.write(numrecs & 0xff);
    	raf.write(numrecs >> 8);
        raf.write(unused);
        
        raf.close();
        
        file.setWritable(!isReadOnly());
    }
    
    /* (non-Javadoc)
     * @see v9t9.common.files.IFDRInfo#getFileName()
     */
    @Override
    public String getFileName() {
    	return name;
    }
    
    @Override
    public void setFileName(String name) throws IOException {
    	this.name = name;
    }
    
    /* (non-Javadoc)
     * @see v9t9.common.files.FDR#fetchContentSectors()
     */
    @Override
    protected int[] fetchContentSectors() {
    	int[] secs = new int[getSectorsUsed()];
    	for (int i = 0; i < secs.length; i++) {
    		secs[i] = i * 256;
    	}
    	return secs;
    }
}