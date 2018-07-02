package at.fhhgb.mc.pro.reader;

import java.util.ArrayList;
import java.util.List;
import gnu.io.SerialPort;
import at.fhhgb.mc.pro.gesture.Gesture;

/**
 * A class representing a component for reading OpenEEG data from a serial port - specifically for Olimex EEG.
 * @author Boris Fuchs, Paul Schmutz
 */
public class OpenEEGReader implements SerialReader.ReceiveEventListener {
	
	/**
	 * Predefined constant for the default number of channels for the Olimex EEG.
	 */
	public static final int CHANNELS_DEFAULT = 2;
	
	/**
	 * Constant for the minimum number of channels for the Olimex EEG.
	 */
	public static final int MIN_CHANNELS = 1;
	
	/**
	 * Constant for the maximum number of channels for the Olimex EEG.
	 */
	public static final int MAX_CHANNELS = 6;
	
	
	/**
	 * Value for the sync byte 0.
	 */
	public static final byte SYNC0 = (byte)0xA5; //165
	
	/**
	 * Value for the sync byte 1.
	 */
	public static final byte SYNC1 = (byte)0x5A;
	
	/**
	 * Value for the version number.
	 */
	public static final byte VERSION = (byte)2;
	
	
	/**
	 * Baud rate for the Olimex EEG.
	 */
	public static final int BAUDRATE = 57600;
	
	/**
	 * Data bits for the Olimex EEG.
	 */
	public static final int DATABITS = SerialPort.DATABITS_8;
	
	/**
	 * Stop bits for the Olimex EEG.
	 */
	public static final int STOPBITS = SerialPort.STOPBITS_1;
	
	/**
	 * Parity for the Olimex EEG.
	 */
	public static final int PARITY = SerialPort.PARITY_NONE;
	
	/**
	 * Number of bytes in one Open EEG packet.
	 */
	public static final int PACKET_SIZE = 17;
	
	/**
	 * Sample rate of the Olimex EEG.
	 */
	public static final int SAMPLE_RATE = 256;
	
	
	/**
	 * Default buffer size for new EEG data arriving at the serial port.
	 */
	public static final int SERIAL_BUFFER_SIZE_DEFAULT = 256;
	
	/**
	 * Default buffer duration of EEG data.
	 */
	public static final int EEG_BUFFER_TIME_DEFAULT = 10;
	
	
	/**
	 * The serial reader to read the EEG data from a serial port.
	 */
	private SerialReader mReader = null;
	
	/**
	 * The thread that is responsible for reading EEG data.
	 */
	private Thread mReaderThread = null;
	
	/**
	 * Buffer for storing the bytes of 1 single Open EEG data packet.
	 */
	private byte[] mTempBuffer = null;
	
	/**
	 * Current pointer to a byte of the temp buffer.
	 */
	private int mTempBufferPointer = -1;
	
	/**
	 * Duration of the EEG data to buffer in seconds.
	 */
	private int mBufferSamplesSeconds = 0;
	
	
	/**
	 * Number of channels.
	 */
	private int mChannels = 0;
	
	/**
	 * The buffer for storing EEG data with the length matching the mBufferSamplesSeconds duration.
	 */
	private double[][] mChannelData = null;
	
	/**
	 * Current pointer to a byte of EEG data buffer.
	 */
	private int mChannelDataPointer = -1;
	
	/**
	 * A sequential number for each packet.
	 */
	private long mChannelDataSampleCount = 0;
	
	/**
	 * Indicator whether the reader is currently waiting for the arriving bytes to sync (to get the beginning byte of a package).
	 */
	private boolean mSyncing = false;
	
	/**
	 * List of gestures that will be supplied with EEG data.
	 */
	@SuppressWarnings("rawtypes")
	private List<Gesture> mGestures = null;
	
	/**
	 * Constructor.
	 * @param _port the serial port for the EEG device
	 */
	public OpenEEGReader(String _port) {
		this(_port, CHANNELS_DEFAULT);
	}
	
