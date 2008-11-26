/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Feb 20, 2006
 *
 */
package v9t9.emulator.clients.builtin.video.tms9918a;

import java.util.Arrays;

import v9t9.emulator.clients.builtin.video.BlankModeRedrawHandler;
import v9t9.emulator.clients.builtin.video.RedrawBlock;
import v9t9.emulator.clients.builtin.video.VdpCanvas;
import v9t9.emulator.clients.builtin.video.VdpChanges;
import v9t9.emulator.clients.builtin.video.VdpModeInfo;
import v9t9.emulator.clients.builtin.video.VdpModeRedrawHandler;
import v9t9.emulator.hardware.memory.mmio.VdpMmio;
import v9t9.engine.VdpHandler;
import v9t9.engine.memory.ByteMemoryAccess;
import v9t9.engine.memory.MemoryDomain;

/**
 * This is the 99/4A VDP chip.
 * <p>
 * Mode bits:
 * <p>
 * R0:	M3 @ 1
 * R1:	M1 @ 4, M2 @ 3 
 * <p>
 * <pre>
 *                   M1  M2  M3  
 * Text 1 mode:      1   0   0   = 1
 * Multicolor:       0   1   0   = 2
 * Graphics 1 mode:  0   0   0   = 0
 * Graphics 2 mode:  0   0   1   = 4
 * </pre>
 * @author ejs
 */
public class VdpTMS9918A implements VdpHandler {
	private RedrawBlock[] blocks;
	protected MemoryDomain vdpMemory;

	protected byte vdpregs[];
	protected byte vdpbg;
	protected byte vdpfg;
	protected boolean drawSprites = true;
	//private final static int REDRAW_NOW = 1		;	/* same-mode change */
	protected final static int REDRAW_SPRITES = 2	;	/* sprites change */
	protected final static int REDRAW_MODE = 4		;	/* mode change */
	protected final static int REDRAW_BLANK = 8		;	/* make blank */
	protected final static int REDRAW_PALETTE = 16;
	protected boolean vdpchanged;

	protected VdpCanvas vdpCanvas;
	protected VdpModeRedrawHandler vdpModeRedrawHandler;
	protected SpriteRedrawHandler spriteRedrawHandler;
	protected VdpChanges vdpChanges = new VdpChanges(getMaxRedrawblocks());
	protected byte vdpStatus;
	
	protected VdpMmio vdpMmio;
	public final static int VDP_INTERRUPT = 0x80;
	public final static int VDP_COINC = 0x40;
	public final static int VDP_FIVE_SPRITES = 0x20;
	public final static int VDP_FIFTH_SPRITE = 0x1f;
	
	final public static int R0_M3 = 0x2; // bitmap
	public final static int R0_EXTERNAL = 1;
	public final static int R1_RAMSIZE = 128;
	public final static int R1_NOBLANK = 64;
	public final static int R1_INT = 32;
	final public static int R1_M1 = 0x10; // text
	final public static int R1_M2 = 0x8; // multi
	
	public final static int R1_SPR4 = 2;
	public final static int R1_SPRMAG = 1;

	public final static int MODE_TEXT = 1;
	public final static int MODE_GRAPHICS = 0;
	public final static int MODE_BITMAP = 4;
	public final static int MODE_MULTI = 2;
	
	public VdpTMS9918A(MemoryDomain videoMemory, VdpCanvas vdpCanvas) {
		this.vdpMemory = videoMemory;
		this.vdpCanvas = vdpCanvas;
		this.vdpregs = allocVdpRegs();
		vdpCanvas.setSize(256, 192);
	}
	
	public MemoryDomain getVideoMemory() {
		return vdpMemory;
	}
	 
	public void setVdpMmio(VdpMmio vdpMmio) {
		this.vdpMmio = vdpMmio;
	}

	public VdpMmio getVdpMmio() {
		return vdpMmio;
	}
	protected byte[] allocVdpRegs() {
		return new byte[8];
	}

