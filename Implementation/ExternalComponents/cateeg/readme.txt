catEEG for windows

Usage: CATEEG [options]
Copy EEG data from COM port to stdout.

options are:
        -f <filename>   output to <filename> instead of stdout
                        WARNING: if <filename> exists it will be overwritten.
        -p <portname>   read from <portname> instead of COM1
        -b <baudrate>   set speed to <baudrate> instead of 57600
        -q              suppress start message
        -h              print this help message

Note that 8 bit data, no parity, 1 stop bit, no handshaking are always used.


I usually use this simultaneously with Jim Peter's BWView <http://www.uazu.net>
like so:

1) open a command window.  CD c:\path\to\cateeg folder.
   Run cateeg like this, e.g.:
	cateeg > c:\temp\eeg.dat            [ reads from COM1 ]
or	cateeg -p COM2 -f c:\temp\eeg.dat   [ reads from COM2 ]

2) open another command window.  CD c:\path\to\bwview
   Run bwview in "follow mode" like this:
	bwview -c F mod/256 c:\temp\eeg.dat

or 
	bwview mod/256 c:\temp\eeg.dat
        then press F to start follow mode.

3) turn on modularEEG.

Questions or problems to <jspaar@myrealbox.com>.

--Jack Spaar
edited 1/21/03