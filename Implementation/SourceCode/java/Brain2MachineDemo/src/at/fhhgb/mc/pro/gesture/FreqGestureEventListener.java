package at.fhhgb.mc.pro.gesture;

public interface FreqGestureEventListener extends GestureEventListener {
	public void onFreqGestureEventStart(FreqGestureEvent _evt);
	public void onFreqGestureEventComplete(FreqGestureEvent _evt);
}