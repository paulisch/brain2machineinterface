package at.fhhgb.mc.pro.reader;

import java.util.ArrayList;
import java.util.List;

import at.fhhgb.mc.pro.gesture.Gesture;
import gnu.io.SerialPort;

public class OpenEEGReader implements SerialReader.ReceiveEventListener {
	
	public static final int CHANNELS_DEFAULT = 2;
	public static final int MIN_CHANNELS = 1;
	public static final int MAX_CHANNELS = 6;
	
	public static final byte SYNC0 = (byte)0xA5; //165
	public static final byte SYNC1 = (byte)0x5A;
	public static final byte VERSION = (byte)2;
	
	public static final int BAUDRATE = 57600;
	public static final int DATABITS = SerialPort.DATABITS_8;
	public static final int STOPBITS = SerialPort.STOPBITS_1;
	public static final int PARITY = SerialPort.PARITY_NONE;
	
	public static final int PACKET_SIZE = 17;
	
	public static final int SAMPLE_RATE = 256;
	
	public static final int SERIAL_BUFFER_SIZE_DEFAULT = 256;
	public static final int EEG_BUFFER_TIME_DEFAULT = 10;
	
	private SerialReader mReader = null;
	private Thread mReaderThread = null;
	
	private byte[] mTempBuffer = null;
	private int mTempBufferPointer = -1;
	
	private int mBufferSamplesSeconds = 0;
	
	private int mChannels = 0;
	private double[][] mChannelData = null;
	private int mChannelDataPointer = -1;
	private long mChannelDataSampleCount = 0;
	
	private boolean mSyncing = false;
	
	private List<Gesture> mGestures = null;
	
	public OpenEEGReader(String _port) {
		this(_port, CHANNELS_DEFAULT);
	}
	
	public OpenEEGReader(String _port, int _channels) {
		this(_port, _channels, EEG_BUFFER_TIME_DEFAULT);
	}
	
	public OpenEEGReader(String _port, int _channels, int _bufferSamplesSeconds) {
		mBufferSamplesSeconds = Math.max(0, _bufferSamplesSeconds);
		setChannels(_channels);
		resetChannelData();
		mReader = new SerialReader(_port, BAUDRATE, DATABITS, STOPBITS, PARITY, SERIAL_BUFFER_SIZE_DEFAULT);
		mReader.addReceiveEventListener(this);
		mTempBuffer = new byte[PACKET_SIZE];
		mGestures = new ArrayList<Gesture>();
	}
	
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
	
	public int getChannels() {
		return mChannels;
	}
	
	public void addGesture(Gesture _gesture) {
		mGestures.add(_gesture);
	}
	
	public void removeGesture(Gesture _gesture) {
		mGestures.remove(_gesture);
	}
	
	public int getBufferSamplesSeconds() {
		return mBufferSamplesSeconds;
	}
	
	public int getBufferLength() {
		return mBufferSamplesSeconds * getSampleRate();
	}
	
	public int getSampleRate() {
		return SAMPLE_RATE;
	}
	
	public double[][] getChannelData() {
		return mChannelData;
	}
	
	public int getChannelDataPointer() {
		return mChannelDataPointer;
	}
	
	public long getChannelDataSampleCount() {
		return mChannelDataSampleCount;
	}
	
	public boolean isChannelDataFirstFill() {
		return mChannelDataSampleCount <= getChannelDataSampleCount();
	}
	
	public void connect() {
		resetChannelData();
		if (mReaderThread == null && !mReader.isConnected()) {
			if (mReader.connect()) {
				mReaderThread = new Thread(mReader);
				mReaderThread.start();
			}
		}
	}
	
	public void disconnect() {
		if (mReader.isConnected()) {
			mReader.disconnect();
		}
		if (mReaderThread != null) {
			mReaderThread.interrupt();
		}
		mReaderThread = null;
	}

	@Override
	public void onReceivedData(byte[] _buffer, int _length) {
		for (int i=0; i < _length; i++) {
			
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
		for(Gesture gesture : mGestures) {
			gesture.handleNextSample(this);
		}
		
		//System.out.println(mChannelData[1][mChannelDataPointer]);
		
		mChannelDataPointer++;
		mChannelDataSampleCount++;
		
		if (mChannelDataPointer >= getBufferLength()) {
			mChannelDataPointer = 0;
		}
	}
	
	private void addNewRawValueToChannel(int _channel, int _high, int _low) {
		
		if (_high < 0) {
			_high = 0;
		} else if (_high > 3) {
			_high = 3;
		}
		
		if (_low < 0) {
			_low = 256 + _low;
		}
        
        // Calculate each channel data from lowbit and highbit. It's a 16bit value divided to 8bits :)
        int intValue  = _high * 256 + _low; // 256 x chan high bytes + chanlow bytes. Google If you are unsure about it.
        
        // Update the packet on the sample.
        mChannelData[_channel][mChannelDataPointer] = intValue / 1023.0;
   }
}