	public byte readVdpReg(int reg) {
		return vdpregs[reg];
	}
	
	protected final boolean CHANGED(byte old,byte val, int v) { return (old&(v))!=(val&(v)); }

    /* (non-Javadoc)
     * @see v9t9.handlers.VdpHandler#writeVdpReg(byte, byte, byte)
     */
    final synchronized public void writeVdpReg(int reg, byte val) {
    	if (reg >= vdpregs.length)
    		return;
    	
    	byte old = vdpregs[reg];
    	vdpregs[reg] = val;
    	if (old == val)
    		return;
    	
    	int         redraw = doWriteVdpReg(reg, old, val);

    	/*  This flag must be checked first because
	 	   it affects the meaning of the following 
	 	   calls and checks. */
	 	if ((redraw & REDRAW_MODE) != 0) {
	 		establishVideoMode();
	 		setupBackdrop();
	 		dirtyAll();
	 	}
	
	 	if ((redraw & REDRAW_SPRITES) != 0) {
			dirtySprites();
		}

	 	if ((redraw & REDRAW_PALETTE) != 0) {
	 		setupBackdrop();
	 		dirtyAll();
	 	}
	
	 	if ((redraw & REDRAW_BLANK) != 0) {
	 		if ((vdpregs[1] & VdpTMS9918A.R1_NOBLANK) == 0) {
	 			vdpCanvas.setBlank(true);
	 			dirtyAll();
	 			//update();
	 		} else {
	 			vdpCanvas.setBlank(false);
	 			dirtyAll();
	 			//update();
	 		}
	 	}

    }
    
    /** Set the backdrop based on the mode */
    protected void setupBackdrop() {
    	vdpCanvas.setClearColor(vdpbg);
	}

	protected int doWriteVdpReg(int reg, byte old, byte val) {
    	int redraw = 0;
    	
    	switch (reg) {
    	case 0:					/* bitmap/video-in */
    		if (CHANGED(old, val, VdpTMS9918A.R0_M3+VdpTMS9918A.R0_EXTERNAL)) {
    			redraw |= REDRAW_MODE;
    		}
    		break;

    	case 1:					/* various modes, sprite stuff */
    		if (CHANGED(old, val, VdpTMS9918A.R1_NOBLANK)) {
    			redraw |= REDRAW_BLANK | REDRAW_MODE;
    		}

    		if (CHANGED(old, val, VdpTMS9918A.R1_SPRMAG + VdpTMS9918A.R1_SPR4)) {
    			redraw |= REDRAW_SPRITES;
    		}

    		if (CHANGED(old, val, VdpTMS9918A.R1_M1 | VdpTMS9918A.R1_M2)) {
    			redraw |= REDRAW_MODE;
    		}

    		/* if interrupts enabled, and interrupt was pending, trigger it */
    		if ((val & VdpTMS9918A.R1_INT) != 0 
    		&& 	(old & VdpTMS9918A.R1_INT) == 0 
    		&&	(vdpStatus & VdpTMS9918A.VDP_INTERRUPT) != 0) 
    		{
    			//trigger9901int( M_INT_VDP);	// TODO
    		}

    		break;

    	case 2:					/* screen image table */
    	case 3:					/* color table */
    	case 4:					/* pattern table */
    	case 5:					/* sprite table */
    	case 6:					/* sprite pattern table */
    		redraw |= REDRAW_MODE;
    		break;

    	case 7:					/* foreground/background color */
			vdpfg = (byte) ((val >> 4) & 0xf);
			vdpbg = (byte) (val & 0xf);
			redraw |= REDRAW_PALETTE;
    		break;

    	default:

    	}

    	return redraw;
    }

    /** Tell if the registers indicate a blank screen. */
    protected boolean isBlank() {
    	return (vdpregs[1] & VdpTMS9918A.R1_NOBLANK) == 0;
    }
    
    final protected int getModeNumber() {
		return (vdpregs[1] & R1_M1) / R1_M1
		+ (vdpregs[1] & R1_M2) / R1_M2 * 2
		+ (vdpregs[0] & R0_M3) / R0_M3 * 4;
	}
    
