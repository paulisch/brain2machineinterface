package at.fhhgb.mc.pro.gesture;

/**
 * A class representing a look gesture event.
 * @author Boris Fuchs, Paul Schmutz
 */
public class LookGestureEvent extends GestureEvent {
	
	/**
	 * The direction of the look gesture.
	 */
	private LookGestureDirection mDirection;
	
	/**
	 * Default constructor.
	 * @param _direction the required direction of the look gesture 
	 */
	public LookGestureEvent(LookGestureDirection _direction) {
		if (_direction == null) {
			throw new IllegalArgumentException("Constructor parameter _direction must not be null in class " + this.getClass().getName() + "!");
		}
		mDirection = _direction;
	}

	/**
	 * Gets the look gesture direction.
	 * @return the look gesture direction
	 */
	public LookGestureDirection getDirection() {
		return mDirection;
	}
}