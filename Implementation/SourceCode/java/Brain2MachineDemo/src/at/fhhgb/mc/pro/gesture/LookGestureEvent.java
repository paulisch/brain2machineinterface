package at.fhhgb.mc.pro.gesture;

public class LookGestureEvent extends GestureEvent {
	
	private LookGestureDirection mDirection;
	
	public LookGestureEvent(LookGestureDirection _direction) {
		if (_direction == null) {
			throw new IllegalArgumentException("Constructor parameter _direction must not be null in class " + this.getClass().getName() + "!");
		}
		setDirection(_direction);
	}

	public LookGestureDirection getDirection() {
		return mDirection;
	}

	public void setDirection(LookGestureDirection _direction) {
		this.mDirection = _direction;
	}
}