    /**
     * Set up the vdpModeRedrawHandler, spriteRedrawHandler, and memory access
     * times for the mode defined by the vdp registers.
     */
    protected void establishVideoMode() {
    	/* Is the screen really blank? */
		if (isBlank()) {
			setBlankMode();
			return;
		}
		
		switch (getModeNumber()) {
		case MODE_TEXT:
			setTextMode();
			dirtyAll();	// for border
			break;
		case MODE_MULTI:
			setMultiMode();
			break;
		case MODE_BITMAP:
			setBitmapMode();
			break;
		case MODE_GRAPHICS:
		default:
			setGraphicsMode();
			break;
		}
	}

    /**
     * Get the address a table will take given the mode and memory size
     * @return
     */
    protected int getModeAddressMask() {
    	return getMemorySize() - 1;
    }
    
    protected VdpModeInfo createSpriteModeInfo() {
		VdpModeInfo vdpModeInfo = new VdpModeInfo(); 
		int ramsize = getModeAddressMask();

		vdpModeInfo.sprite.base = getSpriteTableBase() & ramsize;
		vdpModeInfo.sprite.size = 128;
		vdpModeInfo.sprpat.base = (vdpregs[6] * 0x800) & ramsize;
		vdpModeInfo.sprpat.size = 2048;
		return vdpModeInfo;
	}

	protected int getSpriteTableBase() {
		return (vdpregs[5] * 0x80);
	}

	protected void setGraphicsMode() {
		vdpCanvas.setSize(256, vdpCanvas.getHeight());
		vdpModeRedrawHandler = new GraphicsModeRedrawHandler(
				vdpregs, this, vdpChanges, vdpCanvas, createGraphicsModeInfo());
		spriteRedrawHandler = createSpriteRedrawHandler();
		vdpMmio.setMemoryAccessCycles(8);
		initUpdateBlocks(8);
	}

	private SpriteRedrawHandler createSpriteRedrawHandler() {
		return new SpriteRedrawHandler(
				vdpregs, this, vdpChanges, vdpCanvas, createSpriteModeInfo());
	}

	
	protected VdpModeInfo createGraphicsModeInfo() {
		VdpModeInfo vdpModeInfo = new VdpModeInfo(); 
		int ramsize = getModeAddressMask();
		vdpModeInfo.screen.base = (vdpregs[2] * 0x400) & ramsize;
		vdpModeInfo.screen.size = 768;
		vdpModeInfo.color.base = getColorTableBase() & ramsize;
		vdpModeInfo.color.size = 32;
		vdpModeInfo.patt.base = getPatternTableBase() & ramsize;
		vdpModeInfo.patt.size = 2048;
		vdpModeInfo.sprite.base = getSpriteTableBase() & ramsize;
		vdpModeInfo.sprite.size = 128;
		vdpModeInfo.sprpat.base = (vdpregs[6] * 0x800) & ramsize;
		vdpModeInfo.sprpat.size = 2048;
		return vdpModeInfo;
	}

	protected int getPatternTableBase() {
		return ((vdpregs[4] & 0xff) * 0x800);
	}

	protected int getColorTableBase() {
		return ((vdpregs[3] & 0xff) * 0x40);
	}

	protected void setMultiMode() {
		vdpCanvas.setSize(256, vdpCanvas.getHeight());
		vdpModeRedrawHandler = new MulticolorModeRedrawHandler(
				vdpregs, this, vdpChanges, vdpCanvas, createMultiModeInfo());
		spriteRedrawHandler = createSpriteRedrawHandler();
		vdpMmio.setMemoryAccessCycles(2);
		initUpdateBlocks(8);
	}

