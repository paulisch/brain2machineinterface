// catEEG.c
// 
// Reads data from modularEEG on windows COM port.
// Uses 8 bit data, no parity, 1 stop bit.
// No hardware or software handshaking is used.
//
// Data is copied without any processing, does not
// care about packet format.
//
// Data is flushed as it is written, to enable viewing
// with BWView in follow-mode.
//
// This program is cobbled together from the serial 
// port example tty.c in the MSDN library.
//
//
// 12/17/02 Jack Sparr jspaar@myrealbox.com
// 
//

#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers

#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <io.h>


#define DEFAULT_COM		"COM1"
#define DEFAULT_BAUD	CBR_57600

#define DEV_RBUFSIZ		10240	// com port read buffer size
#define DEV_WBUFSIZ		4096	// com port write buffer size (unneeded)

char *progName = "catEEG";

void print_error(char *msg)
{
	LPVOID lpMsgBuf;
	DWORD dwError = GetLastError();
	FormatMessage(
		FORMAT_MESSAGE_ALLOCATE_BUFFER | 
		FORMAT_MESSAGE_FROM_SYSTEM | 
		FORMAT_MESSAGE_IGNORE_INSERTS,
		NULL,
		dwError,
		0, // Default language
		(LPTSTR) &lpMsgBuf,
		0,
		NULL 
		);
	if (msg != NULL) {
		fprintf(stderr, "%s: %s: %s\n", progName, msg, lpMsgBuf);
	} else {
		fprintf(stderr, "%s: %s\n", progName, lpMsgBuf);
	}
	LocalFree(lpMsgBuf);
	fflush(stderr);
}


void error_exit(char *msg)
{
	print_error(msg);
	exit(1);
}


void usage_exit(char *msg)
{
	if (msg) {
		fprintf(stderr, "%s: %s\n", progName, msg);
	}
	fprintf(stderr, "Usage: %s [options]\n", progName);
	fprintf(stderr, "Copy EEG data from COM port to stdout.\n");
	fprintf(stderr, "\noptions are:\n");
	fprintf(stderr, "\t-f <filename>   output to <filename> instead of stdout\n");
	fprintf(stderr, "\t                WARNING: if <filename> exists it will be overwritten.\n");
	fprintf(stderr, "\t-p <portname>   read from <portname> instead of %s\n", DEFAULT_COM);
	fprintf(stderr, "\t-b <baudrate>   set speed to <baudrate> instead of %d\n", DEFAULT_BAUD);
	fprintf(stderr, "\t-q              suppress start message\n");
	fprintf(stderr, "\t-h              print this help message\n");
	fprintf(stderr, "\nNote that 8 bit data, no parity, 1 stop bit, no handshaking are always used.\n");

	exit(msg != NULL);
}


// SetupConnection sets the port's baud, parity, etc.
BOOL SetupConnection(HANDLE hPort, int baudRate)
{
	BOOL       fRetVal ;
	DCB        dcb ;

	dcb.DCBlength = sizeof(DCB) ;

	GetCommState(hPort, &dcb) ;

	dcb.BaudRate = baudRate;
	dcb.ByteSize = 8;
	dcb.Parity = NOPARITY;
	dcb.StopBits = ONESTOPBIT;
	dcb.fOutxCtsFlow = FALSE;
	dcb.fOutxDsrFlow = FALSE;
	dcb.fInX = FALSE;
	dcb.fOutX = FALSE;
	dcb.fDtrControl = DTR_CONTROL_DISABLE;
	dcb.fRtsControl = RTS_CONTROL_DISABLE;
	dcb.fBinary = TRUE ;
	dcb.fParity = FALSE ;

	fRetVal = SetCommState(hPort, &dcb);

	return (fRetVal) ;
}


