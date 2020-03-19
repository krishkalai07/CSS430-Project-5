import java.util.Vector;

public class FileTable {

    private Vector<FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector<>();     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    } // from the file system
                                
    //
    // major public methods
    //

    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        short iNumber = dir.namei(filename);
        if (iNumber == -1) {
            // cannot create a file on read
            if (mode == "r") {
                return null;
            }
            iNumber = dir.ialloc(filename);
        }
        
        Inode inode = new Inode(iNumber);
        
        if (inode.flag == 2) {
            return null;
        }
        // increment this inode's count
        inode.count++;

        // immediately write back this inode to the disk
        inode.toDisk(iNumber);

        // allocate a new file (structure) table entry for this file name
        FileTableEntry fte = new FileTableEntry(inode, iNumber, mode);
        table.add(fte);

        return fte;
     }

     public synchronized boolean fcontains(int iNumber) {
         for (FileTableEntry fte : table) {
             if (fte.iNumber == iNumber) {
                 return true;
             }
         }
         return false;
     }

    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table   
    public synchronized boolean ffree(FileTableEntry e) {
        return table.remove(e);
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                            // should be called before starting a format
}