	protected VdpModeInfo createMultiModeInfo() {
		VdpModeInfo vdpModeInfo = new VdpModeInfo(); 
		int ramsize = getModeAddressMask();
		
		vdpModeInfo.screen.base = (vdpregs[2] * 0x400) & ramsize;
		vdpModeInfo.screen.size = 768;
		vdpModeInfo.color.base = 0;
		vdpModeInfo.color.size = 0;
		vdpModeInfo.patt.base = getPatternTableBase() & ramsize;
		vdpModeInfo.patt.size = 1536;
		vdpModeInfo.sprite.base = getSpriteTableBase() & ramsize;
		vdpModeInfo.sprite.size = 128;
		
		return vdpModeInfo;
	}

	protected void setTextMode() {
		vdpCanvas.setSize(256, vdpCanvas.getHeight());
		vdpModeRedrawHandler = new TextModeRedrawHandler(
				vdpregs, this, vdpChanges, vdpCanvas, createTextModeInfo());
		spriteRedrawHandler = null;
		vdpMmio.setMemoryAccessCycles(1);
		initUpdateBlocks(6);
	}

	protected VdpModeInfo createTextModeInfo() {
		VdpModeInfo vdpModeInfo = new VdpModeInfo(); 
		int ramsize = getModeAddressMask();
		
		vdpModeInfo.screen.base = (vdpregs[2] * 0x400) & ramsize;
		vdpModeInfo.screen.size = 960;
		vdpModeInfo.color.base = 0;
		vdpModeInfo.color.size = 0;
		vdpModeInfo.patt.base = getPatternTableBase() & ramsize;
		vdpModeInfo.patt.size = 2048;
		vdpModeInfo.sprite.base = 0;
		vdpModeInfo.sprite.size = 0;
		vdpModeInfo.sprpat.base = 0;
		vdpModeInfo.sprpat.size = 0;
		return vdpModeInfo;
	}

	protected void setBitmapMode() {
		vdpCanvas.setSize(256, vdpCanvas.getHeight());
		vdpModeRedrawHandler = new BitmapModeRedrawHandler(
				vdpregs, this, vdpChanges, vdpCanvas, createBitmapModeInfo());
		spriteRedrawHandler = createSpriteRedrawHandler();
		vdpMmio.setMemoryAccessCycles(8);
		initUpdateBlocks(8);
	}

	protected VdpModeInfo createBitmapModeInfo() {
		VdpModeInfo vdpModeInfo = new VdpModeInfo(); 
		int ramsize = getModeAddressMask();

		vdpModeInfo.screen.base = (vdpregs[2] * 0x400) & ramsize;
		vdpModeInfo.screen.size = 768;
		vdpModeInfo.sprite.base = getSpriteTableBase() & ramsize;
		vdpModeInfo.sprite.size = 128;
		vdpModeInfo.sprpat.base = (vdpregs[6] * 0x800) & ramsize;
		vdpModeInfo.sprpat.size = 2048;

		vdpModeInfo.color.base = getColorTableBase() & ~0x1fff & ramsize;
		vdpModeInfo.color.size = 6144;
		
		vdpModeInfo.patt.base = getPatternTableBase() & ~0x1fff & ramsize;
		vdpModeInfo.patt.size = 6144;
		
		return vdpModeInfo;
	}

	protected void setBlankMode() {
		vdpCanvas.setSize(256, vdpCanvas.getHeight());
		vdpModeRedrawHandler = new BlankModeRedrawHandler(
				vdpregs, this, vdpChanges, vdpCanvas, createBlankModeInfo());
		spriteRedrawHandler = null;
		vdpMmio.setMemoryAccessCycles(0);
		initUpdateBlocks(8);
	}

    protected VdpModeInfo createBlankModeInfo() {
    	VdpModeInfo vdpModeInfo = new VdpModeInfo(); 
    	vdpModeInfo.screen.base = 0;
		vdpModeInfo.screen.size = 0;
		vdpModeInfo.color.base = 0;
		vdpModeInfo.color.size = 0;
		vdpModeInfo.patt.base = 0;
		vdpModeInfo.patt.size = 0;
		vdpModeInfo.sprite.base = 0;
		vdpModeInfo.sprite.size = 0;
		vdpModeInfo.sprpat.base = 0;
		vdpModeInfo.sprpat.size = 0;	
		return vdpModeInfo;
	}

