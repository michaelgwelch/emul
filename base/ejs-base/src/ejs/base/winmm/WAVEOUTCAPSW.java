/*
  WAVEOUTCAPSW.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package ejs.base.winmm;
/**
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a>, <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class WAVEOUTCAPSW extends Structure<WAVEOUTCAPSW, WAVEOUTCAPSW.ByValue, WAVEOUTCAPSW.ByReference> {
	public short wMid;
	public short wPid;
	/// Conversion Error : UINT
	/// C type : WCHAR[32]
	public short[] szPname = new short[(32)];
	public int dwFormats;
	public short wChannels;
	public short wReserved1;
	public int dwSupport;
	public WAVEOUTCAPSW() {
		super();
	}
	/// @param szPname C type : WCHAR[32]
	public WAVEOUTCAPSW(short wMid, short wPid, short szPname[], int dwFormats, short wChannels, short wReserved1, int dwSupport) {
		super();
		this.wMid = wMid;
		this.wPid = wPid;
		if (szPname.length != this.szPname.length) 
			throw new java.lang.IllegalArgumentException("Wrong array size !");
		this.szPname = szPname;
		this.dwFormats = dwFormats;
		this.wChannels = wChannels;
		this.wReserved1 = wReserved1;
		this.dwSupport = dwSupport;
	}
	protected ByReference newByReference() { return new ByReference(); }
	protected ByValue newByValue() { return new ByValue(); }
	protected WAVEOUTCAPSW newInstance() { return new WAVEOUTCAPSW(); }
	public static WAVEOUTCAPSW[] newArray(int arrayLength) {
		return Structure.newArray(WAVEOUTCAPSW.class, arrayLength);
	}
	public static class ByReference extends WAVEOUTCAPSW implements Structure.ByReference {}
	public static class ByValue extends WAVEOUTCAPSW implements Structure.ByValue {}
}
