/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Feb 21, 2006
 *
 */
package v9t9.engine.files;

import java.io.File;
import java.io.IOException;

import org.ejs.coffee.core.utils.HexUtils;


/**
 * File descriptor record, as seen in the index sector for files 
 * in the TI Disk Controller.
 * @author ejs
 */
public abstract class FDR implements IFDRInfo {
	public static final int ff_variable = 0x80;
	public static final int ff_backup = 0x10;       // set by MYARC HD
	public static final int ff_protected = 0x8;
	public static final int ff_internal = 0x2;
	public static final int ff_program = 0x1;
	public static final int FF_VALID_FLAGS = ff_variable|ff_backup|ff_protected|ff_internal|ff_program;
	    
	/** number of records: 16-bit */
    protected int numrecs;
    /** length of record: 8-bit */
    protected int reclen;
    /** offset of last record in last sector: 8-bit */
    protected int byteoffs;
    /** number of data sectors used: 16-bit */
    protected int secsused;
    /** number of records per sector: 8-bit */
    protected int recspersec;
    /** flag bits: 8-bit */
    protected int flags;
    
    protected final int fdrsize;
    
    /**
	 * @param fdrsize2
	 */
	public FDR(int fdrsize) {
		this.fdrsize = fdrsize;
	}

	/** get size of FDR */
    public int getFDRSize() {
        return fdrsize;
    }
    
    /** filetype flags */
    public int getFlags() {
        return flags;
    }
    /**
	 * @param flags the flags to set
	 */
	public void setFlags(int flags) {
		if ((flags & 0xff) != flags)
			throw new IllegalArgumentException();

		this.flags = flags;
	}
	
    /** # records per sector, 
    256/reclen for FIXED,
    255/(reclen+1) for VAR,
    0 for program
    */
    public int getRecordsPerSector() {
        return recspersec;
    }
    /**
     * Set # records per sector,
     * 256/reclen for FIXED,
     * 255/(reclen+1) for VAR,
     * 0 for program
	 * @param recspersec the recspersec to set
	 */
	public void setRecordsPerSector(int recspersec) {
		if (recspersec < 0 || recspersec >= 256)
			throw new IllegalArgumentException();
		this.recspersec = recspersec;
	}
    
    /** Get # sectors in file */
    public int getSectorsUsed() {
        return secsused;
    }
    /**
     * Set # sectors in file
	 * @param secsused the secsused to set
	 */
	public void setSectorsUsed(int secsused) {
		if (secsused < 0 || secsused >= 65536)
			throw new IllegalArgumentException();

		this.secsused = secsused;
	}
    
    /** Get last byte used in file 
    (0 = no last empty sector) */
    public int getByteOffset() {
        return byteoffs;
    }
    /**
     * Set the last byte used in file
     * (0 = no last empty sector)
	 * @param byteoffs the byteoffs to set
	 */
	public void setByteOffset(int byteoffs) {
		if (byteoffs < 0 || byteoffs >= 256)
			throw new IllegalArgumentException();

		this.byteoffs = byteoffs;
	}
    
    /** Get record length, 0 for program */
    public int getRecordLength() {
        return reclen;
    }
    
    /**
     * Set the record length, 0 for program
	 * @param reclen the reclen to set
	 */
	public void setRecordLength(int reclen) {
		if (reclen < 0 || reclen >= 256)
			throw new IllegalArgumentException();

		this.reclen = reclen;
	}
    
    /** Get # records for FIXED file,
    # sectors for VARIABLE file,
    0 for program */
    public int getNumberRecords() {
        return numrecs;
    }
    
    /**
     * Set the # of records for FIXED file,
     * # sectors for VARIABLE file,
     * 0 for program
	 * @param numrecs the numrecs to set
	 */
	public void setNumberRecords(int numrecs) {
		if (numrecs < 0 || numrecs >= 65536)
			throw new IllegalArgumentException();

		this.numrecs = numrecs;
	}

    public int getFileSize() {
    	int full;
        if ((flags & FDR.ff_variable + FDR.ff_program) != 0) {
        	full = secsused * 256;
	        if (byteoffs != 0) {
				full = full - 256 + byteoffs;
			}
        } else {
        	full = 0;
        	if (recspersec > 0) {
        		full = (numrecs / recspersec) * 256; 
        		full += (numrecs % recspersec) * reclen;
        	}
        }
        return full;
    }
    
    /**
     * Set the secsused and byteoffs for the given total file size
     * @param size
     * @throws IOException
     */
    public void setFileSize(int size) throws IOException {
    	if (size >= 256 * 65536)
    		throw new IOException("File size too big: " + size);
    	
    	secsused = ((size + 255) / 256);
    	if ((flags & FDR.ff_variable + FDR.ff_program) != 0) {
    		byteoffs = (size & 0xff);
    	} else {
    		if (recspersec > 0 && reclen > 0) {
    			numrecs = (size / 256) * recspersec + (size & 0xff) / reclen;
    		}
    	}
    }

	/**
	 * @return
	 */
	public boolean isReadOnly() {
		return (flags & ff_protected) != 0;
	}
    
	public abstract void writeFDR(File file) throws IOException;

	/** Validate the FDR against the file which provided it */
	public void validate(File file) throws InvalidFDRException {
        // check for invalid filetype flags
        if ((flags & ~FF_VALID_FLAGS) != 0) {
            throw new InvalidFDRException("Invalid FDR flags " + HexUtils.toHex2(flags) + " for " + file);
        }

        long filesize = file.length();
        
        // check for invalid file size:
        // do not allow file to be more than one sector larger than FDR says,
        // but allow it to be up to 64 sectors smaller:  
        // this is a concession for files copied with "direct output to file", 
        // which must write FDR changes before writing data.
        if (secsused < (filesize - fdrsize) / 256 - 1
                 || secsused > (filesize - fdrsize) / 256 + 64) {
                throw new InvalidFDRException("Invalid number of sectors " + secsused + " for data size " + (filesize - fdrsize) +": " + file);
        }

        // fixed files have 256/reclen records per sector
        if ((flags & ff_program) == 0
            && (flags & ff_variable) == 0) {
            if (reclen == 0 ||
                256 / reclen != recspersec) 
            {
                throw new InvalidFDRException("record length "+reclen+" / records per sector "+recspersec+" invalid for FIXED file: " + file);
            }
        }
        
        // variable files have 255/(reclen+1) records per sector
        if ((flags & ff_program) == 0) {
            if (reclen == 0 || 
                255 / (reclen + 1) != recspersec 
                 // known problem that older v9t9s used this calculation
                && 256 / reclen != recspersec)
            {
                throw new InvalidFDRException("record length "+reclen+" / records per sector "+recspersec+" invalid for VARIABLE file: " + file);
            }
        } else {
	        // program files have 0
	        if (reclen != 0 && recspersec != 0) {
	            throw new InvalidFDRException("record length "+reclen+" / records per sector "+recspersec+" invalid for PROGRAM file: " + file);
	        }
        }
	}
    

    /**
     * Set the filename -- may be a noop!
     */
    abstract public void setFileName(String name) throws IOException; 
}
