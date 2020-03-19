public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsizes[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory( int maxInumber ) { // directory constructor
        fsizes = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsizes[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][];
        String root = "/";                // entry(inode) 0 is "/"
        fsizes[0] = root.length( );        // fsize[0] is the size of "/".
        fnames[0] = new char[fsizes[0]];
        root.getChars( 0, fsizes[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    public int bytes2directory( byte data[] ) {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
        if(data.length == 0)
        {
            return -1;
        }

        int offset = 0;
        for(int i = 0; i < fsizes.length; i++)
        {
            fsizes[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }

        for(int i = 0; i < fsizes.length; i++)
        {
            String fileName = new String(data, offset, 60);
            //Saw getChars method used in constructor
            fileName.getChars(0, fsizes[i], fnames[i], 0);
            offset += 60;
        }

        return 1;
    }

    public byte[] directory2bytes( ) {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningfull directory information should be converted
        // into bytes.

        //The transition from integer to byte and that directory should have both fsize and fnames 
        byte[] directory = new byte[fsizes.length * 4 + fnames.length * 60];
        int offset = 0;

        for(int i = 0; i < fsizes.length; i++)
        {
            SysLib.int2bytes(fsizes[i], directory, offset);
            //int is 4 bytes so have to increment by 4
            offset += 4;
        }

        for(int i = 0; i < fnames.length; i++)
        {
            String fileName = new String(fnames[i], 0, fsizes[i]);
            byte[] data = fileName.getBytes();
            System.arraycopy(data, 0, directory, offset, data.length);
            offset += maxChars * 2;
        }

        return directory;
    }

    /// filename is the one of a file to be created.
    /// allocates a new inode number for this filename
    public short ialloc( String filename ) {
        for(int i = 0; i < fsizes.length; i++)
        {
            if(fsizes[i] == 0)
            {
                //this lets our for loop to not access any invalid values
                int lesserLength = 0;
                if(filename.length() > maxChars)
                {
                    lesserLength = maxChars;
                }
                else
                {
                    lesserLength = filename.length();
                }

                fsizes[i] = lesserLength;
                fnames[i] = new char[filename.length()];
                for(int j = 0; j < lesserLength; j++) 
                {
                    fnames[i][j] = filename.charAt(j);
                }

                
                //i is the new inode number
                return (short)i;
            }
        }

        //else list is full and cannot return an inode number, thus returning -1
        return -1;
    }

    public boolean ifree( short iNumber ) {
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
        if(iNumber < 0 || fsizes[iNumber] <= 0)
        {
            return false;
        }

        //just set everything in fnames at the iNumber index to 0 to reset all values

        for(int i = 0; i < fnames[iNumber].length; i++)
        {
            fnames[iNumber][i] = 0;
        }

        fsizes[iNumber] = 0;
        return true;
    }

    public short namei( String filename ) {
        // returns the inumber corresponding to this filename
        for(int i = 0; i < fsizes.length; i++)
        {
            if(fsizes[i] == filename.length())
            {
                if(filename.equals(new String(fnames[i])))
                {
                    return (short) i;
                }
            }
        }

        //if no corresponding file, then return -1 as file does not exist.
        return -1;
    }
}