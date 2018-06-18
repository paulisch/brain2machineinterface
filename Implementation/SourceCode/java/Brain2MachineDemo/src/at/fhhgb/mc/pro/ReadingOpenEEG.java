package at.fhhgb.mc.pro;

import at.fhhgb.mc.pro.gesture.LookGesture;
import at.fhhgb.mc.pro.gesture.LookGestureEvent;
import at.fhhgb.mc.pro.gesture.LookGestureEventListener;
import at.fhhgb.mc.pro.reader.OpenEEGReader;

public class ReadingOpenEEG {
	
	public static void main(String[] _args) {
		OpenEEGReader reader = new OpenEEGReader("COM1");
		LookGesture gesture = new LookGesture();
		gesture.addGestureEventListener(new LookGestureEventListener() {
			@Override
			public void onLookTimeOut(LookGestureEvent _evt) {				
			}
			@Override
			public void onLook(LookGestureEvent _evt) {
				System.out.println("Look " + _evt.getDirection());
			}
		});
		reader.addGesture(gesture);
		reader.connect();
	}
	
}