// ReadCommBlock reads available data up to nMaxLength bytes
int ReadCommBlock(HANDLE hPort, LPSTR lpszBlock, int nMaxLength)
{
	BOOL       fReadStat ;
	COMSTAT    ComStat ;
	DWORD      dwErrorFlags;
	DWORD      dwLength;


	// only try to read number of bytes in queue
	ClearCommError( hPort, &dwErrorFlags, &ComStat ) ;
	dwLength = min( (DWORD) nMaxLength, ComStat.cbInQue ) ;

	if (dwLength > 0)
	{
		fReadStat = ReadFile(hPort, lpszBlock, dwLength, &dwLength,NULL);
		if (!fReadStat)
		{
 			dwLength = 0 ;
			ClearCommError(hPort, &dwErrorFlags, &ComStat ) ;
			if (dwErrorFlags > 0) {
				error_exit("ReadCommBlock");
			}
		}
	}

	return (dwLength) ;
}


int main(int argc, char* argv[])
{
	HANDLE	hPort;
	FILE	*out = NULL;
	int		c = 1;
	char	*fileName = NULL;
	char	*portName = NULL;
	int		baudRate = 0;
	int		quiet = 0;

	progName = strrchr(argv[0], '\\') + 1;
	if (!progName) progName = argv[0];
	if (strrchr(progName, '.')) *(strrchr(progName, '.')) = '\0';

	while (c < argc) {
		if (!strcmp(argv[c], "-f")) {
			if (++c == argc) usage_exit("missing file name");
			fileName = argv[c];
			out = fopen(fileName, "wb");
			if (out == NULL) {
				error_exit("opening output file");
			}
		} else if (!strcmp(argv[c], "-p")) {
			if (++c == argc) usage_exit("missing port name");
			portName = argv[c];
		} else if (!strcmp(argv[c], "-b")) {
			if (++c == argc) usage_exit("missing baud rate");
			baudRate = atoi(argv[c]);
		} else if (!strcmp(argv[c], "-q")) {
			quiet = 1;
		} else if (!strcmp(argv[c], "-h")) {
			usage_exit(NULL);
		} else {
			usage_exit("unknown parameter");
		}
		c++;
	}

	if (!portName) portName = DEFAULT_COM;

	if (!out) {		// default to stdout
		out = stdout; fileName = "stdout";
		// set stdout to binary mode
		_setmode( _fileno( stdout ), _O_BINARY );
	}
	setvbuf(out, NULL, _IONBF, 0);  // unbuffered output

	if (!baudRate) baudRate = DEFAULT_BAUD;


	hPort = CreateFile(portName,
		GENERIC_READ | GENERIC_WRITE,
		0,
		NULL,
		OPEN_EXISTING,
		FILE_ATTRIBUTE_NORMAL, // | FILE_FLAG_OVERLAPPED,
		NULL);

	if (hPort == INVALID_HANDLE_VALUE) {
		error_exit("Opening COM port");
	}
    
	if (!SetCommMask( hPort, EV_RXCHAR )) {
		error_exit("SetCommMask");
	}

	SetupComm(hPort, DEV_RBUFSIZ, DEV_WBUFSIZ);

    if (!SetupConnection(hPort, baudRate)) {
		error_exit("SetupConnection");
	}


	if (!quiet) {
		fprintf(stderr, "%s: Copying %s at %d baud to %s\n", progName, portName, baudRate, fileName);
		fprintf(stderr, "Press ctrl-C to quit.\n");
		fflush(stderr);
	}

	while (TRUE) {			// loop until ^C
		DWORD	dwEvtMask ;
		int		nLength;
		BYTE	inBuf[512];

		dwEvtMask = 0;
		WaitCommEvent(hPort, &dwEvtMask, NULL);

		if ((dwEvtMask & EV_RXCHAR) == EV_RXCHAR) {
			do	{
				if (nLength = ReadCommBlock(hPort, (LPSTR)inBuf, sizeof(inBuf)-1))
				{
					fwrite((const void *)inBuf, nLength, 1, out);
					//fflush(out);
				}
			} while (nLength > 0) ;
		}
	}

	return 0;
}
