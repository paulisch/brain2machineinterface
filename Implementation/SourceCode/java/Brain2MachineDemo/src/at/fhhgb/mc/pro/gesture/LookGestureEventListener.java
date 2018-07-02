package at.fhhgb.mc.pro.gesture;

/**
 * An interface representing an event listener for listening to look gestures.
 * @author Boris Fuchs, Paul Schmutz
 */
public interface LookGestureEventListener extends GestureEventListener {
	public void onLook(LookGestureEvent _evt);
}