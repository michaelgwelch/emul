/*
  Check.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package ejs.base.utils;

public class Check {
    public static void checkArg(Object o) {
        if (o == null) {
			throw new IllegalArgumentException();
		}
    }
    public static void checkArg(boolean state) {
        if (!state) {
			throw new IllegalArgumentException();
		}
    }
    public static void checkState(boolean b) {
        if (!b) {
			throw new IllegalStateException();
		}
    }
    public static void failedArg(Throwable t) {
        throw new IllegalArgumentException(t);
    }
}
