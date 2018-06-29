package at.fhhgb.mc.pro.reader;

import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class SerialReader implements Runnable {
	
	public static final int BUFFER_SIZE_DEFAULT = 256;
	public static final String PORT_WINDOWS_DEFAULT = "COM1";
	public static final int BAUDRATE_DEFAULT = 9600;
	public static final int DATABITS_DEFAULT = SerialPort.DATABITS_8;
	public static final int STOPBITS_DEFAULT = SerialPort.STOPBITS_1;
	public static final int PARITY_DEFAULT = SerialPort.PARITY_NONE;
	
	private String mPort = null;
	private int mBaudRate = 0;
	private int mDataBits = 0;
    private int mStopBits = 0;
    private int mParity = 0;
    private int mBufferSize = 0;
	
    private SerialPort mSerialPort = null;
    private InputStream mIStream = null;
    
    private boolean mRunReadingThread = false;
    
    private List<ReceiveEventListener> mReceiveListeners = null;
	
	public SerialReader() {
		this(PORT_WINDOWS_DEFAULT);
	}
	
	public SerialReader(String _port) {
		this(_port, BAUDRATE_DEFAULT, DATABITS_DEFAULT, STOPBITS_DEFAULT, PARITY_DEFAULT);
	}
	
	public SerialReader(String _port, int _baudRate, int _databits, int _stopbits, int _parity) {
		this(_port, _baudRate, _databits, _stopbits, _parity, BUFFER_SIZE_DEFAULT);
	}

	public SerialReader(String _port, int _baudRate, int _databits, int _stopbits, int _parity, int _bufferSize) {
		mPort = _port;
		mBaudRate = _baudRate;
		mDataBits = _databits;
		mStopBits = _stopbits;
		mParity = _parity;
		mBufferSize = _bufferSize;
		mReceiveListeners = new ArrayList<>();
		mRunReadingThread = false;
	}
	
	public void addReceiveEventListener(ReceiveEventListener _listener) {
		if (!mReceiveListeners.contains(_listener)) {
			mReceiveListeners.add(_listener);
		}
	}
	
	public void removeReceiveEventListener(ReceiveEventListener _listener) {
		mReceiveListeners.remove(_listener);
	}
	
	public void clearReceiveEventListeners() {
		mReceiveListeners.clear();
	}

	@Override
	public void run() {
		mRunReadingThread = true;
		byte[] buffer = new byte[mBufferSize];
        int len = -1;
        try {
            while (mRunReadingThread && (len = mIStream.read(buffer)) > -1) {
                if (len > 0) {
                	fireReceiveData(buffer, len);
                }
            }
            System.out.println("Reading thread exit.");
        }
        catch (IOException e)
        {
            System.err.println(e.getStackTrace());
        }
	}
	
	private void fireReceiveData(byte[] _buffer, int _length) {
		for(ReceiveEventListener listener : mReceiveListeners) {
			listener.onReceivedData(_buffer, _length);
		}
	}
	
    public boolean connect() {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(mPort);        
            if (portIdentifier.isCurrentlyOwned()) {
                System.err.println("Port " + mPort + " is currently in use");
                return false;
            }
            else {
            	//Open port with timeout of 2000ms
                CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

                if (commPort instanceof SerialPort) {
                    mSerialPort = (SerialPort) commPort;
                    mSerialPort.setSerialPortParams(mBaudRate, mDataBits, mStopBits, mParity);

                    mIStream = mSerialPort.getInputStream();
                } else {
                    System.err.println("Only serial ports are handled currently.");
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Exception while connecting to port " + mPort + ": " + e.getMessage());
            return false;
        }
        System.out.println("Com port connected " + mPort);
        return true;
    }
    
    public void disconnect() {
        mRunReadingThread = false;
        if (isConnected()) {
            System.out.println("Closing serial port " + mPort);
            if (mIStream != null) {
                try {
                	mIStream.close();
                } catch(Exception e) {
                    System.err.println("Exception when closing input stream " + e + ".");
                }                
            }
            mSerialPort.close();
            System.out.println("Serial port " + mPort + " disconnected.");  
            mIStream = null;
            mSerialPort = null;        
        } else {
            System.out.println("No need to close serial port: Not connected.");
        }
    }
    
    public boolean isConnected() {
        if (mSerialPort != null)
            return true;
        return false;
    }
	
	public static interface ReceiveEventListener {
		public void onReceivedData(byte[] _buffer, int _length);
	}
}