/*
  ZeroByteMemoryArea.java

  (c) 2005-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.memory;

/**
 * @author ejs
 */
public class ZeroByteMemoryArea extends ByteMemoryArea {
	/* can neither read nor write directly */
	/* for reads, return zero */
	/* for writes, ignore */
    static byte zeroes[] = new byte[0x10000];

	public ZeroByteMemoryArea() {
		super(1);
		memory = zeroes;
	}
}

