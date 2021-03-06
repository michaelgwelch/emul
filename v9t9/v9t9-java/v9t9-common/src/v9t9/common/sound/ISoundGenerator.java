/*
  ISoundGenerator.java

  (c) 2011-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.sound;

import javax.sound.sampled.AudioFormat;

import v9t9.common.settings.SettingSchema;
import ejs.base.sound.ISoundOutput;
import ejs.base.sound.ISoundVoice;

/**
 * @author ejs
 *
 */
public interface ISoundGenerator {

	String getName();
	
	SettingSchema getRecordingSettingSchema();
	boolean isSilenceRecorded();
	
	AudioFormat getAudioFormat();
	ISoundVoice[] getSoundVoices();
	//void tick();
	
	void generate(ISoundOutput output, int pos, int total);
	/**
	 * @param output
	 */
	void configureSoundOutput(ISoundOutput output);
	/**
	 * @param iSoundOutput
	 * @param pos
	 * @param total
	 */
	void flushAudio(ISoundOutput iSoundOutput, int pos, int total);
	/**
	 * @return
	 */
}
