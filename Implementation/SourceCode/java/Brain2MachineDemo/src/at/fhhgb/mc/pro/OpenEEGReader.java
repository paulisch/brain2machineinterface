package at.fhhgb.mc.pro;

import gnu.io.SerialPort;

public class OpenEEGReader implements SerialReader.ReceiveEventListener {
	
	public static final int CHANNELS_DEFAULT = 2;
	public static final int MIN_CHANNELS = 1;
	public static final int MAX_CHANNELS = 6;
	
	public static final byte SYNC0 = (byte)0xA5;
	public static final byte SYNC1 = (byte)0x5A;
	
	public static final int BAUDRATE = 57600;
	public static final int DATABITS = SerialPort.DATABITS_8;
	public static final int STOPBITS = SerialPort.STOPBITS_1;
	public static final int PARITY = SerialPort.PARITY_NONE;
	
	public static final int PACKET_SIZE = 17;
	
	public static final int SAMPLE_RATE = 256;
	
	public static final int SERIAL_BUFFER_SIZE_DEFAULT = 256;
	public static final int EEG_BUFFER_SIZE_DEFAULT = SAMPLE_RATE * 10;
	
	private SerialReader mReader = null;
	private Thread mReaderThread = null;
	
	private byte[] mTempBuffer = null;
	private int mTempBufferPointer = -1;
	
	private int mChannels = 0;
	private double[][] mChannelData = null;
	private int mChannelDataPointer = -1;
	
	public OpenEEGReader(String _port) {
		this(_port, CHANNELS_DEFAULT);
	}
	
	public OpenEEGReader(String _port, int _channels) {
		setChannels(_channels);
		resetChannelData();
		mReader = new SerialReader(_port, BAUDRATE, DATABITS, STOPBITS, PARITY, SERIAL_BUFFER_SIZE_DEFAULT);
		mReader.addReceiveEventListener(this);
		mTempBuffer = new byte[PACKET_SIZE];
	}
	
	private void resetChannelData() {
		if (mChannelData == null || mChannelData.length != mChannels) {
			mChannelData = new double[mChannels][EEG_BUFFER_SIZE_DEFAULT];
		}
		for(int i=0; i<mChannelData.length; i++) {
			for(int j=0; j<mChannelData[i].length; j++) {
				mChannelData[i][j] = -1;
			}
		}
		mChannelDataPointer = -1;
		mTempBufferPointer = -1;
	}
	
	private void setChannels(int _channels) {
		if (_channels < MIN_CHANNELS) {
			System.out.println("At least "+MIN_CHANNELS+" channel(s) necessary! Setting channel count to " + MIN_CHANNELS + ".");
			_channels = MIN_CHANNELS;
		} else if (_channels > MAX_CHANNELS) {
			System.out.println("At most "+MIN_CHANNELS+" channel(s) possible! Setting channel count to " + MIN_CHANNELS + ".");
			_channels = MAX_CHANNELS;
		}
	}
	
	public int getChannels() {
		return mChannels;
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
		mReaderThread = null;
	}

	@Override
	public void onReceivedData(byte[] _buffer, int _length) {
		if (mChannelDataPointer <= -1) {
			mChannelDataPointer = 0;
		}
		if (mTempBufferPointer <= -1) {
			mTempBufferPointer = 0;
		}
		for (int i=0; i<_length - 1; i++) {
			if(_buffer[i] == SYNC0 && _buffer[i+1] == SYNC1) {
				addNewRawValueToChannel(0, _buffer[i + 3], _buffer[i + 4]);
                addNewRawValueToChannel(1, _buffer[i + 5], _buffer[i + 6]);
			}
		}
	}
	
	private void addNewRawValueToChannel(int _channel, byte _high, byte _low) {    
        
        // Calculate each channel data from lowbit and highbit. It's a 16bit value divided to 8bits :)
        int intValue  = _high * 256 + _low; // 256 x chan high bytes + chanlow bytes. Google If you are unsure about it.
        
        // Update the packet on the sample.
        mChannelData[_channel][mChannelDataPointer] = intValue / 1023.0;
   }
}