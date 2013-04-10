/**
 * 
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


/**
 * @author ejs
 *
 */
public class CassetteReader {
	static final boolean DEBUG = false;


	/**
	 * 
	 */
	private static final float MIN_MAG = 0.1f;

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
		
		CassetteReader reader = new CassetteReader(is);
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
	private int lastPolarity;
	private float min;
	private float max;
	private float dcOffset;
	private int locMaxima;
	private float prevPeak;
	private float prevRevPeak;
	private int polarity;

	/**
	 * @param is 
	 * 
	 */
	public CassetteReader(AudioInputStream is) {
		this.is = is;
		
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
		sampSize = is.getFormat().getFrameSize();
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
		if (endOfTape)
			return 0f;
		try {
			float total = 0.f;
			byte[] buf = new byte[is.getFormat().getFrameSize()];
			for (int ch = 0; ch < nch; ch++) {
				int samp = 0;
				int len = is.read(buf);
				if (len != buf.length) {
					if (!endOfTape) {
						endOfTape = true;
					}
				}
				if (sampSize == 1) {
					samp = signed ? buf[0] : (buf[0] - 0x80) & 0xff;
					total += samp / 128f;
				}
				else if (sampSize == 2) {
					if (bigEndian)
						samp = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
					else
						samp = ((buf[1] & 0xff) << 8) | (buf[0] & 0xff);
					if (signed)
						samp = (short) samp;
					total += samp / 32768f;
				}
				else if (sampSize == 4) {
					long lsamp;
					if (bigEndian)
						lsamp = ((buf[3] & 0xff) << 24) | ((buf[2] & 0xff) << 16) |
							((buf[1] & 0xff) << 8) | (buf[1] & 0xff);
					else
						lsamp = ((buf[0] & 0xff) << 24) | ((buf[1] & 0xff) << 16) |
							((buf[2] & 0xff) << 8) | (buf[3] & 0xff);
					if (signed)
						lsamp = (int) lsamp;

					total += lsamp / (float)0x80000000L;
				}
				position++;
			}
			
			float samp = total / nch;
			
			float absSamp = Math.abs(samp);
			if (absSamp >= mag) {
				mag = (mag * 7 + absSamp) / 8f;
			} else {
				mag = (mag * 255f) / 256f;
			}
			
			if (mag >= MIN_MAG) {
				return samp;
			}
			return 0f;
			
		} catch (IOException e) {
			return 0;
		}
	}
	
	public boolean readBit(float secs) {
		int samples = (int) (is.getFormat().getFrameRate() * secs);
		if (samples > 0) {
			/*int changes =*/ scanPolarities(samples);
		}
		return polarity > 0;
		
//		if (lastPolarity == 0) {
//			lastPolarity = polarity;
//			return 0;
//		}
//		else if (polarity != lastPolarity) {
//			lastPolarity = polarity;
//			return 1;
//		}
//		return 0;
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
		if (DEBUG) System.out.print(" @"+ samples+":");
		
		int changes = 0;
		polarity = lastPolarity;
		
		while (samples-- > 0) {
			float samp = readSample();
			if (samp < min) {
				min = samp;
			} else if (samp > max) {
				max = samp;
			} 

			if (max > 0 && min < 0)
				dcOffset = (max + min) / 2;
			
			samp -= dcOffset / 16;
//			if (Math.abs(samp) < MIN_MAG)
//				continue;
			
			//long pos = reader.getPosition();
			
			boolean newPeak = false;
			if (min < 0 && samp < 0 && samp < prevPeak && prevRevPeak >= 0) {
				locMaxima++;
				newPeak = locMaxima >= 2;
				prevPeak = samp;
			} else if (max > 0 && samp > 0 && samp > prevPeak && prevRevPeak <= 0) {
				locMaxima++;
				newPeak = locMaxima >= 2;
				prevPeak = samp;
			} 
			if (newPeak) {
				polarity = samp < 0 ? -1 : 1;
				if (polarity != lastPolarity) {
					lastPolarity = polarity;
					changes++;
				}
				
				locMaxima = 0;
				prevRevPeak = samp;
				prevPeak = 0;
			}
			max *= 0.99f;
			min *= 0.99f;
			
		}
		return changes;
	}

	public boolean isEndOfTape() {
		return endOfTape;
	}
	/**
	 * 
	 */
	public void close() {
		try { is.close(); } catch (IOException e) { }
	}


}
