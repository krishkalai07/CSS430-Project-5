public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
 
    public FileSystem(int diskBlocks) {
       // create superblock, and format disk with 64 inodes in default
       superblock = new SuperBlock(diskBlocks);
 
       // create directory, and register "/" in directory entry 0
       directory = new Directory(superblock.totalInodes);
 
       // file table is created, and store directory in the file table
       filetable = new FileTable(directory);
 
       // directory reconstruction
       FileTableEntry dirEnt = open("/", "r");
       int dirSize = fsize(dirEnt);
       if (dirSize > 0) {
          byte[] dirData = new byte[dirSize];
          read(dirEnt, dirData);
          directory.bytes2directory(dirData);
       }
       close(dirEnt);
    }
 
    //syncs the file system back to the physical disk and write the directory information 
    public void sync() {
        //open root with write access
        FileTableEntry root = open("/", "w");
        //write to root
        write(root, directory.directory2bytes());
        //close root 
        close(root);
        //sync superblock
        superblock.sync();
    }

    /**
     * 
     * 
     * @param files the maxinum number of files to create.
     * @return
     */
    public boolean format(int files) {
        //format superblocks
        superblock.format(files);
        //create directory,
        directory = new Directory(superblock.totalInodes);
        //file table is created and store directory in the file table
        filetable = new FileTable(directory);
        //return true once completed
        return true;
    }


    FileTableEntry open(String filename, String mode) {
        FileTableEntry fte = filetable.falloc(filename, mode);

        return fte;
      
    }

    boolean close(FileTableEntry ftEnt) {
        return false;
    }

    int fsize(FileTableEntry ftEnt) {
        return 0;
    }

    int read(FileTableEntry ftEnt, byte[] buffer) {
        return 0;
    }

    int write(FileTableEntry ftEnt, byte[] buffer) {
        return 0;
    }

    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        return false;
    }
    
    boolean delete(String filename) {
        return false;
    }
 
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;
 
    int seek(FileTableEntry ftEnt, int offset, int whence) {
        return false;
    }
 }
 
