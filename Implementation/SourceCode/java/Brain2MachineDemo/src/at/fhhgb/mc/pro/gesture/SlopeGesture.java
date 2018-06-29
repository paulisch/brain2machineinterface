package at.fhhgb.mc.pro.gesture;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import at.fhhgb.mc.pro.reader.OpenEEGReader;

public class SlopeGesture extends Gesture<SlopeGestureEventListener> {
	
	public static final double THRESHOLD_CH1_HIGH = 0.61;
	public static final double THRESHOLD_CH1_LOW = 0.43;
	
	public static final double THRESHOLD_CH2_HIGH = 0.75;
	public static final double THRESHOLD_CH2_LOW = 0.18;
	
	public static final double SLOPE_SECONDS = 0.015;
	public static final double SAFE_OFFSET_SECONDS = 0.25;
	
	public static final double SLOPE_MIN = 15;
	public static final double SLOPE_MAX = 10000000;
	
	private double mCh1High = 0;
	private double mCh1Low = 0;
	
	private double mCh2High = 0;
	private double mCh2Low = 0;
	
	private double mSlopeSeconds = 0;
	private double mSafeOffsetSeconds = 0;
	
	private double mSlopeMin = 0;
	private double mSlopeMax = 0;
	
	private long mLastDetectionSample = -1;
	private long mLastSample = -1;

	public SlopeGesture(double _ch1High, double _ch1Low, double _ch2High, double _ch2Low, double _slopeSeconds, double _safeOffsetSeconds, double _slopeMin, double _slopeMax) {
		mCh1High = _ch1High;
		mCh1Low = _ch1Low;
		mCh2High = _ch2High;
		mCh2Low = _ch2Low;
		mSlopeSeconds = Math.abs(_slopeSeconds);
		mSafeOffsetSeconds = Math.abs(_safeOffsetSeconds);
		mSlopeMin = Math.abs(_slopeMin);
		mSlopeMax = Math.abs(_slopeMax);
	}
	
	@Override
	public void handleNextSample(OpenEEGReader _reader) {
		int safeOffset = (int)(mSafeOffsetSeconds * _reader.getSampleRate());
		int slopeSamples = (int)(mSlopeSeconds * _reader.getSampleRate());
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
			
			boolean fallingSignal = (ch1Val <= mCh1Low && ch2Val <= mCh2Low);
			boolean risingSignal = (ch1Val >= mCh1High && ch2Val >= mCh2High);
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
				
				if ((((fallingSignal && slope < 0) || (risingSignal && slope > 0)) && Math.abs(slope) <= SLOPE_MAX) && Math.abs(slope) >= mSlopeMin && Math.abs(slope) <= mSlopeMax) {
					SlopeGestureEvent evt = new SlopeGestureEvent();
					notifySlope(evt);
					mLastDetectionSample = _reader.getChannelDataSampleCount() - idx;
				}
			}
		}
		
		mLastSample = _reader.getChannelDataSampleCount();
	}
	
	private void notifySlope(SlopeGestureEvent _evt) {
		for (SlopeGestureEventListener listener : mListeners) {
			listener.onSlopeDetected(_evt);
		}
	}
}