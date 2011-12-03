/**
 * 
 */
package v9t9.emulator.hardware.memory;


/** Enhanced console RAM */
public class EnhancedRamArea extends ConsoleMemoryArea {

    public EnhancedRamArea(int latency, int size) {
    	super(latency);
    	
        memory = new short[size/2];
        read = memory;
        write = memory;
    }
}