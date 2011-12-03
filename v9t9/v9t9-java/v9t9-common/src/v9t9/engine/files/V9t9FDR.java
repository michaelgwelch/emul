/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Feb 21, 2006
 *
 */
package v9t9.engine.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class V9t9FDR extends FDR {

    public static final int FDRSIZE = 128;
    
    /** 10 bytes, padded with spaces */
    private final byte[] filename = new byte[10];
    
    private final byte[] res10 = new byte[2];
    private final byte[] dcpb = new byte[100];
    private final byte[] rec20 = new byte[8];

    /**
    char        filenam[10];// filename, padded with spaces
    u8          res10[2];   // reserved 
    u8          flags;      // filetype flags 
    u8          recspersec; // # records per sector, 
                               256/reclen for FIXED,
                               255/(reclen+1) for VAR,
                               0 for program 
    u16         secsused;   // [big-endian]:  # sectors in file 
    u8          byteoffs;   // last byte used in file 
                                   (0 = no last empty sector) 
    u8          reclen;     // record length, 0 for program 
    u16         numrecs;    // [little-endian]:  # records for FIXED file,
                               # sectors for VARIABLE file,
                               0 for program 
    u8          rec20[8];   // reserved 
    u8          dcpb[100];  // sector layout of file, ignored for v9t9 
     */

    public V9t9FDR() {
    	super(FDRSIZE);
    }
    public static V9t9FDR readFDR(File file) throws IOException, InvalidFDRException {
        V9t9FDR fdr = new V9t9FDR();
        FileInputStream stream = new FileInputStream(file);
        try {
	        stream.read(fdr.filename);
	        stream.read(fdr.res10);
	        fdr.flags = stream.read();
	        fdr.recspersec = stream.read();
	        fdr.secsused = (stream.read() << 8) | stream.read();
	        fdr.byteoffs = stream.read();
	        fdr.reclen = stream.read();
	        fdr.numrecs = stream.read() | (stream.read() << 8);
	        stream.read(fdr.rec20);
	        stream.read(fdr.dcpb);
        } finally { 
        	stream.close();
        }
        fdr.validate(file);
        
        return fdr;
    }
    
    public static V9t9FDR createFDR(byte[] data, int offset) {
        V9t9FDR fdr = new V9t9FDR();
        
    	System.arraycopy(data, offset, fdr.filename, 0, fdr.filename.length);
    	System.arraycopy(data, offset + 0xA, fdr.res10, 0, fdr.res10.length);
    	fdr.flags = data[offset + 0xc] & 0xff;
        fdr.recspersec = data[offset + 0xd] & 0xff;
        fdr.secsused = ((data[offset + 0xe] & 0xff) << 8) | (data[offset + 0xf] & 0xff);
        fdr.byteoffs = data[offset + 0x10] & 0xff;
        fdr.reclen = data[offset + 0x11] & 0xff;
        fdr.numrecs = (data[offset + 0x12] & 0xff) | ((data[offset + 0x13] & 0xff) << 8);
        System.arraycopy(data, offset + 0x14, fdr.rec20, 0, fdr.rec20.length);
        System.arraycopy(data, offset + 0x1C, fdr.dcpb, 0, fdr.dcpb.length);
        
        return fdr;
    }

    /**
     * Set the filename
     */
    public void setFileName(String name) throws IOException {
    	if (name.length() > 10)
    		throw new IOException("Name too long: " + name);
    	for (int i = 0; i < filename.length; i++) {
    		char ch = ' ';
    		if (i < name.length())
    			ch = name.charAt(i);
    		filename[i] = (byte) ch;
    	}
    }

    public void writeFDR(File file) throws IOException {
    	file.setWritable(true);
    	
    	RandomAccessFile raf = new RandomAccessFile(file, "rw");
    	raf.seek(0);
    	
    	raf.write(filename);
    	raf.write(res10);
    	raf.write(flags);
    	raf.write(recspersec);
    	raf.write(secsused >> 8);
    	raf.write(secsused & 0xff);
    	raf.write(byteoffs);
    	raf.write(reclen);
    	raf.write(numrecs & 0xff);
    	raf.write(numrecs >> 8);
    	raf.write(rec20);
        raf.write(dcpb);
        
        raf.close();
        
        file.setWritable(!isReadOnly());
    }
    
	public String getFileName() {
		StringBuilder builder = new StringBuilder();
		int len = 0;
    	for (int i = 0; i < filename.length; i++) {
    		char ch = (char) filename[i];
    		if (ch != ' ')
    			len = i;
    		builder.append(ch);
    	}
    	builder.setLength(len + 1);
		return builder.toString();
	}
}