	/**
	 * Constructor.
	 * @param _port the serial port for the EEG device
	 * @param _channels the number of channels of the EEG device
	 */
	public OpenEEGReader(String _port, int _channels) {
		this(_port, _channels, EEG_BUFFER_TIME_DEFAULT);
	}
	
	/**
	 * Constructor.
	 * @param _port the serial port for the EEG device
	 * @param _channels the number of channels of the EEG device
	 * @param _bufferSamplesSeconds the duration to buffer EEG data in seconds
	 */
	@SuppressWarnings("rawtypes")
	public OpenEEGReader(String _port, int _channels, int _bufferSamplesSeconds) {
		mBufferSamplesSeconds = Math.max(0, _bufferSamplesSeconds);
		setChannels(_channels);
		resetChannelData();
		mReader = new SerialReader(_port, BAUDRATE, DATABITS, STOPBITS, PARITY, SERIAL_BUFFER_SIZE_DEFAULT);
		mReader.addReceiveEventListener(this);
		mTempBuffer = new byte[PACKET_SIZE];
		mGestures = new ArrayList<Gesture>();
	}
	
	/**
	 * Reset the buffers and pointers.
	 */
	private void resetChannelData() {
		if (mChannelData == null || mChannelData.length != mChannels) {
			mChannelData = new double[mChannels][getBufferLength()];
		}
		for(int i=0; i<mChannelData.length; i++) {
			for(int j=0; j<mChannelData[i].length; j++) {
				mChannelData[i][j] = -1;
			}
		}
		mChannelDataPointer = -1;
		mTempBufferPointer = -1;
		mChannelDataSampleCount = 0;
		mSyncing = true;
	}
	
	/**
	 * Sets the number of channels.
	 * @param _channels the number of channels
	 */
	private void setChannels(int _channels) {
		if (_channels < MIN_CHANNELS) {
			System.out.println("At least "+MIN_CHANNELS+" channel(s) necessary! Setting channel count to " + MIN_CHANNELS + ".");
			_channels = MIN_CHANNELS;
		} else if (_channels > MAX_CHANNELS) {
			System.out.println("At most "+MIN_CHANNELS+" channel(s) possible! Setting channel count to " + MIN_CHANNELS + ".");
			_channels = MAX_CHANNELS;
		}
		mChannels = _channels;
	}
	
	/**
	 * Gets the number of channels.
	 * @return the number of channels
	 */
	public int getChannels() {
		return mChannels;
	}
	
	/**
	 * Adds a gesture to the gesture list.
	 * @param _gesture the gesture to add
	 */
	@SuppressWarnings("rawtypes")
	public void addGesture(Gesture _gesture) {
		mGestures.add(_gesture);
	}
	
	/**
	 * Removes a gesture from the gesture list.
	 * @param _gesture the gesture to remove
	 */
	@SuppressWarnings("rawtypes")
	public void removeGesture(Gesture _gesture) {
		mGestures.remove(_gesture);
	}
	
	/**
	 * Gets the duration of EEG data to buffer in seconds.
	 * @return the duration of EEG data to buffer in seconds
	 */
	public int getBufferSamplesSeconds() {
		return mBufferSamplesSeconds;
	}
	
	/**
	 * Gets the length of EEG data to buffer.
	 * @return the length of EEG data to buffer.
	 */
	public int getBufferLength() {
		return mBufferSamplesSeconds * getSampleRate();
	}
	
	/**
	 * Gets the sample rate.
	 * @return the sample rate
	 */
	public int getSampleRate() {
		return SAMPLE_RATE;
	}
	
	/**
	 * Gets the EEG data divided in multiple channels.
	 * @return the EEG data divided in multiple channels.
	 */
	public double[][] getChannelData() {
		return mChannelData;
	}
	
	/**
	 * Gets the pointer to the current byte in the channel data.
	 * @return the pointer to the current byte in the channel data
	 */
	public int getChannelDataPointer() {
		return mChannelDataPointer;
	}
	
	/**
	 * Gets the channel data sample count.
	 * @return the channel data sample count
	 */
	public long getChannelDataSampleCount() {
		return mChannelDataSampleCount;
	}
	
