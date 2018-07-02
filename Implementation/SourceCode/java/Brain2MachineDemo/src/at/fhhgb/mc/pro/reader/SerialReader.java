package at.fhhgb.mc.pro.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

/**
 * A class that is capable of reading from a serial port and notifying listeners that register to receive that data.
 * @author Boris Fuchs, Paul Schmutz
 */
public class SerialReader {
	
	/**
	 * Constant for default buffer size.
	 */
	public static final int BUFFER_SIZE_DEFAULT = 256;
	
	/**
	 * Default serial port for windows.
	 */
	public static final String PORT_WINDOWS_DEFAULT = "COM1";
	
	/**
	 * Default baud rate.
	 */
	public static final int BAUDRATE_DEFAULT = 9600;
	
	/**
	 * Default data bits.
	 */
	public static final int DATABITS_DEFAULT = SerialPort.DATABITS_8;
	
	/**
	 * Default stop bits.
	 */
	public static final int STOPBITS_DEFAULT = SerialPort.STOPBITS_1;
	
	/**
	 * Default parity.
	 */
	public static final int PARITY_DEFAULT = SerialPort.PARITY_NONE;
	
	
	
	/**
	 * Desired port to listen to.
	 */
	private String mPort = null;
	
	/**
	 * Baud rate for that port.
	 */
	private int mBaudRate = 0;
	
	/**
	 * Info about data bits.
	 */
	private int mDataBits = 0;
	
	/**
	 * Info about stop bits.
	 */
    private int mStopBits = 0;
    
    /**
	 * Info about parity.
	 */
    private int mParity = 0;
    
    /**
	 * The buffer size used for receiving data.
	 */
    private int mBufferSize = 0;
	
    /**
     * The serial port object used for handling communication; on Windows always starting with "COM" and followed by a number (e. g. "COM1").
     */
    private SerialPort mSerialPort = null;
    
    /**
     * The input stream handle for the serial port to receive data from.
     */
    private InputStream mIStream = null;
    
    /**
     * Indicates whether reading thread is running.
     */
    private boolean mRunReadingThread = false;
    
    /**
     * The reading thread that handles incoming data.
     */
    private Thread mReadingThread = null;
    
    /**
     * List of listeners that get notified about new data from the serial port.
     */
    private List<ReceiveEventListener> mReceiveListeners = null;
	
    /**
     * Default constructor.
     */
	public SerialReader() {
		this(PORT_WINDOWS_DEFAULT);
	}
	
	/**
	 * Constructor.
	 * @param _port the desired serial port
	 */
	public SerialReader(String _port) {
		this(_port, BAUDRATE_DEFAULT, DATABITS_DEFAULT, STOPBITS_DEFAULT, PARITY_DEFAULT);
	}
	
	/**
	 * Constructor.
	 * @param _port the desired serial port
	 * @param _baudRate the baud rate for communicating over the serial port
	 * @param _databits the data bits for the serial port communication
	 * @param _stopbits the stop bits for the serial port communication
	 * @param _parity the parity for the serial port communication
	 */
	public SerialReader(String _port, int _baudRate, int _databits, int _stopbits, int _parity) {
		this(_port, _baudRate, _databits, _stopbits, _parity, BUFFER_SIZE_DEFAULT);
	}

	/**
	 * Constructor.
	  * @param _port the desired serial port
	 * @param _baudRate the baud rate for communicating over the serial port
	 * @param _databits the data bits for the serial port communication
	 * @param _stopbits the stop bits for the serial port communication
	 * @param _parity the parity for the serial port communication
	 * @param _bufferSize the buffer size used for receiving input of the serial port
	 */
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
	
	/**
	 * Registers the receive event listener.
	 * @param _listener the receive event listener to add
	 */
	public void addReceiveEventListener(ReceiveEventListener _listener) {
		if (!mReceiveListeners.contains(_listener)) {
			mReceiveListeners.add(_listener);
		}
	}
	
	/**
	 * Unregisters the receive event listener.
	 * @param _listener the receive event listener to remove
	 */
	public void removeReceiveEventListener(ReceiveEventListener _listener) {
		mReceiveListeners.remove(_listener);
	}
	
	/**
	 * Unregisters all receive event listeners.
	 */
	public void clearReceiveEventListeners() {
		mReceiveListeners.clear();
	}
	
	
	/**
	 * Helper method to notify all listeners about new received data.
	 * @param _buffer the buffer containing the new data from index 0 to _length - 1 
	 * @param _length the length of the newly arrived data
	 */
	private void fireReceiveData(byte[] _buffer, int _length) {
		for(ReceiveEventListener listener : mReceiveListeners) {
			listener.onReceivedData(_buffer, _length);
		}
	}
	
	/**
	 * Connects to the serial device on the serial port using the specified configuration parameters.
	 * @return true if connection succeeded; false if an error occurred
	 */
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
    
    /**
     * Disconnects from the device on the serial port.
     */
    public void disconnect() {
    	stopReading();
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
    
    /**
     * Checks whether a connection on the serial port exists.
     * @return true if the serial port is connected; false otherwise
     */
    public boolean isConnected() {
        if (mSerialPort != null)
            return true;
        return false;
    }
    
    /**
     * Starts reading from the serial port if serial port is connected and reading has not yet started.
     * @return the thread of the new or existing thread that reads data from the serial port; may be null if serial port is not connected
     */
    public Thread startReading() {
    	if (isConnected() && (mReadingThread == null || (!mReadingThread.isAlive() && mRunReadingThread))) {
    		mReadingThread = new Thread(new Reader());
        	mReadingThread.start();
    	}
    	return mReadingThread;
    }
    
    /**
     * Stops the reading thread.
     */
    public void stopReading() {
    	mRunReadingThread = false;
    	mReadingThread = null;
    }
    
    /**
     * Gets the reading thread.
     * @return the thread that is reading data from the serial port; may be null if not connected and reading has not started yet
     */
    public Thread getReadingThread() {
    	return mReadingThread;
    }
    
    /**
     * Helper class that is providing the functionality of reading from the serial port.
     */
    private class Reader implements Runnable {
    	/**
    	 * Executed in a separate thread this method will receive data from the serial port and notify listeners about new data.
    	 */
    	@Override
    	public void run() {
    		mRunReadingThread = true;
    		byte[] buffer = new byte[mBufferSize];
            int len = -1;
            try {
                while (isConnected() && mRunReadingThread && (len = mIStream.read(buffer)) > -1) {
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
            mRunReadingThread = false;
    	}
    }
	
    /**
     * Interface for representing event listeners that are notified about new data and can implement custom behavior on that received data.
     */
	public static interface ReceiveEventListener {
		/**
		 * Method that is called when new data arrived.
		 * @param _buffer the buffer containing the new data from index 0 to _length - 1
		 * @param _length the length of the received data bytes
		 */
		public void onReceivedData(byte[] _buffer, int _length);
	}
}