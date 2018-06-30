package at.fhhgb.mc.pro.gesture;

/**
 * An interface providing methods for a frequency gesture event listeners.
 * @author Boris Fuchs, Paul Schmutz
 */
public interface FreqGestureEventListener extends GestureEventListener {
	/**
	 * Method that is called when frequency strength exceeds a certain threshold.
	 * @param _evt the event being triggered
	 */
	public void onFreqGestureEventStart(FreqGestureEvent _evt);
	
	/**
	 * Method that is called when frequency strength falls under a certain threshold again.
	 * @param _evt the event being triggered
	 */
	public void onFreqGestureEventComplete(FreqGestureEvent _evt);
}