	/**
	 * Connects the serial reader and starts reading data.
	 */
	public void connect() {
		resetChannelData();
		if (mReaderThread == null && !mReader.isConnected()) {
			if (mReader.connect()) {
				mReaderThread = mReader.startReading();
			}
		}
	}
	
	/**
	 * Closes the connection to serial device.
	 */
	public void disconnect() {
		if (mReader.isConnected()) {
			mReader.disconnect();
		}
		if (mReaderThread != null) {
			mReaderThread.interrupt();
		}
		mReaderThread = null;
	}

	/**
	 * Method that is called when new data arrived.
	 * @param _buffer the buffer containing the new data from index 0 to _length - 1
	 * @param _length the length of the received data bytes
	 */
	@Override
	public void onReceivedData(byte[] _buffer, int _length) {
		for (int i=0; i < _length; i++) {
			
			//Still in syncing phase? (Has to find the first byte of a packet)
			if (mSyncing) {
				if (mTempBufferPointer == -1) {
					mTempBufferPointer = 0;
				}
				mTempBuffer[mTempBufferPointer] = _buffer[i];
				mTempBufferPointer = (mTempBufferPointer + 1) % mTempBuffer.length;
				
				//Detect start of package
				if (_buffer[i] == VERSION) {
					int idxSync = -1;
					for (int j=0; j<mTempBuffer.length; j++) {
						if (mTempBuffer[j] == SYNC0 && mTempBuffer[(j+1)%mTempBuffer.length] == SYNC1 && mTempBuffer[(j+2)%mTempBuffer.length] == VERSION) {
							idxSync = j;
							break;
						}
					}
					
					if (idxSync != -1) {
						for (int j=0; j<3; j++) {
							mTempBuffer[j] = mTempBuffer[(j + idxSync)%mTempBuffer.length];
						}
						mSyncing = false;
						mTempBufferPointer = 3;
						
						System.out.println("Syncing done.");
					}
				}
			} else {
				//Collect next value if valid
				mTempBuffer[mTempBufferPointer] = _buffer[i];
				mTempBufferPointer++;
				if (mTempBufferPointer >= mTempBuffer.length) {
					mTempBufferPointer = 0;
					submitTempBuffer();
				}
			}
		}
	}
	
	/**
	 * Helper method to submit data for the EEG buffer whenever a new complete EEG packet has arrived.
	 */
	private void submitTempBuffer() {
		if (mChannelDataPointer <= -1) {
			mChannelDataPointer = 0;
		}
	
		//Add values to channels
		for(int ch=0; ch<mChannels; ch++) {
			addNewRawValueToChannel(ch, mTempBuffer[4+ch*2], mTempBuffer[4+ch*2+1]);
		}
		
		//Reset temp buffer
		for (int i=0; i<mTempBuffer.length; i++) {
			mTempBuffer[i] = -1;
		}
		
		//Handle gestures
		for(@SuppressWarnings("rawtypes") Gesture gesture : mGestures) {
			gesture.handleNextSample(this);
		}
		
		mChannelDataPointer++;
		mChannelDataSampleCount++;
		
		if (mChannelDataPointer >= getBufferLength()) {
			mChannelDataPointer = 0;
		}
	}
	
	/**
	 * Helper method to add a new signal byte to the EEG data buffer.
	 * @param _channel the channel to add the value to
	 * @param _high the high bit of the 10-bit value
	 * @param _low the low bit of the 10-bit value
	 */
	private void addNewRawValueToChannel(int _channel, int _high, int _low) {
		
		if (_high < 0) {
			_high = 0;
		} else if (_high > 3) {
			_high = 3;
		}
		
		if (_low < 0) {
			_low = 256 + _low;
		}
        
        // Calculate each channel data from low bit and high bit.
        int intValue  = _high * 256 + _low; // 256 x chan high bytes + chanlow bytes.
        
        // Update the packet on the sample.
        mChannelData[_channel][mChannelDataPointer] = intValue / 1023.0;
   }
}