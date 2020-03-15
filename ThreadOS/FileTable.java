public class FileTable {

   private java.util.Vector<FileTableEntry> table;         //the actual entity of this file table
   private Directory dir;        //the root directory 

   public FileTable( Directory directory ) { //constructor
      table = new java.util.Vector<>();     //instantiate a file (structure) table
      dir = directory;           //receive a reference to the Director
   }                             //from the file system

   //major public methods
   public synchronized FileTableEntry falloc( String filename, String mode ) {
      short inumber = dir.namei(filename);
      if ((mode == "r" && inumber == -1)) {
         return null;
      }
      FileTableEntry fte = null;
      for (FileTableEntry fileTableEntry : table) {
         if (inumber == fileTableEntry.iNumber) {
            fte = fileTableEntry;
            break;
         }
      }

      //allocate/retrieve and register the corresponding inode using dir
      if (fte == null) {
         //allocate a new file (structure) table entry for this file name
         Inode inode = new Inode(inumber);
         fte = new FileTableEntry(inode, inumber, mode);
         inode.count++; //thread usage count
         //immediately write back this inode to the disk
         inode.toDisk(inumber);
         table.add(fte);
      }
      else {
         fte.inode.count++; //thread usage count
      }

      //return a reference to this file (structure) table entry
      return fte;
   }

   public synchronized boolean ffree( FileTableEntry e ) {
      //receive a file table entry reference
      //save the corresponding inode to the disk
      //free this file table entry.
      //return true if this file table entry found in my table
   }

   public synchronized boolean fempty( ) {
      return table.isEmpty( );  //return if table is empty 
   }                            //should be called before starting a format
}
