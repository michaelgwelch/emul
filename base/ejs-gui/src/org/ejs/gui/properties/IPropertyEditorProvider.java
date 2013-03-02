/*
  IPropertyEditorProvider.java

  (c) 2010-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package org.ejs.gui.properties;

import ejs.base.properties.IProperty;



/**
 * @author ejs
 *
 */
public interface IPropertyEditorProvider {
	String getLabel(IProperty property);
	IPropertyEditor createEditor(IProperty property);
}
