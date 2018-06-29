package at.fhhgb.mc.pro.gesture;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import at.fhhgb.mc.pro.reader.OpenEEGReader;

public class FreqGesture extends Gesture<FreqGestureEventListener> {
	
	public static final float THRESHOLD_BITE_1_SEC_SAMPLES_CH1 = 0.6f;
	public static final float THRESHOLD_BITE_1_SEC_SAMPLES_CH2 = 2.1f;
	
	public static final float THRESHOLD_SMILE_1_SEC_SAMPLES_CH1 = 0.07f;
	public static final float THRESHOLD_SMILE_1_SEC_SAMPLES_CH2 = 0.37f;
	
	public static final float THRESHOLD_PREVENT_LOOK_1_SEC_SAMPLES_CH1 = 0.5f;
	public static final float THRESHOLD_PREVENT_LOOK_1_SEC_SAMPLES_CH2 = 0.37f;
	
	private int mFreqRangeLow = 0;
	private int mFreqRangeHigh = 0;
	private float mSampleDuration = 0;
	private float mOverlap = 0;
	private float mThresholdCh1 = 0;
	private float mThresholdCh2 = 0;
	
	private long mLastFourierTaken = 0;
	private boolean mStarted = false;
	
	public FreqGesture(int _freqRangeLow, int _freqRangeHigh, float _sampleDuration, float _overlap, float _thresholdCh1, float _thresholdCh2) {
		mFreqRangeLow = Math.max(0, _freqRangeLow);
		mFreqRangeHigh = Math.max(0, _freqRangeHigh);
		
		if (mFreqRangeHigh < mFreqRangeLow) {
			int tmp = mFreqRangeHigh;
			mFreqRangeHigh = mFreqRangeLow;
			mFreqRangeLow = tmp;
		}
		
		mSampleDuration = Math.max(0, _sampleDuration);
		mOverlap = Math.max(0, Math.min(1, _overlap));
		mThresholdCh1 = _thresholdCh1;
		mThresholdCh2 = _thresholdCh2;
	}

	@Override
	public void handleNextSample(OpenEEGReader _reader) {
		int durationSamples = (int)(mSampleDuration * _reader.getSampleRate());
		int overlapOffset = (int)(durationSamples * (1f - mOverlap));
		int i = _reader.getChannelDataPointer();
		double[][] buffer = _reader.getChannelData();
		
		if (_reader.getChannelDataSampleCount() <= durationSamples) {
			return;
		}
		
		if (mLastFourierTaken == -1 || _reader.getChannelDataSampleCount() - mLastFourierTaken >= overlapOffset) {
			mLastFourierTaken = _reader.getChannelDataSampleCount();
			FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
			
			double[][] chFourier = new double[2][durationSamples];
			
			int fIdx = 0;
			
			//Get channel data for transformation
			for(int idx = i-durationSamples; idx < i; idx++) {
				int actualIdx = idx < 0 ? buffer[0].length + idx : idx;
				chFourier[0][fIdx] = buffer[0][actualIdx];
				chFourier[1][fIdx] = buffer[1][actualIdx];
				fIdx++;
			}
			
			//Transform data
			Complex[][] transformed = {
					fft.transform(chFourier[0], TransformType.FORWARD),
					fft.transform(chFourier[1], TransformType.FORWARD)
			};
			
			//Get ABS of transformation
			double[][] transformedReal = new double[transformed.length][transformed[0].length];
			for (int j=0; j<transformedReal.length; j++) {
				for (int k=0; k<transformedReal[j].length; k++) {
					transformedReal[j][k] = transformed[j][k].abs();
				}
			}
			
			//Calculate average over desired frequency room
			double[] avg = { 0, 0 };
			int[] count = { 0, 0 };
			for (int j=0; j<transformedReal.length; j++) {
				for (int k=0; k<transformedReal[j].length / 2; k++) {
					double freq = k * _reader.getSampleRate() / (double)transformedReal[j].length;
					if (freq >= mFreqRangeLow && freq <= mFreqRangeHigh) {
						avg[j] += transformedReal[j][k];
						count[j]++;
					}
				}
			}
			
			for(int j=0; j<avg.length; j++) {
				avg[j] /= count[j];
			}
			
			//Check threshold
			if(avg[0] >= mThresholdCh1 && avg[1] >= mThresholdCh2) {
				if (!mStarted) {
					notifyStart(new FreqGestureEvent());
				}
				mStarted = true;
			} else {
				if (mStarted) {
					notifyComplete(new FreqGestureEvent());
				}
				mStarted = false;
			}
		}
	}
	
	private void notifyStart(FreqGestureEvent _evt) {
		for (FreqGestureEventListener listener : mListeners) {
			listener.onFreqGestureEventStart(_evt);
		}
	}
	
	private void notifyComplete(FreqGestureEvent _evt) {
		for (FreqGestureEventListener listener : mListeners) {
			listener.onFreqGestureEventComplete(_evt);
		}
	}
}