package at.fhhgb.mc.pro.gesture;

public interface LookGestureEventListener extends GestureEventListener {
	public void onLook(LookGestureEvent _evt);
	public void onLookTimeOut(LookGestureEvent _evt);
}