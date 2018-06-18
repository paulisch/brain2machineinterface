package at.fhhgb.mc.pro.gesture;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import at.fhhgb.mc.pro.reader.OpenEEGReader;

public class LookGesture extends Gesture<LookGestureEventListener> {
	
	private static final double THRESHOLD_CH1_HIGH = 0.57;
	private static final double THRESHOLD_CH1_LOW = 0.44;
	
	private static final double THRESHOLD_CH2_HIGH = 0.9;
	private static final double THRESHOLD_CH2_LOW = 0.1;
	
	private static final double SAFE_OFFSET_SECONDS = 0.25;
	private static final double SLOPE_SECONDS = 0.08;
	private static final double LOOK_BACK_TO_CENTER_OFFSET = 1.0;
	
	private long mLastDetectionSample = -1;
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
		
		double ch1Val = buffer[0][i];
		double ch2Val = buffer[1][i];
		
		boolean fallingSignal = (ch1Val <= THRESHOLD_CH1_LOW && ch2Val <= THRESHOLD_CH2_LOW);
		boolean risingSignal = (ch1Val >= THRESHOLD_CH1_HIGH && ch2Val >= THRESHOLD_CH2_HIGH);
		boolean detected = fallingSignal != risingSignal;
		
		if (detected && (mLastDetectionSample == -1 || (_reader.getChannelDataSampleCount() - mLastDetectionSample) >= safeOffset)) {			
			SimpleRegression regression = new SimpleRegression();
			double[][] regArray = new double[slopeSamples][2];
			int regIdx = 0;
			for(int idx = i-slopeSamples; idx < i; idx++) {
				int actualIdx = idx < 0 ? buffer.length + idx : idx;
				regArray[regIdx][0] = (double)regIdx / _reader.getSampleRate();
				regArray[regIdx][1] = buffer[1][actualIdx];
				regIdx++;
			}
			regression.addData(regArray);
			double slope = regression.getSlope();
			
			if ((fallingSignal && slope < 0) || (risingSignal && slope > 0)) {
				LookGestureDirection dir = fallingSignal ? LookGestureDirection.RIGHT : LookGestureDirection.LEFT;
				
				if (!(mLastDirection != dir && (_reader.getChannelDataSampleCount() - mLastDetectionSample) <= backToCenterSamples)) {
					LookGestureEvent evt = new LookGestureEvent(dir);
					notifyLook(evt);
					
					mLastDetectionSample = _reader.getChannelDataSampleCount();
					mLastDirection = dir;
				}
			}
		}
	}
	
	private void notifyLook(LookGestureEvent _evt) {
		for (LookGestureEventListener listener : mListeners) {
			listener.onLook(_evt);
		}
	}
}