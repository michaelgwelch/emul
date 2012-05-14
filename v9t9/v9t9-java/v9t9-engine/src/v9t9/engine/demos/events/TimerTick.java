/**
 * 
 */
package v9t9.engine.demos.events;

import v9t9.common.demo.IDemoEvent;
import v9t9.common.machine.IMachine;

public class TimerTick implements IDemoEvent {

	private final long elapsedTime;

	/**
	 * @param elapsedTime
	 */
	public TimerTick(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoEvent#execute(v9t9.common.machine.IMachine)
	 */
	@Override
	public void execute(IMachine machine) {
		// contribute time, so sound, etc. will proceed
		machine.getCpu().tick();
	}
	
	/**
	 * @return the elapsedTime
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}
}