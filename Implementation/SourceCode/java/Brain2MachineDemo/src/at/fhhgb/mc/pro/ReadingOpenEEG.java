package at.fhhgb.mc.pro;

import java.util.Timer;
import java.util.TimerTask;

import at.fhhgb.mc.pro.gesture.FreqGesture;
import at.fhhgb.mc.pro.gesture.FreqGestureEvent;
import at.fhhgb.mc.pro.gesture.FreqGestureEventListener;
import at.fhhgb.mc.pro.gesture.LookGesture;
import at.fhhgb.mc.pro.gesture.LookGestureEvent;
import at.fhhgb.mc.pro.gesture.LookGestureEventListener;
import at.fhhgb.mc.pro.gesture.SlopeGesture;
import at.fhhgb.mc.pro.gesture.SlopeGestureEvent;
import at.fhhgb.mc.pro.gesture.SlopeGestureEventListener;
import at.fhhgb.mc.pro.reader.OpenEEGReader;

public class ReadingOpenEEG {
	
	private static long timeMouth = -1;
	private static boolean preventLook = false;
	private static boolean isBiting = false;
	
	private static Timer timer = new Timer();
	private static TimerTask task = null;
	
	public static void main(String[] _args) {		
		OpenEEGReader reader = new OpenEEGReader("COM1");
		LookGesture lookGesture = new LookGesture();
		lookGesture.addGestureEventListener(new LookGestureEventListener() {
			@Override
			public void onLookTimeOut(LookGestureEvent _evt) {				
			}
			@Override
			public void onLook(LookGestureEvent _evt) {
				if (!isBiting && !preventLook && System.currentTimeMillis() - timeMouth > 250) {
					if (task == null) {
						task = new TimerTask() {
							@Override
							public void run() {
								System.out.println("Look " + _evt.getDirection());
								task = null;
							}
						};
						timer.schedule(task, 350);
					}
				}
			}
		});
		
		FreqGesture preventLookGesture = new FreqGesture(20, 128, 1, 0.6f, FreqGesture.THRESHOLD_PREVENT_LOOK_1_SEC_SAMPLES_CH1, FreqGesture.THRESHOLD_PREVENT_LOOK_1_SEC_SAMPLES_CH2);
		preventLookGesture.addGestureEventListener(new FreqGestureEventListener() {
			@Override
			public void onFreqGestureEventStart(FreqGestureEvent _evt) {
				System.out.println("Prevent look start");
				preventLook = true;
				if (task != null) {
					task.cancel();
				}
			}
			@Override
			public void onFreqGestureEventComplete(FreqGestureEvent _evt) {
				System.out.println("Prevent look end");
				preventLook = false;
				if (task != null) {
					task.cancel();
				}
			}
		});
		
		FreqGesture biteFreqGesture = new FreqGesture(20, 128, 1, 0.6f, FreqGesture.THRESHOLD_BITE_1_SEC_SAMPLES_CH1, FreqGesture.THRESHOLD_BITE_1_SEC_SAMPLES_CH2);
		biteFreqGesture.addGestureEventListener(new FreqGestureEventListener() {
			@Override
			public void onFreqGestureEventStart(FreqGestureEvent _evt) {
				System.out.println("Start bite");
				isBiting = true;
				if (task != null) {
					task.cancel();
				}
			}
			@Override
			public void onFreqGestureEventComplete(FreqGestureEvent _evt) {
				System.out.println("End bite");
				isBiting = false;
				if (task != null) {
					task.cancel();
				}
			}
		});
		
		SlopeGesture mouthCloseGesture = new SlopeGesture(SlopeGesture.THRESHOLD_CH1_HIGH, SlopeGesture.THRESHOLD_CH1_LOW, SlopeGesture.THRESHOLD_CH2_HIGH, SlopeGesture.THRESHOLD_CH2_LOW, SlopeGesture.SLOPE_SECONDS, SlopeGesture.SAFE_OFFSET_SECONDS, SlopeGesture.SLOPE_MIN, SlopeGesture.SLOPE_MAX);
		mouthCloseGesture.addGestureEventListener(new SlopeGestureEventListener() {
			@Override
			public void onSlopeDetected(SlopeGestureEvent _evt) {
				if (!isBiting) {
					System.out.println("Mouth closed");
					timeMouth = System.currentTimeMillis();
					if (task != null) {
						task.cancel();
					}
				}
			}
		});
		
		/*FreqGesture smileFreqGesture = new FreqGesture(20, 128, 1, 0.6f, FreqGesture.THRESHOLD_SMILE_1_SEC_SAMPLES_CH1, FreqGesture.THRESHOLD_SMILE_1_SEC_SAMPLES_CH2);
		smileFreqGesture.addGestureEventListener(new FreqGestureEventListener() {
			@Override
			public void onFreqGestureEventStart(FreqGestureEvent _evt) {
				System.out.println("Start smile");
				isSmiling = true;
			}
			@Override
			public void onFreqGestureEventComplete(FreqGestureEvent _evt) {
				System.out.println("End smile");
				isSmiling = false;
			}
		});*/
		
		//reader.addGesture(mouthCloseGesture);
		reader.addGesture(lookGesture);
		reader.addGesture(preventLookGesture);
		reader.addGesture(biteFreqGesture);
		//reader.addGesture(smileFreqGesture);
		reader.connect();
	}
}