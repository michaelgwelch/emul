/*
  ConstPool.java

  (c) 2008-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.asm.transform;

import java.util.LinkedHashMap;
import java.util.Map;

import v9t9.tools.asm.IAssembler;
import v9t9.tools.asm.Symbol;
import v9t9.tools.asm.operand.hl.AddrOperand;
import v9t9.tools.asm.operand.hl.AssemblerOperand;
import v9t9.tools.asm.operand.hl.BinaryOperand;
import v9t9.tools.asm.operand.hl.NumberOperand;
import v9t9.tools.asm.operand.hl.SymbolOperand;

/**
 * This keeps track of constants referenced as #... operands in instructions.
 * <p>
 * It tries to pack constants into bytes if possible, unless a word instruction
 * also uses the value.
 * <p>
 * For instance: bytes 1, 2 will be allocated >0102
 * For bytes 1 and word >0100 will be allocated >0100 (can also hold byte 0)
 * For word >0001 and >0100, these are two words.
 * @author Ed
 *
 */
public class ConstPool {
	private Map<Integer, Integer> instWordMap = new LinkedHashMap<Integer, Integer>();
	private Map<Integer, Integer> constWordMap = new LinkedHashMap<Integer, Integer>();
	private Symbol tableAddr;
	private int tableSize;
	private boolean lastAccessWasByte;

	private final IAssembler assembler;
	
	public ConstPool(IAssembler assembler) {
		this.assembler = assembler;
	}
	
	public void clear() {
		instWordMap.clear();
		constWordMap.clear();
		if (tableAddr != null)
			tableAddr.setDefined(false);
		tableAddr = null;
		tableSize = 0;
		lastAccessWasByte = false;
	}

	public AssemblerOperand allocateByte(int value) {
		value &= 0xff;
		
		// see if we have an entry in a low or high part of an instruction
		for (Map.Entry<Integer, Integer> entry : instWordMap.entrySet()) {
			if ((entry.getKey() & 0xff00) >> 8 == value)
				return new AddrOperand(new NumberOperand(entry.getValue()));
			if ((entry.getKey() & 0x00ff) == value)
				return new AddrOperand(new NumberOperand(entry.getValue() + 1));
		}
		
		// see if we have an entry in a low or high part of another const pool entry
		for (Map.Entry<Integer, Integer> entry : constWordMap.entrySet()) {
			if ((entry.getKey() & 0xff00) >> 8 == value)
				return createTableOffset(entry.getValue());
			if ((entry.getKey() & 0x00ff) == value)
				return createTableOffset(entry.getValue() + 1);
		}
		
		// else, register a byte
		
		// if we last registered a byte, and no words access it yet,
		// we can put the new byte there
		if ((tableSize & 1) == 1 && lastAccessWasByte) {
			// find the word for this
			for (Map.Entry<Integer, Integer> entry : constWordMap.entrySet()) {
				if (entry.getValue() == tableSize - 1) {
					constWordMap.remove(entry.getKey());
					constWordMap.put(entry.getKey() | value, tableSize - 1);
					lastAccessWasByte = true;
					return createTableOffset(tableSize++);
				}
			}
		}
		
		// bump size if necessary
		if ((tableSize & 1) == 1)
			tableSize++;
		
		int addr = tableSize;
		constWordMap.put(value << 8, addr);
		tableSize++;
		lastAccessWasByte = true;
		
		return createTableOffset(addr);
	}
	
	public AssemblerOperand allocateWord(int value) {
		value &= 0xffff;
		
		// see if we have an entry as an opcode word
		Integer pc = instWordMap.get(value);
		if (pc != null) {
			return new AddrOperand(new NumberOperand(pc));
		}
		
		// see if we have an entry as a word
		Integer offset = constWordMap.get(value);
		if (offset != null) {
			if (offset == tableSize - 2)
				lastAccessWasByte = false;
			return createTableOffset(offset);
		}
			
		// else, register a word 
		if ((tableSize & 1) == 1) {
			tableSize++;
			
		}
		int addr = tableSize;
		tableSize += 2;
		constWordMap.put(value, addr);
		
		lastAccessWasByte = false;
		return createTableOffset(addr);
	}

	public Symbol getTableAddr() {
		return tableAddr;
	}

	/**
	 * Get @@table+offset
	 * @param value
	 * @return
	 */
	public AssemblerOperand createTableOffset(int value) {
		if (tableAddr == null)
			tableAddr = assembler.getSymbolTable().createSymbol("$consttable");
		
		return new AddrOperand(new BinaryOperand('+', new SymbolOperand(tableAddr), new NumberOperand(value)));
	}
	
	/**
	 * Get the offset into @@table (or -1 if not an entry)
	 * @param operand
	 * @return offset into table in bytes, or -1
	 */
	public int getTableOffset(AssemblerOperand operand) {
		if (operand instanceof AddrOperand) {
			if (((AddrOperand) operand).getAddr() instanceof BinaryOperand) {
				BinaryOperand binop = (BinaryOperand) ((AddrOperand) operand).getAddr();
				if (binop.getKind() == '+'
					&& binop.getLeft() instanceof SymbolOperand
					&& ((SymbolOperand) binop.getLeft()).getSymbol().equals(tableAddr)
					&& binop.getRight() instanceof NumberOperand)
					return ((NumberOperand) binop.getRight()).getValue();
			}
		}
		return -1;
	}

	public byte[] getBytes() {
		byte[] bytes = new byte[(tableSize + 1) & 0xfffe];
		for (Map.Entry<Integer, Integer> entry : constWordMap.entrySet()) {
			int addr = entry.getValue();
			int key = entry.getKey();
			bytes[addr] = (byte) (key >> 8);
			bytes[addr+1] = (byte) (key & 0xff);
		}
		return bytes;
	}

	/** Reset the contributions from other instructions */
	public void resetInstructions() {
		
	}

	/**
	 * @return the instWordMap
	 */
	public Map<Integer, Integer> getInstWordMap() {
		return instWordMap;
	}
	
	public int getSize() {
		return tableSize;
	}
}
