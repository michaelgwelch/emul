/**
 * 
 */
package v9t9.emulator.clients.builtin.sound;

import javax.sound.sampled.AudioFormat;

import org.ejs.emul.core.sound.ISoundListener;
import org.ejs.emul.core.sound.ISoundOutput;
import org.ejs.emul.core.sound.ISoundVoice;
import org.ejs.emul.core.sound.SoundFactory;
import org.ejs.emul.core.sound.ui.SoundRecordingHelper;
import org.ejs.emul.core.utils.ISettingListener;
import org.ejs.emul.core.utils.Setting;

import v9t9.emulator.Machine;
import v9t9.emulator.clients.builtin.SoundProvider;
import v9t9.emulator.hardware.sound.SoundVoice;
import v9t9.engine.SoundHandler;

/**
 * Handle sound generation for output with Java APIs
 * @author ejs
 *
 */
public class JavaSoundHandler implements SoundHandler {
	public static Setting settingPlaySound = new Setting("PlaySound", new Boolean(true));
	public static Setting settingRecordSoundOutputFile = new Setting("RecordSoundOutputFile", (String)null);
	public static Setting settingRecordSpeechOutputFile = new Setting("RecordSpeechOutputFile", (String)null);

	private SoundRecordingHelper soundRecordingHelper;
	private SoundRecordingHelper speechRecordingHelper;
	private AudioFormat soundFormat;
	private ISoundOutput output;
	private SoundProvider sound;
	private int lastUpdatedPos;
	private int soundFramesPerTick;
	private AudioFormat speechFormat;
	private ISoundOutput speechOutput;
	private SpeechVoice speechVoice;
	private ISoundListener audio;
	private ISoundListener speechAudio;
	
	static class SpeechVoice implements ISoundVoice {
		
		private short sample;

		public void setSoundClock(int soundClock) {
		}
		
		public void reset() {
			sample = 0;
		}
		
		public boolean isActive() {
			return true;
		}
		
		public void generate(float[] soundGeneratorWorkBuffer, int from, int to) {
			float delta = sample / 32768.f;
			while (from < to)
				soundGeneratorWorkBuffer[from++] += delta;
		}
		
		public void setSample(short sample) {
			this.sample = sample;
		}
	}
	public JavaSoundHandler(final Machine machine) {

		soundFormat = new AudioFormat(55930, 16, 2, true, false);
		speechFormat = new AudioFormat(8000, 16, 1, true, false);
		
		output = SoundFactory.createSoundOutput(soundFormat, machine.getCpuTicksPerSec());
		speechOutput = SoundFactory.createSoundOutput(speechFormat, machine.getCpuTicksPerSec());
		
		audio = SoundFactory.createAudioListener();
		speechAudio = SoundFactory.createAudioListener();
		output.addListener(audio);
		speechOutput.addListener(speechAudio);
		
		speechVoice = new SpeechVoice();
		
		soundRecordingHelper = new SoundRecordingHelper(output, settingRecordSoundOutputFile, "sound");
		speechRecordingHelper = new SoundRecordingHelper(speechOutput, settingRecordSpeechOutputFile, "speech");
		
		sound = machine.getSound();

		// frames in ALSA means samples per channel, but raw freq in javax
		soundFramesPerTick = (int) ((soundFormat.getFrameRate() * soundFormat.getChannels() 
				+ machine.getCpuTicksPerSec() - 1) / machine.getCpuTicksPerSec());
		
		settingPlaySound.addListener(new ISettingListener() {

			public void changed(Setting setting, Object oldValue) {
				toggleSound(setting.getBoolean());
			}
			
		});
		
		toggleSound(settingPlaySound.getBoolean());
	}

	public void toggleSound(boolean enabled) {
		if (enabled) {
			output.start();
			speechOutput.start();
		} else {
			output.stop();
			speechOutput.stop();
		}
	}

	public synchronized void updateVoice(int pos, int total) {
		if (total == 0)
			return;
		
		int currentPos = (int) ((long) (pos * soundFramesPerTick * soundFormat.getChannels() + total - 1 ) / total);
		if (currentPos < 0)
			currentPos = 0;
		updateSoundGenerator(lastUpdatedPos, currentPos);
		lastUpdatedPos = currentPos;
	}

	protected synchronized void updateSoundGenerator(int from, int to) {
		if (to > soundFramesPerTick)
			to = soundFramesPerTick;
		if (from >= to)
			return;

		SoundVoice[] vs = sound.getSoundVoices();

		int samples = to - from;
		samples -= samples % soundFormat.getChannels();
		output.generate(vs, to - from);
	}

	public void dispose() {
		toggleSound(false);

		if (soundRecordingHelper != null) {
			soundRecordingHelper.dispose();
			soundRecordingHelper = null;
		}
		if (speechRecordingHelper != null) {
			speechRecordingHelper.dispose();
			speechRecordingHelper = null;
		}
		if (output != null) {
			output.dispose();
			output = null;
		}
		
		if (speechOutput != null) {
			speechOutput.dispose();
			speechOutput = null;
		}
	}

	public synchronized void speech(short sample) {
		speechVoice.setSample(sample);
		speechOutput.generate(new ISoundVoice[] { speechVoice }, 1);
	}

	public synchronized void flushAudio(int pos, int limit) {
		if (output != null) {
			// hmm, it would be nice if the audio gate could work perfectly,
			// but the calculations of its data have assumed the "limit" would
			// match reality, when sometimes the tick comes earlier or later.
			// int length = lastUpdatedPos;
			/*
			 * int length = (int) ((long)pos soundGeneratorWaveForm.length / limit);
			 * if (length < lastUpdatedPos) length = lastUpdatedPos; if (length >
			 * soundGeneratorWaveForm.length) length =
			 * soundGeneratorWaveForm.length; byte[] part = new byte[length];
			 * System.arraycopy(soundGeneratorWaveForm, 0, part, 0, length);
			 * Arrays.fill(soundGeneratorWaveForm, (byte) 0); soundQueue.add(new
			 * AudioChunk(part, null, null));
			 */
	
			updateSoundGenerator(lastUpdatedPos, soundFramesPerTick);
			lastUpdatedPos = 0;
	
			output.flushAudio();
		}
		
		if (speechOutput != null) {
			speechOutput.flushAudio();
		}
	}

	/**
	 * @return the soundRecordingHelper
	 */
	public SoundRecordingHelper getSoundRecordingHelper() {
		return soundRecordingHelper;
	}
	
	/**
	 * @return the speechRecordingHelper
	 */
	public SoundRecordingHelper getSpeechRecordingHelper() {
		return speechRecordingHelper;
	}
}
