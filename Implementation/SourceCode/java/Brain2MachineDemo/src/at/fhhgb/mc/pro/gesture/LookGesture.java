package at.fhhgb.mc.pro.gesture;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import at.fhhgb.mc.pro.reader.OpenEEGReader;

/**
 * A class representing a look gesture (moving eyes).
 * @author Boris Fuchs, Paul Schmutz
 */
public class LookGesture extends Gesture<LookGestureEventListener> {
	
	/**
	 * Predefined constant for the threshold of look gestures on channel 1 high.
	 */
	private static final double THRESHOLD_CH1_HIGH = 0.57;
	
	/**
	 * Predefined constant for the threshold of look gestures on channel 1 low.
	 */
	private static final double THRESHOLD_CH1_LOW = 0.44;
	
	
	/**
	 * Predefined constant for the threshold of look gestures on channel 2 high.
	 */
	private static final double THRESHOLD_CH2_HIGH = 0.9;
	
	/**
	 * Predefined constant for the threshold of look gestures on channel 2 low.
	 */
	private static final double THRESHOLD_CH2_LOW = 0.1;
	
	
	/**
	 * Time in seconds that has to pass before the next look gesture can be detected.
	 */
	private static final double SAFE_OFFSET_SECONDS = 0.25;
	
	/**
	 * The duration of the slope that is characteristic for a look gesture.
	 */
	private static final double SLOPE_SECONDS = 0.08;
	
	/**
	 * The duration of being able to look back to center without triggering another look gesture.
	 */
	private static final double LOOK_BACK_TO_CENTER_OFFSET = 1.0;
	
	/**
	 * The maximum slope of the look gesture.
	 */
	private static final double SLOPE_MAX = 4;
	
	private long mLastDetectionSample = -1;
	private long mLastSample = -1;
	private LookGestureDirection mLastDirection = null;

	@Override
	public void handleNextSample(OpenEEGReader _reader) {
		int safeOffset = (int)(SAFE_OFFSET_SECONDS * _reader.getSampleRate());
		int slopeSamples = (int)(SLOPE_SECONDS * _reader.getSampleRate());
		int backToCenterSamples = (int)(LOOK_BACK_TO_CENTER_OFFSET * _reader.getSampleRate());
		int i = _reader.getChannelDataPointer();
		double[][] buffer = _reader.getChannelData();
		
		if (_reader.getChannelDataSampleCount() <= slopeSamples) {
			return;
		}
		
		int max = mLastSample == -1 ? 0 : (int)(_reader.getChannelDataSampleCount() - mLastSample);
		
		for (int idx = max - 1; idx>=0; idx--) {
			int actualIdx = i - idx;
			actualIdx = actualIdx < 0 ? buffer[0].length + actualIdx : actualIdx;
			
			double ch1Val = buffer[0][actualIdx];
			double ch2Val = buffer[1][actualIdx];
			
			boolean fallingSignal = (ch1Val <= THRESHOLD_CH1_LOW && ch2Val <= THRESHOLD_CH2_LOW);
			boolean risingSignal = (ch1Val >= THRESHOLD_CH1_HIGH && ch2Val >= THRESHOLD_CH2_HIGH);
			boolean detected = fallingSignal != risingSignal;
			
			if (detected && (mLastDetectionSample == -1 || (_reader.getChannelDataSampleCount() - idx - mLastDetectionSample) >= safeOffset)) {			
				SimpleRegression regression = new SimpleRegression();
				double[][] regArray = new double[slopeSamples][2];
				int regIdx = 0;
				for(int idx2 = i-slopeSamples; idx2 < i; idx2++) {
					int actualIdx2 = idx2 < 0 ? buffer[0].length + idx2 : idx2;
					regArray[regIdx][0] = (double)regIdx / _reader.getSampleRate();
					regArray[regIdx][1] = buffer[1][actualIdx2];
					regIdx++;
				}
				regression.addData(regArray);
				double slope = regression.getSlope();
				
				if (((fallingSignal && slope < 0) || (risingSignal && slope > 0)) && Math.abs(slope) <= SLOPE_MAX) {					
					LookGestureDirection dir = fallingSignal ? LookGestureDirection.RIGHT : LookGestureDirection.LEFT;
					
					if (!(mLastDirection != dir && (_reader.getChannelDataSampleCount() - mLastDetectionSample) <= backToCenterSamples)) {
						LookGestureEvent evt = new LookGestureEvent(dir);
						notifyLook(evt);
						
						mLastDetectionSample = _reader.getChannelDataSampleCount() - idx;
						mLastDirection = dir;
					}
				}
			}
		}
		
		mLastSample = _reader.getChannelDataSampleCount();
	}
	
	private void notifyLook(LookGestureEvent _evt) {
		for (LookGestureEventListener listener : mListeners) {
			listener.onLook(_evt);
		}
	}
}