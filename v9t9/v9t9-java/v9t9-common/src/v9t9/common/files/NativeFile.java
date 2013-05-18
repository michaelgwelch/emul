/*
  NativeFile.java

  (c) 2008-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.files;

import java.io.File;

/**
 * This is the interface to a native (TI-99) file
 * @author ejs
 */
public interface NativeFile extends EmulatedFile {
    /** Get the host file */
    public File getFile();
}