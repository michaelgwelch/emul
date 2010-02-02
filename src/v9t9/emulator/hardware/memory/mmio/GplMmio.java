/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Dec 18, 2004
 *
 */
package v9t9.emulator.hardware.memory.mmio;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.ejs.coffee.core.utils.PrefUtils;

import v9t9.emulator.Machine.ConsoleMmioReader;
import v9t9.emulator.Machine.ConsoleMmioWriter;
import v9t9.engine.memory.MemoryDomain;

/** GPL chip entry
 * @author ejs
 */
public class GplMmio implements ConsoleMmioReader, ConsoleMmioWriter {

    private MemoryDomain memory;
    
    short gromaddr;
    boolean gromaddrflag;

    /**
     * @param machine
     */
    public GplMmio(MemoryDomain memory) {
        if (memory == null) {
			throw new IllegalArgumentException();
		}
        this.memory = memory;
     }

    /*	GROM has a strange banking scheme where the upper portion
	of the address does not change when incremented;
	this acts like an 8K bank. */
    private short getNextAddr(short addr) {
        return (short) (addr+1 & 0x1fff | gromaddr & 0xe000);
    }
    
    public int getAddr() {
        return gromaddr;
    }
    
    public byte getAddrByte() {
        return (byte) (getNextAddr(gromaddr) >> 8);        
    }
    
    public void setAddr(short addr) {
        gromaddr = addr;
    }

    public boolean addrIsComplete() {
        return !gromaddrflag;
    }
    
    /**
     * @see v9t9.engine.memory.Memory.ConsoleMmioReader#read
     */
    public byte read(int addr) {
    	byte ret;
    	short temp;
    	
    	if ((addr & 2) != 0) {
    	    /* >9802, address read */
    	    temp = getNextAddr(gromaddr);
    	    ret = getAddrByte();
    	    gromaddr = (short) (temp << 8);
    	    gromaddrflag = !gromaddrflag;
    	} else {
    	    /* >9800, memory read */
    	    gromaddrflag = false;
    	    ret = memory.readByte(gromaddr);
    	    gromaddr = getNextAddr(gromaddr);
    	}
    	return ret;
    }

    /**
     * @see v9t9.engine.memory.Memory.ConsoleMmioWriter#write 
     */
    public void write(int addr, byte val) {
    	if ((addr & 2) != 0) {				
    	    /* >9C02, address write */
    	    
    		gromaddr = (short) (gromaddr << 8 | val & 0xff);
    		gromaddrflag = !gromaddrflag;
    	} else {					
    	    /* >9C00, data write */
    		gromaddrflag = false;

    		memory.writeByte(gromaddr, val);
    		gromaddr = getNextAddr(gromaddr);
    	}    
    }

	public void loadState(IDialogSettings section) {
		gromaddr = (short) PrefUtils.readSavedInt(section, "Addr");
		gromaddrflag = PrefUtils.readSavedBoolean(section, "AddrFlag");
	}

	public void saveState(IDialogSettings section) {
		section.put("Addr", gromaddr);
		section.put("AddrFlag", gromaddrflag);
	}
}
