/*
  RegisterViews.java

  (c) 2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.gui.client.swt.shells.debugger;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import v9t9.common.machine.IMachine;
import v9t9.gui.client.swt.shells.debugger.CpuViewer.ICpuTracker;

/**
 * View CPU and VDP registers
 * @author ejs
 *
 */
public class RegisterViews extends SashForm implements ICpuTracker {

	private RegisterViewer cpuRegisterViewer;
	private RegisterViewer vdpRegisterViewer;
	
	public RegisterViews(Composite parent, int style, final IMachine machine) {
		super(parent, style | SWT.VERTICAL);
		
		setLayout(new GridLayout());

		IRegisterProvider cpuRegs = new CpuRegisterProvider(machine);
		IRegisterProvider vdpRegs = new VdpRegisterProvider(machine);
		
		cpuRegisterViewer = new RegisterViewer(this, machine, cpuRegs, 4);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(cpuRegisterViewer);
		vdpRegisterViewer = new RegisterViewer(this, machine, vdpRegs, 
				vdpRegs.getRegisterCount() < 16 ? 4 : 12);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(vdpRegisterViewer);
	}


	public void updateForInstruction() {
		cpuRegisterViewer.update();
		vdpRegisterViewer.update();
	}

}
