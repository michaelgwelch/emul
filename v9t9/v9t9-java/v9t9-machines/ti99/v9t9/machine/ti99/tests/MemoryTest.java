/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Dec 15, 2004
 *
 */
package v9t9.machine.ti99.tests;

import java.util.Random;

import ejs.base.properties.IProperty;

import junit.framework.TestCase;
import v9t9.common.client.ISettingsHandler;
import v9t9.common.memory.IMemoryDomain;
import v9t9.common.settings.BasicSettingsHandler;
import v9t9.machine.ti99.machine.TI994A;
import v9t9.machine.ti99.memory.ConsoleRamArea;
import v9t9.machine.ti99.memory.ExpRamArea;

/**
 * @author ejs
 */
public class MemoryTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MemoryTest.class);
    }

    private ISettingsHandler settings;
    private TI994A machine;
    private IMemoryDomain CPU;

    /**
     * Constructor for MemoryTest.
     * 
     * @param arg0
     */
    public MemoryTest(String arg0) {
        super(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
	protected void setUp() throws Exception {
        super.setUp();
        settings = new BasicSettingsHandler();
        machine = new TI994A(settings);
        CPU = machine.getConsole();
    }

    public void testReads() {

        int i;
        for (i = 0; i < 65536; i++) {
            CPU.flatReadByte(i);
        }
        for (i = 0x8400; i < 0xa000; i++) {
            CPU.readByte(i);
        }
        for (i = 0x8400; i < 0xa000; i++) {
            CPU.writeByte(i, (byte) 0);
        }

        IProperty expRam = settings.get(ExpRamArea.settingExpRam);
        expRam.setBoolean(true);
        IProperty enhRam = settings.get(ConsoleRamArea.settingEnhRam);
        enhRam.setBoolean(true);

        int firstRAM = 0;
        Random rand = new Random();
        rand.setSeed(0);
        for (i = 0; i < 65536; i++) {
            if (CPU.hasRamAccess(i)) {
                if (firstRAM == 0) {
					firstRAM = i;
				}
                byte byt = (byte) rand.nextInt();
                //System.out.println("setting " + i + " to " + byt);
                CPU.writeByte(i, byt);

            }
        }
        rand.setSeed(0);
        for (i = 0; i < 65536; i++) {
            if (CPU.hasRamAccess(i)) {
                byte byt = (byte) rand.nextInt();
                byte red = CPU.readByte(i);

                if (byt != red) {
					fail("memory reread failed at " + Integer.toHexString(i)
                            + " byt=" + byt + ", red=" + red);
				}
            }
        }

        /* verify byte ordering */
        CPU.writeWord(firstRAM, (short) 0x1234);
        assertEquals(CPU.readByte(firstRAM), 0x12);
        assertEquals(CPU.readByte(firstRAM + 1), 0x34);
        assertEquals(CPU.readWord(firstRAM), 0x1234);
        /* and off address munging */
        assertEquals(CPU.readWord(firstRAM + 1), 0x1234);

        /* turn off expansion RAM, shouldn't get anything from it... */
        expRam.setBoolean(false);
        for (i = 0x2000; i < 0x4000; i++) {
            byte red = CPU.readByte(i);
            if (red != 0) {
				fail("memory read failed at " + Integer.toHexString(i));
			}
        }
        for (i = 0xA000; i < 0x10000; i++) {
            byte red = CPU.readByte(i);
            if (red != 0) {
				fail("memory read failed at " + Integer.toHexString(i));
			}
        }

        /* without enhanced ram, 0x8000 mirrors 0x8100 through 0x8300 */
        enhRam.setBoolean(false);
        rand.setSeed(0);
        for (i = 0x8000; i < 0x8400; i++) {
            byte byt = (byte) rand.nextInt();
            //System.out.println("setting " + i + " to " + byt);
            CPU.writeByte(i, byt);
        }

        for (i = 0x8000; i < 0x8100; i++) {
            byte byt0 = CPU.readByte(i);
            byte byt1 = CPU.readByte(i + 0x100);
            byte byt2 = CPU.readByte(i + 0x200);
            byte byt3 = CPU.readByte(i + 0x300);
            if (byt0 != byt1 || byt1 != byt2 || byt2 != byt3) {
				fail("memory reread failed at " + Integer.toHexString(i));
			}
        }

        for (i = 0; i < 65536; i++) {
            machine.getGplMemoryDomain().readByte(i);
        }
        for (i = 0; i < 65536; i++) {
            machine.getVdpMemoryDomain().readByte(i);
        }
        for (i = 0; i < 65536; i++) {
            machine.getSpeechMemoryDomain().readByte(i);
        }
    }
}