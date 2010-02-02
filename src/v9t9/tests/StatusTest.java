/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Dec 18, 2004
 *
 */
package v9t9.tests;

import junit.framework.TestCase;
import v9t9.engine.cpu.Status;

/**
 * @author ejs
 */
public class StatusTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(StatusTest.class);
    }
    public void testIt() {
        Status status = new Status();

        status.expand(Status.ST_L);	/* L> */
        assertTrue(status.isH());
        assertTrue(status.isHE());
        assertFalse(status.isL());
        assertFalse(status.isEQ());
        
        status.expand(Status.ST_A); /* A> */
        assertTrue(status.isGT());
        assertFalse(status.isEQ());
        assertFalse(status.isLT());
        
        status.expand(Status.ST_E);
        assertTrue(status.isEQ());
        assertTrue(status.isHE());
        assertTrue(status.isLE());
        assertFalse(status.isNE());
        
        status.set_LAE((short)0);
        assertTrue(status.isHE());
        assertTrue(status.isEQ());
        assertTrue(status.isLE());
        assertFalse(status.isNE());
        assertFalse(status.isGT());
        assertFalse(status.isLT());

        status.set_LAE((short)1);
        assertTrue(status.isHE());
        assertTrue(status.isNE());
        assertFalse(status.isLE());
        assertFalse(status.isEQ());
        assertFalse(status.isL());
        assertFalse(status.isLT());

        status.set_LAE((short)-1);
        assertTrue(status.isHE());
        assertTrue(status.isNE());
        assertTrue(status.isLT());
        assertFalse(status.isLE());
        assertFalse(status.isEQ());
        assertFalse(status.isL());
        
        status.set_E(false);
        assertTrue(status.isNE());
        status.set_E(true);
        assertTrue(status.isEQ());

        /* overflow */
        status.set_LAEO((short)0x0);
        assertTrue(!status.isO());
        status.set_LAEO((short)-1);
        assertTrue(!status.isO());
        status.set_LAEO((short)0x8000);
        assertTrue(status.isO());

        /* parity */
        status.set_BYTE_LAEP((byte)0x0);
        assertTrue(!status.isP());
        status.set_BYTE_LAEP((byte)0x1);
        assertTrue(status.isP());
        status.set_BYTE_LAEP((byte)0xff);
        assertTrue(!status.isP());
        status.set_BYTE_LAEP((byte)0xab);
        assertTrue(status.isP());

        /* word op doesn't reset P */
        status.set_LAE((short)0x0);
        assertTrue(status.isP());

        /* carries */
        status.set_SHIFT_LEFT_CO((short)0x4000, (short)1);
        assertTrue(!status.isC());
        assertTrue(status.isO());
        status.set_SHIFT_LEFT_CO((short)0x8000, (short)1);
        assertTrue(status.isC());
        assertTrue(status.isO());

        status.set_SHIFT_LEFT_CO((short)0x4000, (short)2);
        assertTrue(status.isC());
        assertTrue(!status.isO());
        status.set_SHIFT_LEFT_CO((short)0x2000, (short)2);
        assertTrue(!status.isC());
        assertTrue(status.isO());

        status.set_SHIFT_RIGHT_C((short)0x1, (short)1);
        assertTrue(status.isC());
        status.set_SHIFT_RIGHT_C((short)0x2, (short)1);
        assertTrue(!status.isC());
        status.set_SHIFT_RIGHT_C((short)0x2, (short)2);
        assertTrue(status.isC());
        status.set_SHIFT_RIGHT_C((short)0x4, (short)2);
        assertTrue(!status.isC());
        
        /* adds */
        status.set_ADD_LAECO((short)0x1, (short)0x2);
        assertTrue(!status.isC());
        assertTrue(!status.isO());
        assertTrue(status.isNE());
        assertTrue(status.isGT());

        status.set_ADD_LAECO((short)0x7fff, (short)0x1);
        assertTrue(!status.isC());
        assertTrue(status.isO());

        status.set_ADD_LAECO((short)0x1, (short)0xffff);
        assertTrue(status.isC());
        assertTrue(!status.isO());

        status.set_ADD_LAECO((short)-0x1, (short)0xffff);
        assertTrue(status.isC());
        assertTrue(!status.isO());
        
        status.set_ADD_LAECO((short)0x8001, (short)0xffff);
        assertTrue(status.isC());
        assertTrue(!status.isO());

        status.set_ADD_LAECO((short)0x8000, (short)0x8000);
        assertTrue(status.isC());
        assertTrue(status.isO());
    }
}
