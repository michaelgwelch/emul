/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tm.te.ui.views.interfaces.workingsets;

/**
 * Name and IDs used by the working set implementation.
 */
public interface IWorkingSetNameIDs {

	/** The element factory id.*/
	String FACTORY_ID = "factoryId"; //$NON-NLS-1$

	/** The attribute to store the working set element's id. */
	String ATTR_ELEMENTID = "elementId"; //$NON-NLS-1$

	/** The attribute to store the working set's name. */
	String ATTR_WORKINGSET_NAME = "workingSetName"; //$NON-NLS-1$
}