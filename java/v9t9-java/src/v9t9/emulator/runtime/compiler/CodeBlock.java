package v9t9.emulator.runtime.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.ejs.coffee.core.utils.HexUtils;

import v9t9.emulator.runtime.cpu.AbortedException;
import v9t9.emulator.runtime.cpu.Executor;
import v9t9.engine.HighLevelCodeInfo;
import v9t9.engine.cpu.InstTableCommon;
import v9t9.engine.cpu.RawInstruction;
import v9t9.engine.memory.MemoryEntry;
import v9t9.tools.asm.decomp.HighLevelInstruction;

/** This represents a compiled block of code. */
public class CodeBlock implements ICompiledCode, v9t9.engine.memory.MemoryListener {
	HighLevelCodeInfo highLevel;
    /** basic class name */
    String className;
    /** unique class name, for rebuilds */
    String uniqueClassName;
    /** tell whether this is being built */
    boolean building;
    /** ignore this block in the future? */
    boolean ignore;
    /** generated bytecode */
    byte[] bytecode;
    /** implementation */
    CompiledCode code;
    String baseName;
    private boolean running;
	private Executor exec;
	private DirectLoader loader;
	int addr;
	int size;
	MemoryEntry ent;
	private Compiler compiler;
	private AbortedException gAbortedException = new AbortedException();
    
    static int uniqueClassSuffix;

    public CodeBlock(Executor exec, DirectLoader loader, MemoryEntry ent, int addr, int size) {
        this.exec = exec;
        this.loader = loader;
        this.highLevel = exec.getHighLevelCode(ent);
        this.ent = ent;
        this.addr = addr;
        this.size = size;
        this.baseName = createBaseIdentifier(ent.getUniqueName()); 
        this.className = this.getClass().getName() + "$" + baseName + "_";
        exec.cpu.getMachine().getMemory().addListener(this);
    }
    
    String createBaseIdentifier(String entName) {
        if (entName == null) {
            return HexUtils.toHex4(addr);
        }
        String copy = new String();
        for (int i = 0; i < entName.length(); i++) {
            char c = entName.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
				copy += c;
			} else {
				copy += '_';
			}
        }
        return copy + HexUtils.toHex4(addr);
    }

    public boolean matches(MemoryEntry ent_) {
        return this.ent == ent_;
    }

    private void getUniqueClassName() {
        // we may be reloading the class due to changes,
        // and we need a unique name for this
        uniqueClassName = className;
        try {
            if (Class.forName(className, true, loader) != null) {
                // if we get here, we already loaded this class and 
                // need to rename it
                System.out.print("Rewriting class file " + className);
                uniqueClassName += "$" + uniqueClassSuffix;
                uniqueClassSuffix++;
                System.out.println(" as " + uniqueClassName);
            } 
        } catch (ClassNotFoundException e) {
            
        }
    }
    
	public void compile(Compiler compiler) {
		this.compiler = compiler;
		build();
	}
	
    /** Build the bytecode for a block of memory */
    boolean build() {
        if (bytecode == null) {
            if (ignore) {
				return false;
			}

            if (building) {
				return false;
			}
            
            getUniqueClassName();

            building = true;
            
            try {
                clear();
                System.out.println("compiling code block at >"
                        + HexUtils.toHex4(addr) + ":"
                        + HexUtils.toHex4(size) + "/" + ent.getUniqueName());

                highLevel.analyze();
                
                int numinsts = size / 2;
                RawInstruction insts[] = new RawInstruction[numinsts];
        	    
        	    for (int i = 0; i < numinsts; i++) {
        	    	insts[i] = highLevel.getInstruction(addr + i * 2);
        	    }
        	    
        	    if (Compiler.settingOptimize.getBoolean() 
        	    		&& Compiler.settingOptimizeStatus.getBoolean()) {
        	    	HLInstructionOptimizer.peephole_status(insts, numinsts);
        	    }
        	    
            	bytecode = compiler.compile(uniqueClassName, baseName,
            			highLevel,
            			insts, null);
            	
            	if (bytecode == null) {
            		ignore = true;
            		return false;
            	}
            } finally {
            	building = false;
            }
        }
        if (!load()) {
			return false;
		}
        return true;
    }

    void clear() {
        code = null;
        bytecode = null;
    }

     public boolean run() throws AbortedException {
        /* build the code if necessary... */
        if (!build()) {
			return false;
		}
            
        /* verify preconditions */
        if (!compiler.validCpuState()) {
			return false;
		}
        
        /* now execute */
        running = true;
        code.nInstructions = 0;
        code.nCycles = 0;
        boolean ret = true;
        AbortedException abort = null;
        try {
        	//System.out.println(code.getClass());
            ret = code.run();
        }
        catch (AbortedException e) {
        	exec.nSwitches++;
        	abort = e;
        }
        finally {
        	
        	// throws due to AbortedException usually 
	        running = false;
	        if (code != null) {
	        	if (code.nInstructions == 0 && abort == null) {
	        		ret = false;
	        	}
		        exec.nInstructions += code.nInstructions;
		        exec.nCompiledInstructions += code.nInstructions;
		        exec.cpu.addCycles(code.nCycles);
	        }
	        //System.out.println("invoked "+code.nInstructions+" at "+Utils.toHex4(origpc)+" to "+Utils.toHex4(exec.cpu.getPC()));
	        
	        if (abort == null && !ret) {
        		// target the PC later
        		int pc = exec.cpu.getPC() & 0xffff;
        		HighLevelInstruction inst = highLevel.getLLInstructions().get(pc);
        		if (inst != null && inst.getInst().getInst() != InstTableCommon.Idata) {
        			if ((inst.flags & HighLevelInstruction.fStartsBlock) == 0) {
        				inst.flags |= HighLevelInstruction.fStartsBlock;
        				/*
        				highLevel.markDirty();
        				if (origpc >= addr && origpc < addr + size && pc >= addr && pc < addr + size) {
        					clear();
        				}
        				*/
        			}
        		}
        	}
	        if (abort != null)
	        	throw abort;
        }
        return ret;
    }


    /**
     * @param exec
     * @return
     */
    @SuppressWarnings("unchecked")
	private boolean load() {
        if (code != null) {
			return true;
		}
            
        if (bytecode == null) {
			return false;
		}

        // load and construct an instance of the class
        Class clas = loader.load(uniqueClassName, bytecode);
        try {
            Constructor cons = clas.getConstructor(new Class[] { Executor.class });
            code = (CompiledCode)cons.newInstance(new Object[] { exec });
            return true;
        } catch (InvocationTargetException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
            return false;
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
            return false;
        } catch (IllegalAccessException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
            return false;
        } catch (InstantiationException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
            return false;
        }
    }

    /* (non-Javadoc)
     * @see v9t9.Memory.Listener#notifyMemoryChanged(v9t9.MemoryEntry)
     */
    public void logicalMemoryMapChanged(MemoryEntry entry) {
    	//System.out.println("Memory map changed");
        //clear();
        if (running) {
			throw gAbortedException;
		} 
    }
    public void physicalMemoryMapChanged(MemoryEntry entry) {
    	//System.out.println("Memory map changed");
    	//clear();
    	if (running) {
    		throw gAbortedException;
    	} 
    }


}
