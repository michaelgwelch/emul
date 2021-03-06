/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River)- [345552] Edit the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tm.te.tcf.filesystem.internal.exceptions;

/**
 * TCF remote file system exception.
 */
public class TCFFileSystemException extends TCFException {
	private static final long serialVersionUID = -5203855887734608373L;

	/**
	 * Constructor.
	 *
	 * @param message
	 *            The exception detail message or <code>null</code>.
	 */
	public TCFFileSystemException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message
	 *            The exception detail message or <code>null</code>.
	 * @param cause
	 *            The exception cause or <code>null</code>.
	 */
	public TCFFileSystemException(String message, Throwable cause) {
		super(message, cause);
	}

}
