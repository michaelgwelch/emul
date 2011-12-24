/**
 * 
 */
package v9t9.audio.sound;

import java.util.HashMap;
import java.util.Map;

import v9t9.common.machine.IMachine;
import v9t9.common.sound.TMS9919BConsts;
import static v9t9.common.sound.TMS9919BConsts.*;

/**
 * Controller for the TMS9919(B) sound chip.
 * </pre>
 * @author ejs
 *
 */
public class SoundTMS9919BGenerator extends SoundTMS9919Generator {

	protected Map<Integer, EnhancedVoice> regIdToVoiceEffects; 
	protected Map<Integer, Integer> regIdEffectId; 
	
	public SoundTMS9919BGenerator(IMachine machine, String name, int regBase) {
		super(machine, name, regBase);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.audio.sound.SoundTMS9919Generator#doInitVoices(java.lang.String, int)
	 */
	@Override
	protected int doInitVoices(String name, int regBase) {

		regIdToVoiceEffects = new HashMap<Integer, EnhancedVoice>();
		regIdEffectId = new HashMap<Integer, Integer>();
		
		for (int i = VOICE_TONE_0; i <= VOICE_TONE_2; i++) {
			EnhancedToneGeneratorVoice ev = new EnhancedToneGeneratorVoice(name, i);
			soundVoicesList.add(ev);
			regBase += setupEnhancedToneVoice(regBase, i, ev);
		}
		
		EnhancedNoiseGeneratorVoice nv = new EnhancedNoiseGeneratorVoice(name,
				(ClockedSoundVoice) soundVoicesList.get(soundVoicesList.size() - 1));
		soundVoicesList.add(nv);
		regBase += setupEnhancedNoiseVoice(regBase, nv);
		
		AudioGateSoundVoice av = new AudioGateSoundVoice(name);
		soundVoicesList.add(av);
		regBase += setupAudioGateVoice(regBase, av);
		
		return regBase;
	}
	

	protected int setupEnhancedToneVoice(int regBase, int num, EnhancedToneGeneratorVoice voice) {
		int cnt = super.setupToneVoice(regBase, num, voice);
		
		for (int e = 0; e < TMS9919BConsts.CMD_NUM_EFFECTS; e++) {
			int rnum = regBase + TMS9919BConsts.REG_COUNT_TONE + e;
			regIdToVoiceEffects.put(rnum, voice);
			regIdEffectId.put(rnum, e);
		}

		return cnt + TMS9919BConsts.CMD_NUM_EFFECTS;
	}

	protected int setupEnhancedNoiseVoice(int regBase, EnhancedNoiseGeneratorVoice voice) {
		int cnt = super.setupNoiseVoice(regBase, voice);
		
		for (int e = 0; e < TMS9919BConsts.CMD_NUM_EFFECTS; e++) {
			int rnum = regBase + TMS9919BConsts.REG_COUNT_NOISE + e;
			regIdToVoiceEffects.put(rnum, voice);
			regIdEffectId.put(rnum, e);
		}

		return cnt + TMS9919BConsts.CMD_NUM_EFFECTS;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.machine.IRegisterAccess.IRegisterWriteListener#registerChanged(int, int)
	 */
	@Override
	public void registerChanged(int reg, int val) {
		
		EnhancedVoice ev = regIdToVoiceEffects.get(reg);
		if (ev != null) {
			int eff = regIdEffectId.get(reg);
			switch (eff) {
			case CMD_RESET:
				ev.getEffectsController().reset();
				break;
			case CMD_RELEASE:
				ev.getEffectsController().startRelease();
				break;
			case CMD_ENVELOPE:
				ev.getEffectsController().setSustain(val & 0xf);
				break;
			case CMD_ENV_ATTACK_DECAY:
				ev.getEffectsController().setADSR(
						OP_ATTACK, (val >> 4) & 0xf);
				ev.getEffectsController().setADSR(
						OP_DECAY, val & 0xf);
				break;
			case CMD_ENV_HOLD_RELEASE:
				ev.getEffectsController().setADSR(
						OP_HOLD, (val >> 4) & 0xf);
				ev.getEffectsController().setADSR(
						OP_RELEASE, val & 0xf);
				break;
			case CMD_VIBRATO:
				ev.getEffectsController().setVibrato(
						(val >> 4) & 0xf, val & 0xf);
				break;
			case CMD_TREMOLO:
				ev.getEffectsController().setTremolo(
						(val >> 4) & 0xf, val & 0xf);
				break;
			case CMD_WAVEFORM:
				ev.getEffectsController().setWaveform(val & 0xf);
				break;
			case CMD_SWEEP_PROPORTION: {
				int target = ((ClockedSoundVoice) ev).getClock();
				if (val > 0)
					target += (target * val / 127);
				else
					target -= (target * -val / 128);
				ev.getEffectsController().setSweepTarget(target);
				break;
			}
			case CMD_SWEEP_TIME: {
				int clocks = (val & 0xff) * ((ClockedSoundVoice)ev).soundClock * 64 / 255;
				ev.getEffectsController().setSweepTime(clocks);
				break;
			}
			case CMD_BALANCE:
				((SoundVoice)ev).setBalance((byte) val);
				break;
			}
			
			if (soundHandler != null)
				soundHandler.generateSound();

		} else {
			super.registerChanged(reg, val);

			ClockedSoundVoice cv = regIdToVoiceAtten.get(reg);
			if (cv instanceof EnhancedVoice) {
				ev = (EnhancedVoice) cv;
				if ((val & 0x80) == 0x80) { 
					byte vol = cv.getVolume();
					if (vol == 0 || (val & 0x90) == 0x80)
						ev.getEffectsController().stopEnvelope();
					else
						ev.getEffectsController().updateVoice();
				}
			}

		}
	}
}
