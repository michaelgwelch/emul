/**
 * 
 */
package v9t9.engine.sound;

import v9t9.common.machine.IRegisterAccess.IRegisterWriteListener;
import ejs.base.settings.ISettingSection;
import ejs.base.utils.HexUtils;
import ejs.base.utils.ListenerList;

/**
 * @author ejs
 *
 */
public abstract class BaseEnhancedClockedVoice extends BaseClockedVoice implements IEnhancedVoice {
	protected byte[] effectValues;
	protected final int numEffects;
	protected int baseEffectReg;

	/**
	 * @param id
	 * @param name
	 * @param listeners
	 */
	public BaseEnhancedClockedVoice(String id, String name,
			ListenerList<IRegisterWriteListener> listeners,
			int numEffects) {
		super(id, name, listeners);
		this.numEffects = numEffects;
		this.effectValues = new byte[numEffects];
	}

	protected int doInitEffectRegisters(int baseReg) {
		baseEffectReg = baseReg;
		for (int i = 0; i < numEffects; i++) {
			String regStr = "#" + (numEffects < 16 ? "" + Integer.toHexString(i) : HexUtils.toHex2(i));
			register(baseEffectReg + i,
					getId() + ":E" + regStr,
					getName() + " " + regStr);
		}
		return numEffects;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.sound.IEnhancedVoice#setEffect(int, int)
	 */
	@Override
	public void setEffect(int effect, byte value) {
		effectValues[effect] = value;
		fireRegisterChanged(baseEffectReg + effect, value);
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.sound.IEnhancedVoice#getEffectValue(int)
	 */
	@Override
	public byte getEffectValue(int effect) {
		return effectValues[effect];
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.sound.BaseClockedVoice#loadState(ejs.base.settings.ISettingSection)
	 */
	@Override
	public void loadState(ISettingSection settings) {
		if (settings == null) return;
		super.loadState(settings);
		
		for (int i = 0; i < numEffects; i++) {
			effectValues[i] = (byte) settings.getInt("Effect" + i);
		}
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.sound.BaseClockedVoice#saveState(ejs.base.settings.ISettingSection)
	 */
	@Override
	public void saveState(ISettingSection settings) {
		super.saveState(settings);
		
		for (int i = 0; i < numEffects; i++) {
			settings.put("Effect" + i, effectValues[i]);
		}

	}
}