	/** preinitialize the update blocks with the sizes for this mode */
	protected void initUpdateBlocks(int blockWidth) {
		int w = blockWidth;
    	int h = 8;
		if (blocks == null) {
			blocks = new RedrawBlock[getMaxRedrawblocks()];
			for (int i = 0; i < blocks.length; i++) {
				blocks[i] = new RedrawBlock();
			}
		}
		if (blocks[0].w != blockWidth) {
			for (int i = 0; i < blocks.length; i++) {
				blocks[i].w = w;
				blocks[i].h = h;
			}
		}
	}

	protected int getMaxRedrawblocks() {
		return 1024;
	}

	/* (non-Javadoc)
     * @see v9t9.handlers.VdpHandler#readVdpStatus()
     */
    public byte readVdpStatus() {
		/* >8802, status read */
    	byte ret = vdpStatus;
		vdpStatus &= ~0xe0;		// thierry:  reset bits when read
		// TODO machine.getCpu().reset9901int(v9t9.cpu.Cpu.M_INT_VDP);

        return ret;
    }

    /* (non-Javadoc)
     * @see v9t9.handlers.VdpHandler#writeVdpMemory(short, byte)
     */
    public synchronized void touchAbsoluteVdpMemory(int vdpaddr, byte val) {
		if (vdpModeRedrawHandler != null) {
	    	vdpchanged |= vdpModeRedrawHandler.touch(vdpaddr);
	    	if (spriteRedrawHandler != null) {
	    		vdpchanged |= spriteRedrawHandler.touch(vdpaddr);
	    	}
		}
    }
    
    public byte readAbsoluteVdpMemory(int vdpaddr) {
    	return vdpMmio.readFlatMemory(vdpaddr);
    }

	public ByteMemoryAccess getByteReadMemoryAccess(int addr) {
		return vdpMmio.getByteReadMemoryAccess(addr);
	}
	
	protected void dirtySprites() {
		vdpChanges.sprite = -1;
		Arrays.fill(vdpChanges.sprpat, 0, vdpChanges.sprpat.length, (byte)1);
		vdpchanged = true;
	}


	protected void dirtyAll() {
		vdpchanged = true;
		vdpChanges.fullRedraw = true;
	}
	
	public synchronized void update() {
		if (!vdpchanged)
			return;
		
		if (vdpModeRedrawHandler != null) {
			vdpModeRedrawHandler.propagateTouches();
			
			if (spriteRedrawHandler != null) {
				vdpStatus = spriteRedrawHandler.updateSpriteCoverage(vdpStatus);
			}
			
			if (vdpChanges.fullRedraw) {
				vdpCanvas.clear();
				vdpCanvas.markDirty();
			}
			
			int count = vdpModeRedrawHandler.updateCanvas(blocks, vdpChanges.fullRedraw);
			
			if (spriteRedrawHandler != null && drawSprites) {
				spriteRedrawHandler.updateCanvas(vdpChanges.fullRedraw);
			}

			vdpCanvas.markDirty(blocks, count);
			
			Arrays.fill(vdpChanges.screen, 0, vdpChanges.screen.length, (byte) 0);
			Arrays.fill(vdpChanges.patt, 0, vdpChanges.patt.length, (byte) 0);
			Arrays.fill(vdpChanges.sprpat, 0, vdpChanges.sprpat.length, (byte) 0);
			Arrays.fill(vdpChanges.color, 0, vdpChanges.color.length, (byte) 0);
			vdpChanges.sprite = 0;
			
			vdpChanges.fullRedraw = false;
		}
		
		vdpchanged = false;
		
	}

	public synchronized VdpCanvas getCanvas() {
		return vdpCanvas;
	}

	public int getMemorySize() {
		return vdpMmio.getMemorySize();
	}
	
	public void tick() {
		
	}
}
