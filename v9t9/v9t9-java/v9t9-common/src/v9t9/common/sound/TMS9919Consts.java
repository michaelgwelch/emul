/**
 * 
 */
package v9t9.common.sound;

/**
 * @author ejs
 *
 */
public class TMS9919Consts {
	public static final String GROUP_NAME = "TMS 9919";

//	/** low 4 bits [1vv0yyyy] */
//	final public static int OPERATION_FREQUENCY_LO = 0;
//	/** for noise  [11100xyy] */
//	final public static int OPERATION_NOISE_CONTROL = 0;
//	/** These are used as an index into the registers for each REG_BASE_VOICE_xxx and the REG_BASE_VOICE_NOISE group */
//	final public static int 
//		OPERATION_FREQUENCY_HI = 1,		/* hi 6 bits  [00yyyyyy] */
//		OPERATION_ATTENUATION = 2		/* low 4 bits [1vv1yyyy] */
//	;
//	/** These are used as an index into the registers for each REG_BASE_VOICE_xxx and the REG_BASE_VOICE_NOISE group */
//	final public static int 
//		OPERATION_FREQUENCY_HI = 1,		/* hi 6 bits  [00yyyyyy] */
//		OPERATION_ATTENUATION = 2		/* low 4 bits [1vv1yyyy] */
//	;

	/** Tone voice frequency in Hz */
	final public static int REG_OFFS_PERIOD = 0;
	/** Voice attenuation: 0=loudest, 15=silent/off */
	final public static int REG_OFFS_ATTENUATION = 1;
	/** Noise control for noise voice: (xyy); x=0 for periodic, 1 for white noise; y=0-2 for fixed freq */
	final public static int REG_OFFS_NOISE_CONTROL = 2;
	
	/** State of audio gate (0=off, 1=on) */
	final public static int REG_OFFS_AUDIO_GATE = 0;
	
	final public static int REG_COUNT_TONE = 2;
	final public static int REG_COUNT_NOISE = 3;
	final public static int REG_COUNT_AUDIO_GATE = 1;

	/**
	 *	Masks for the OPERATION_CONTROL byte for VOICE_NOISE
	 */
	public final static int NOISE_PERIODIC = 0,
		NOISE_WHITE = 0x4
	;
	
	public final static int NOISE_PERIOD_FIXED_0 = 0,
		NOISE_PERIOD_FIXED_1 = 1,
		NOISE_PERIOD_FIXED_2 = 2,
		NOISE_PERIOD_VARIABLE = 3;
	;

	public static final int  noise_period[] = 
	{
		16,
		32, 
		64, 
		0 						/* determined by VOICE_TONE_2 */
	};

	public static int periodToHertz(int p) {
		return ((p) > 1 ? (111860 / (p)) : 55930);
	}
	public static int period16ToHertz(int p) {
		return (int) ((p) > 1 ? ((long)111860 * 55930 / (p)) : 55930 * 55930);
	}

}
