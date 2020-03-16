public class Directory {
   private static int maxChars = 30; // max characters of each file name

   // Directory entries
   private int fsize[];        // each element stores a different file size.
   private char fnames[][];    // each element stores a different file name.

    /**
     * Constructs a directory with maxInumber files, and initializes root.
     * 
     * @param maxInumber The maximum number of files in the directory
     */
    public Directory( int maxInumber ) {
        fsize = new int[maxInumber];
        for ( int i = 0; i < maxInumber; i++ ) 
            fsize[i] = 0;
        fnames = new char[maxInumber][0];
        String root = "/";
        fsize[0] = root.length();
        fnames[0] = new char[1];
        root.getChars( 0, fsize[0], fnames[0], 0 );
    }

    // assumes data[] received directory information from disk
    // initializes the Directory instance with this data[]
    /**
     * Converts data received from disk to this directory object.
     * The data is formatted as [filename_size * 4, file_name ** filename_size, ...]. 
     * The data does not have a sentinel. Instead, the entire array is read.
     * 
     * @param data The data read from disk.
     * @return unkown
     */
    public int bytes2directory( byte data[] ) {
        for (int offset = 0, findex = 0; offset < data.length; findex++) {

            int fnameSize = SysLib.bytes2int(data, offset);
            offset += 4;
            fnames[findex] = new char[fnameSize];
            for (int i = 0; i < fnameSize; i++) {
                fnames[findex][i] = (char)data[offset + i];
            }
            offset += fnameSize;
        }
        return 0; // FIXME: check what it should return
    }

    /**
     * Converts this directory object into a byte array to be written to disk.
     * 
     * @return the directory data written to disk.
     */
    public byte[] directory2bytes() {
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

   /**
    * Creates a file with the name filename and assigns an iNumber.
    * Filename's length will be trimmed to maxChars if it exceeds it.
    *
    * @param filename the name of the file
    * @return the iNumber of this file. -1 if there is no available space
    */
   public short ialloc( String filename ) {
        // finds the index of fsize where it is zero (empty) and assign to it
        for (short i = 0; i < fsize.length; i++) {
            if (fsize[i] == 0) {
                if (filename.length() > maxChars) {
                    fsize[i] = maxChars;
                    filename = filename.substring(0, maxChars);
                } else {
                    fsize[i] = filename.length();
                }
                fnames[i] = filename.toCharArray();
                return i;
            }
        }
        return -1;  // ERROR
    }

    /**
     * Deallocates the iNumber.
     * 
     * @param iNumber The iNumber of the file to be deallocated.
     * @return true if the file has been deallocated, false otherwise.
     */
    public boolean ifree( short iNumber ) {
        if (iNumber >= 0 && iNumber < fsize.length && fsize[iNumber] > 0) {
            fsize[iNumber] = 0;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Searches for the file with the matching fileNames.
     * 
     * @param filename The filename to search for
     * @return the iNumber of the corresponding file, or -1 if not found.
     */
    public short namei( String filename ) {
        //System.out.println("target: " + filename);
        for (short i = 0; i < fsize.length; i++) {
            //System.out.println(i + " " + fnames[i].length);
            if (filename.equals(new String(fnames[i]))) {
                return i;
            }
        }
        return -1;
    }
}
