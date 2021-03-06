/*
  OldDemoFormatOutputStream.java

  (c) 2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.demos.format.old;

import java.io.IOException;
import java.io.OutputStream;

import v9t9.common.demos.IDemoEvent;
import v9t9.common.demos.IDemoOutputBuffer;
import v9t9.common.demos.IDemoOutputEventBuffer;
import v9t9.common.demos.IDemoOutputStream;
import v9t9.engine.demos.events.OldSpeechEvent;
import v9t9.engine.demos.events.SoundWriteDataEvent;
import v9t9.engine.demos.events.SoundWriteRegisterEvent;
import v9t9.engine.demos.events.SpeechEvent;
import v9t9.engine.demos.events.TimerTick;
import v9t9.engine.demos.events.VideoWriteDataEvent;
import v9t9.engine.demos.events.VideoWriteRegisterEvent;
import v9t9.engine.demos.stream.BaseDemoOutputStream;

/**
 * Writer for TI Emulator v6.0 & V9t9 demo formats
 * @author ejs
 *
 */
public class OldDemoFormatOutputStream extends BaseDemoOutputStream implements IDemoOutputStream {

	private int ticks60;

	protected OldDemoPacketBuffer videoBuffer;
	protected OldDemoPacketBuffer soundDataBuffer;
	protected OldDemoPacketBuffer speechBuffer;

	public OldDemoFormatOutputStream(OutputStream os) throws IOException {
		super(os);
		
		os.write(OldDemoFormat.DEMO_MAGIC_HEADER_V910);
		
		this.videoBuffer = allocateStandardBuffer(
				OldDemoFormat.VIDEO, OldDemoFormat.VIDEO_BUFFER_SIZE, 
				VideoWriteDataEvent.ID, VideoWriteRegisterEvent.ID);
		this.soundDataBuffer = allocateStandardBuffer(
				OldDemoFormat.SOUND, OldDemoFormat.SOUND_BUFFER_SIZE, 
				SoundWriteDataEvent.ID);
		this.speechBuffer = allocateStandardBuffer(
				OldDemoFormat.SPEECH, OldDemoFormat.SPEECH_BUFFER_SIZE, 
				SpeechEvent.ID);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.demos.IDemoOutputStream#getDemoFormat()
	 */
	@Override
	public byte[] getDemoFormat() {
		return OldDemoFormat.DEMO_MAGIC_HEADER_V910;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoOutputStream#registerBuffer(v9t9.common.demo.IDemoOutputBuffer, java.lang.Class)
	 */
	@Override
	public void registerBuffer(IDemoOutputEventBuffer buffer,
			String eventId) throws IOException {
		if (!buffers.contains(buffer))
			buffers.add((IDemoOutputBuffer) buffer);
	}
	
	private OldDemoPacketBuffer allocateStandardBuffer(int code, int bufSize, 
			String... eventIds) 
			throws IOException {
		OldDemoPacketBuffer buffer = new OldDemoPacketBuffer(os, code, bufSize) {
			@Override
			public void encodeEvent(IDemoEvent event) throws IOException {
				if (event instanceof VideoWriteRegisterEvent) {
					writeVideoRegisterEvent(event);
				}
				else if (event instanceof VideoWriteDataEvent) {
					writeVideoDataEvent(event);
				}
				else if (event instanceof SoundWriteDataEvent) {
					writeSoundDataEvent(event);
				}
				else if (event instanceof SoundWriteRegisterEvent) {
					writeSoundRegisterEvent(event);
				}
				else if (event instanceof SpeechEvent) {
					writeSpeechEvent(event);
				}
				else {
					throw new IOException("unknown event type: " + event);
				}
			}
		};
		
		for (String eventId : eventIds)
			registerBuffer(buffer, eventId);
		
		return buffer;
	}

	
	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoOutputStream#getTimerRate()
	 */
	@Override
	public int getTimerRate() {
		return 60;
	}


	@Override
	public synchronized void writeEvent(IDemoEvent event) throws IOException {
		if (event instanceof TimerTick) {
			writeTimerTick();
		}
		else if (event instanceof VideoWriteRegisterEvent) {
			writeVideoRegisterEvent(event);
		}
		else if (event instanceof VideoWriteDataEvent) {
			writeVideoDataEvent(event);
		}
		else if (event instanceof SoundWriteDataEvent) {
			writeSoundDataEvent(event);
		}
		else if (event instanceof SoundWriteRegisterEvent) {
			writeSoundRegisterEvent(event);
		}
		else if (event instanceof OldSpeechEvent) {
			writeSpeechEvent(event);
		}
		else {
			throw new IOException("unknown event type: " + event);
		}
	}

	@Override
	protected void writeTimerTick() throws IOException {
		flushAll();
		os.write(OldDemoFormat.TICK);
		ticks60++;
	}
	
	protected void writeSpeechEvent(IDemoEvent event) throws IOException {
		OldSpeechEvent ev = (OldSpeechEvent) event;
		if (ev.getCode() != OldSpeechEvent.SPEECH_ADDING_BYTE || !speechBuffer.isAvailable(2)) {
			speechBuffer.flush();
		}

		speechBuffer.push((byte) ev.getCode());
		if (ev.getCode() == OldSpeechEvent.SPEECH_ADDING_BYTE)
			speechBuffer.push(ev.getAddedByte());
		else
			speechBuffer.push((byte) -1);
	}

	protected void writeSoundRegisterEvent(IDemoEvent event)
			throws IOException {
		throw new IOException("this demo format does not support sound registers");
	}

	protected void writeSoundDataEvent(IDemoEvent event)
			throws IOException {
		SoundWriteDataEvent ev = (SoundWriteDataEvent) event;
		byte[] data = ev.getData();
		soundDataBuffer.pushData(data, 0, ev.getLength());
	}

	protected void writeVideoDataEvent(IDemoEvent event)
			throws IOException {
		VideoWriteDataEvent we = (VideoWriteDataEvent) event;
		int len = we.getLength();
		int offs = 0;
		while (len > 0) {
			int toUse = Math.min(255, len);
			if (!videoBuffer.isAvailable(toUse + 3)) {
				videoBuffer.flush();
			}
			videoBuffer.pushWord(we.getAddress() + offs);
			videoBuffer.push((byte) toUse);
			videoBuffer.pushData(we.getData(), offs + we.getOffset(), toUse);
			
			len -= toUse;
			offs += toUse;
		}
	}

	protected void writeVideoRegisterEvent(IDemoEvent event)
			throws IOException {
		if (!videoBuffer.isAvailable(2)) {
			videoBuffer.flush();
		}
		videoBuffer.pushWord(((VideoWriteRegisterEvent) event).getAddr());
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoOutputStream#getElapsedTime()
	 */
	@Override
	public long getElapsedTime() {
		return ticks60 * 1000 / 60;
	}
}
