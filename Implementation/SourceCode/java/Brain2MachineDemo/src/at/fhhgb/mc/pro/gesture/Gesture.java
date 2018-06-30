package at.fhhgb.mc.pro.gesture;

import java.util.ArrayList;
import java.util.List;
import at.fhhgb.mc.pro.reader.OpenEEGReader;

/**
 * A class representing a gesture that is capable of managing a set of gesture event listeners that get notified when gestures are detected and events are triggered.
 * @author Boris Fuchs, Paul Schmutz
 */
public abstract class Gesture<T extends GestureEventListener> {
	
	/**
	 * A list of all gesture event listeners.
	 */
	protected List<T> mListeners = null;
	
	/**
	 * Default constructor.
	 */
	public Gesture() {
		mListeners = new ArrayList<>();
	}
	
	/**
	 * Registers a certain gesture event listener.
	 * @param _listener the gesture event listener
	 */
	public void addGestureEventListener(T _listener) {
		if (!mListeners.contains(_listener)) {
			mListeners.add(_listener);
		}
	}
	
	/**
	 * Unregisters a certain gesture event listener.
	 * @param _listener the gesture event listener to unregister
	 */
	public void removeGestureEventListener(T _listener) {
		mListeners.remove(_listener);
	}
	
	/**
	 * Unregisters all gesture event listeners.
	 */
	public void clearGestureEventListeners() {
		mListeners.clear();
	}
	
	/**
	 * Checks next incoming samples of the reader to trigger events if appropriate.
	 * @param _reader the OpenEEGReader that provides the data for analysis
	 */
	public abstract void handleNextSample(OpenEEGReader _reader);
}