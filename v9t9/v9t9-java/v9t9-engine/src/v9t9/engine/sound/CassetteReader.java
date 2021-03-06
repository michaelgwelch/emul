/*
  CassetteReader.java

  (c) 2013 Ed Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.sound;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.settings.BasicSettingsHandler;

import ejs.base.properties.IProperty;
import ejs.base.sound.ISoundOutput;


/**
 * @author ejs
 *
 */
public class CassetteReader {

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {

		File audioFile = new File(args[0]);
		InputStream fis = new BufferedInputStream(new FileInputStream(audioFile));
		AudioFileFormat format = AudioSystem.getAudioFileFormat(fis);
		
		AudioInputStream is = null;
		
		is = new AudioInputStream(
				fis,
				format.getFormat(),
				audioFile.length());

		final int BASE_CLOCK = 3000000;
		final int POLL_CLOCK = 1378 * 3 / 2;
		float secsPerPoll = (float) POLL_CLOCK / BASE_CLOCK;
		
		ISettingsHandler settings = new BasicSettingsHandler();
		IProperty debug = settings.get(CassetteVoice.settingCassetteDebug);
		
		CassetteReader reader = new CassetteReader(is, debug, null);
		while (!reader.isDone()) {
			
			boolean val = reader.readBit(secsPerPoll);
			System.out.print(reader.getPosition() + ": ");
			System.out.println(val);
		}
	}

	
	private long position;
	private AudioInputStream is;
	private float mag;
	private int nch;
	private int sampSize;
	private boolean bigEndian;
	private boolean signed;
	private boolean endOfTape;
	private float min;
	private float max;
	private float dcOffset;
	private float prev;
	private int polarity;


	private float samplesFrac;
	private IProperty debug;

	/**
	 * @param is 
	 * @param debug 
	 * 
	 */
	public CassetteReader(AudioInputStream is, IProperty debug,
			ISoundOutput output) {
		this.is = is;
		this.debug = debug;
		
		// why doesn't Java provide a way to skip the header!?!?
		AudioFileFormat format;
		try {
			format = AudioSystem.getAudioFileFormat(is);
			if (format.getType() == Type.WAVE) {
				is.skip(44);
			}
		} catch (IOException e) {
		} catch (UnsupportedAudioFileException e) {
		}
		
		nch = is.getFormat().getChannels();
		sampSize = is.getFormat().getFrameSize() / nch;
		bigEndian = is.getFormat().isBigEndian();
		signed = is.getFormat().getEncoding() == Encoding.PCM_SIGNED;
		mag = 1.0f;
		min = 1f;
		max = -1f;
	}

	/**
	 * @return
	 */
	private boolean isDone() {
		try {
			return is.available() == 0;
		} catch (IOException e) {
			return true;
		}
	}

	/**
	 * @return
	 */
	public long getPosition() {
		return position;
	}
	

	/**
	 * @return
	 */
	public float readSample() {
		if (endOfTape) {
			return 0f;
		}
		try {
			float total = 0.f;
			byte[] buf = new byte[is.getFormat().getFrameSize()];
			int len = is.read(buf);
			if (len != buf.length) {
				if (!endOfTape) {
					mag = 0f;
					endOfTape = true;
				}
				return 0f;
			}
			position++;
			for (int ch = 0; ch < nch; ch++) {
				int bufIdx = ch * sampSize;
				int samp = 0;
				if (sampSize == 1) {
					samp = signed ? buf[bufIdx] : (byte) ((buf[bufIdx]) & 0xff);
					total += samp / 128f;
				}
				else if (sampSize == 2) {
					if (bigEndian)
						samp = ((buf[bufIdx] & 0xff) << 8) | (buf[bufIdx+1] & 0xff);
					else
						samp = ((buf[bufIdx+1] & 0xff) << 8) | (buf[bufIdx] & 0xff);
					if (signed)
						samp = (short) samp;
					total += samp / 32768f;
				}
				else if (sampSize == 4) {
					long lsamp;
					if (bigEndian)
						lsamp = ((buf[bufIdx+3] & 0xff) << 24) | ((buf[bufIdx+2] & 0xff) << 16) |
							((buf[bufIdx+1] & 0xff) << 8) | (buf[bufIdx] & 0xff);
					else
						lsamp = ((buf[bufIdx] & 0xff) << 24) | ((buf[bufIdx+1] & 0xff) << 16) |
							((buf[bufIdx+2] & 0xff) << 8) | (buf[bufIdx+3] & 0xff);
					if (signed)
						lsamp = (int) lsamp;

					total += lsamp / (float)0x80000000L;
				}
			}
			
			float samp = total / nch;
			
			float absSamp = Math.abs(samp);
			if (absSamp >= mag) {
				mag = (mag * 7 + absSamp) / 8f;
			} else {
				mag = (mag * 255f) / 256f;
			}
			
			if (absSamp < 0.001f)
				return 0f;
			
			return samp;
			
		} catch (IOException e) {
			return 0;
		}
	}
	
	public boolean readBit(float secs) {
		if (isEndOfTape()) {
			polarity = 0;
			return false;
		}
		
		float samplesf = (is.getFormat().getFrameRate() * secs);
		int samples = (int) samplesf;
		samplesFrac += (samplesf - samples);
		if (samplesFrac >= 1.0f) {
			samplesFrac -= 1.0f;
			samples++;
		}
		if (samples > 0) {
			polarity = scanPolarities(samples);
		}
		return polarity > 0;
	}
	/**
	 * Read the current polarity
	 * @param secs amount of time, in seconds, to poll
	 * @return
	 */
	protected int scanPolarities(int samples) {
		if (samples > 48) {
			samples = 48;
		}
		if (debug.getBoolean()) System.out.print(" @"+ samples+":");
		
		int newPolarity = polarity;
		
		while (samples-- > 0) {
			float samp = readSample();
			if (samp < min) {
				min = samp;
			} else if (samp > max) {
				max = samp;
			} 

			if (max > 0 && min < 0) {
				dcOffset = (dcOffset + (max + min) / 2) / 2;
			} else {
				dcOffset = 0f;
			}
			
			samp -= dcOffset;
			
			if (samp < 0) {
				if (prev < 0) {
					newPolarity = -1;
				}
			} else if (samp > 0) {
				if (prev > 0) {
					newPolarity = 1;
				}
			}
			
			max *= 0.99f;
			min *= 0.99f;
			
			prev = samp;
			
		}
		
		return newPolarity;
	}

	public boolean isEndOfTape() {
		return endOfTape;
	}
	/**
	 * 
	 */
	public void close() {
		try { is.close(); } catch (IOException e) { }
		endOfTape = true;
	}


}
