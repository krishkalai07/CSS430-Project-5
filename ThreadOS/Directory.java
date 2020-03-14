public class Directory {
   private static int maxChars = 30; // max characters of each file name

   // Directory entries
   private int fsize[];        // each element stores a different file size.
   private char fnames[][];    // each element stores a different file name.

    public Directory( int maxInumber ) { // directory constructor
        fsize = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ ) 
            fsize[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fsize[0] = root.length( );        // fsize[0] is the size of "/".
        root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    // assumes data[] received directory information from disk
    // initializes the Directory instance with this data[]
    public int bytes2directory( byte data[] ) {
        for (int offset = 0, findex = 0; offset < data.length; findex++) {

            int fnameSize = SysLib.bytes2int(data, offset);
            offset += 4;
            fnames[findex] = new char[fnameSize];
            for (int i = 0; i < fnameSize; i++) {
                fnames[findex][i] = (char)data[offset + i];// read in the filename
            }
            offset += fnameSize;
        }
        return 0; // FIXME: check what it should return
    }

   public byte[] directory2bytes() {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningfull directory information should be converted
        // into bytes.

        byte[] barr = new byte[512]; 
        int offset = 0;

        for (int f = 0; f < fnames.length; f++) {
            SysLib.int2bytes(fsize[f], barr, offset);   // copy the file size
            offset += 4;
            for (int i = 0; i < fsize[f]; i++) {        // copy the file name to byte array
                barr[offset + i] = (byte)fnames[f][i];
            }
            offset += fsize[f];
        }
        return barr;
   }

   public short ialloc( String filename ) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename

        // finds the index of fsize where it is zero (empty) and assign to it
        for (short i = 0; i < fsize.length; i++) {
            if (fsize[i] == 0) {
                if (filename.length() > maxChars) {
                    fsize[i] = maxChars;
                    filename = filename.substring(0, maxChars);     // FIXME: I cut the string if it is longer than maxChars
                } else {
                    fsize[i] = filename.length();
                }
                fnames[i] = filename.toCharArray();
                return i;
            }
        }
        return -1;  // ERROR
    }

    public boolean ifree( short iNumber ) {
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
        if (iNumber < fsize.length && fsize[iNumber] > 0) { // FIXME: need (iNumber < MAX_CHARS)? cant it be (iNumber < fisze.length)
            fsize[iNumber] = 0;
            return true;
        } else {
            return false;
        }
    }

    // -1 if it is not there
    public short namei( String filename ) {
        // returns the inumber corresponding to this filename
        for (short i = 0; i < fsize.length; i++) {
            if (filename.equals(new String(fnames[i]))) {
                return i;
            }
        }
        return -1;      // ERROR
    }

}
