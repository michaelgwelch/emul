/*
  RawTrackDiskImage.java

  (c) 2010-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.files.image;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;

import v9t9.common.client.ISettingsHandler;

/**
 * Raw track image which has no header
 * @author ejs
 *
 */
public class RawTrackDiskImage extends BaseTrackDiskImage  {
	
	public RawTrackDiskImage(ISettingsHandler settings, String name, File file) {
		super(name, file, settings);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.realdisk.BaseDiskImage#getDiskType()
	 */
	@Override
	public String getDiskType() {
		return "track-image-raw";
	}
	
	@Override
	public void writeImageHeader() throws IOException {
		if (getHandle() == null || readonly) {
			return;
		}

		/* maintain invariants */
		growImageForContent(); 
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.realdisk.BaseDiskImage#getHeaderSize()
	 */
	@Override
	public int getHeaderSize() {
		return 0;
	}
	
	@Override
	public void readImageHeader() throws IOException {
		if (getHandle() == null)
			return;
		
		readonly = !spec.canWrite();

		// try to get sector 0
		byte[] sector = readSector0(getHandle());

		if (sector == null)
			throw new IOException(MessageFormat.format("RawTrackDiskImage:  disk image ''{0}'' does not appear to be a raw track image",
					spec));

		hdr.setSides(sector[0x12] & 0xff);
		hdr.setTracks(sector[0x11] & 0xff);
		hdr.setSecsPerTrack(sector[0x0C] & 0xff);
		
		if (hdr.getTracks() == 0 || hdr.getSides() == 0 ||
				'D' != sector[0x0D] || 'S' != sector[0x0E] || 'K' != sector[0x0F])
			throw new IOException(MessageFormat.format("RawTrackDiskImage:  disk image ''{0}'' does not appear to be formatted",
					spec));
		
		hdr.setTrackSize((int) (getHandle().length() / hdr.getTracks() / hdr.getSides()));
		if (hdr.getSides() == 1 && hdr.getTrackSize() > 5000) {
			hdr.setTrackSize(hdr.getTrackSize() / 2);
			hdr.setSides(hdr.getSides() + 1);
		}
		
		hdr.setTrack0Offset(0);

		hdr.setSide2DirectionKnown(false);
		
		if (hdr.getTrackSize() <= 0) {
			throw new IOException(MessageFormat.format("RawTrackDiskImage:  disk image ''{0}'' has invalid track size {1}\n",
					  spec, hdr.getTrackSize()));
		}

		if (hdr.getTrackSize() > RealDiskConsts.DSKbuffersize) {
			throw new IOException(MessageFormat.format("RawTrackDiskImage: disk image ''{0}'' has too large track size ({1} > {2})",
					spec, hdr.getTrackSize(), RealDiskConsts.DSKbuffersize));
		}
		
	}
	
	/**
	 * @param handle
	 * @return
	 * @throws IOException 
	 */
	private static byte[] readSector0(RandomAccessFile handle) throws IOException {
		int ch;
		int beforeMarkerSeen = 0;
		int count = 0;
		handle.seek(0);
		while ((ch = handle.read()) >= 0 && count < 18 && ++beforeMarkerSeen < 65536) {
			if (ch == 0xfe 		// ID
			&& handle.read() == 0 	// track
			&& handle.read() == 0	// ...
			&& handle.read() == 0)	// sector 
			{
				while ((ch = handle.read()) != 0xfb && ch >= 0) /* */ ;
				if (ch < 0)
					continue;
				
				byte[] sector = new byte[256];
				handle.read(sector);
				
				if (new String(sector).contains("DSK"))
					return sector;
				count++;
				
				beforeMarkerSeen = 0;
			}
		}	
		return null;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.dsrs.realdisk.BaseDiskImage#getDefaultTrackSize()
	 */
	@Override
	public short getDefaultTrackSize() {
		return (short) 3253;
	}
	
	public static boolean isTrackImage(File file) {
		RandomAccessFile fh = null;
		try {
			fh = new RandomAccessFile(file, "r");
			byte[] sector = readSector0(fh);
			
			return sector != null;
		} catch (IOException e) {
			// ignore
		} finally {
			if (fh != null) {
				try {
					fh.close();
				} catch (IOException e) {
				}
			}
		}
		return false;
	}

}