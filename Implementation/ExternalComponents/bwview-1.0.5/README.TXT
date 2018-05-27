Welcome to BWView!
------------------

Here is a list of some of the files in this distribution:

TUTORIAL.TXT    A tutorial to get you quickly up-to-speed with running
		BWView and understanding its output.

NEWS.TXT	News on changes from previous versions

bwview.exe	The executable binary for Windows users
SDL.dll		The SDL library for Windows users

src		Directory containing the source code and build scripts

LOG001.DAT	Example data files provided by Jim Meissner (see 
EXAMPLE.DAT       http://www.MeissnerResearch.com).  Linux users need to
DOWNEY.DAT        download these separately from http://uazu.net/bwview/

BUILD.Linux	Build and install instructions for Linux users

BUILD.Win32     Build instructions for Win32/MinGW users.  This needs
		some more work as building on Win32 required some
		bodges.  However, Windows users normally won't need 
		to build the tool, because they can use the provided
		executable.

Windows users need to start the DOS prompt, CD to the 'bwview'
directory (i.e. wherever it was unpacked) and type 'bwview'.  If this
gives a welcome message and a list of options, then everything is
working fine.

Linux users need to build the executable, which means installing SDL
1.2 and FFTW if they are not already installed.  The BUILD.Linux file
outlines this procedure.

