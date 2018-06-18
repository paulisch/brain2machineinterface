package at.fhhgb.mc.pro.gesture;

import java.util.ArrayList;
import java.util.List;
import at.fhhgb.mc.pro.reader.OpenEEGReader;

public abstract class Gesture<T extends GestureEventListener> {
	
	protected List<T> mListeners = null;
	
	public Gesture() {
		mListeners = new ArrayList<>();
	}
	
	public void addGestureEventListener(T _listener) {
		if (!mListeners.contains(_listener)) {
			mListeners.add(_listener);
		}
	}
	
	public void removeGestureEventListener(T _listener) {
		mListeners.remove(_listener);
	}
	
	public void clearGestureEventListeners() {
		mListeners.clear();
	}
	
	public abstract void handleNextSample(OpenEEGReader _reader);
}