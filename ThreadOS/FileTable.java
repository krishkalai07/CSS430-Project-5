public class FileTable {

   private java.util.Vector<FileTableEntry> table;         //the actual entity of this file table
   private Directory dir;        //the root directory 

   public FileTable( Directory directory ) { //constructor

      table = new java.util.Vector<>();     //instantiate a file (structure) table
      dir = directory;           //receive a reference to the Directory
   }                             //from the file system

   //major public methods
   public synchronized FileTableEntry falloc( String filename, String mode ) {
      short inumber = dir.namei(filename);
      if ((mode == "r" && inumber == -1)) {
         //System.out.println("cat will slap a whiteboard if i see this");
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
         //System.out.println("kittens want to see this");
         // allocate a new file (structure) table entry for this file name
         inumber = dir.ialloc(filename);     // FIXME: make use of free list
         Inode inode = new Inode(inumber);
         inode.flag = 1;
         fte = new FileTableEntry(inode, inumber, mode);
         inode.count++; //thread usage count
         // immediately write back this inode to the disk
         System.out.println("!!!inumber: " + inumber);
         inode.toDisk(inumber);
         table.add(fte);
      }
      else {
         fte.inode.count++; //thread usage count
         fte.inode.flag = 1;
         switch(mode) {
            case "r":
            case "w":
            case "w+":
               fte.seekPtr = 0;
               break;
            case "a":
               fte.seekPtr = fte.inode.length;
               break;
            default:
               break;
         }
         fte.inode.toDisk(inumber);
      }

      //return a reference to this file (structure) table entry
      return fte;
   }

   /**
    * 
    * @param e a file table entry reference to be freed
    * @return true if it has been freed successfully
    */
   public synchronized boolean ffree(FileTableEntry fte) {
      //save the corresponding inode to the disk
      //free this file table entry.
      //return true if this file table entry found in my table
      for (int i = 0; i < table.size(); i++) {
         if (table.get(i).iNumber == fte.iNumber) {
            fte.inode.toDisk(fte.iNumber);
            fte.inode.flag = 0;
            table.remove(i);
            return true;
         }
      }
      return false;
   }

   public synchronized boolean ffree(short iNumber) {
      //save the corresponding inode to the disk
      //free this file table entry.
      //return true if this file table entry found in my table
      
      for (int i = 0; i < table.size(); i++) {
         if (table.get(i).iNumber == iNumber) {
            table.get(i).inode.toDisk(iNumber);
            table.remove(i);
            return true;
         }
      }
      return false;
   }

   public synchronized boolean fempty( ) {
      return table.isEmpty( );  //return if table is empty 
   }                            //should be called before starting a format
}
