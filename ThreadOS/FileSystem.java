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
        if (fte == null) {
            return null;
        }
        return fte;
    }

    boolean close(FileTableEntry ftEnt) {
        // Closes the file corresponding to fd, commits all file transactions on this file, 
        // and unregisters fd from the user file descriptor table of the calling thread's TCB. 
        // The return value is 0 in success, otherwise -1.
        ftEnt.inode.count--;
        // FIXME: is this all we do in close? o we need to kick out a TCB (A.E.) or smth or this out of TCB?
        return true;
    }

    int fsize(FileTableEntry ftEnt) {
        return ftEnt.inode.length;
    }

    int read(FileTableEntry ftEnt, byte[] buffer) {
        int temp = ftEnt.seekPtr;
        int index = ftEnt.seekPtr / 512;    // index of a direct/indirect array–
        Inode inode = ftEnt.inode;
        while (temp > 512) {
            short inumber = inode.indirect;
            inode = new Inode(inumber);
            temp = temp / 512;
        }
        int blockNumber = temp;

        byte[] readFromDisk = new byte[512];
        if (SysLib.rawread(inode.direct[blockNumber], readFromDisk) == -1) {
            return -1;
        }
        System.arraycopy(readFromDisk, temp, buffer, 0, buffer.length);
        
        return 0;
    }

    /**
     * Writes the contents of buffer to the file indicated by fd, starting at the position indicated by
     * the seek pointer. The operation may overwrite existing data in the file and/or append to the end
     * of the file. SysLib.write increments the seek pointer by the number of bytes to have been written.
     * The return value is the number of bytes that have been written, or a negative value upon an error.
     * @param ftEnt
     * @param buffer
     * @return
     */
    int write(FileTableEntry ftEnt, byte[] buffer) {
        byte[] readFromDisk = new byte[512];

        Inode inode = ftEnt.inode;
        int blockIndex = ftEnt.seekPtr / 512;
        if (blockIndex < ftEnt.inode.direct.length) {       // direct
            int blockNumber = inode.direct[blockIndex];
            SysLib.rawread(blockNumber, readFromDisk);
            System.arraycopy(buffer, 0, readFromDisk, ftEnt.seekPtr % 512, buffer.length);  // TODO: assuming that the data does not go out of the block

            SysLib.rawwrite(blockNumber, readFromDisk);
        } else {                                            // indirect
            //TODO: FIXME: hi hi hi
        }

        return 0;
    }

    boolean delete(String fileName) {
        short inumber = directory.namei(fileName);
        Inode inode = new Inode(inumber);
        // If the file is currently open, it is not deleted and the operation returns a -1.
        if (inode.count == 0) {
            return false;
        }

        // Deletes the file specified by fileName.
        // All blocks used by file are freed.
        
        // If successfully deleted a 0 is returned.
        return filetable.ffree(inumber);
    }
 
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;
 
    /**
     * Updates the seek pointer corresponding to fd as follows:
     *
     * If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes from the beginning of the file
     * If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current value plus the offset. The offset can be positive or negative.
     * If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the file plus the offset. The offset can be positive or negative.
     *
     * If the user attempts to set the seek pointer to a negative number you must clamp it to zero.
     * If the user attempts to set the pointer to beyond the file size, you must set the seek pointer to the end of the file. The offset loction of the seek pointer in the file is returned from the call to seek.
     */
    int seek(FileTableEntry ftEnt, int offset, int whence) {
        
        switch (whence) {
            case SEEK_SET:
                ftEnt.seekPtr = 0;
                break;
            case SEEK_CUR: 
                break;
            case SEEK_END:
                ftEnt.seekPtr = ftEnt.inode.length;
                break;
            default:
                return -1;
        }

        ftEnt.seekPtr += offset;
        if (ftEnt.seekPtr < 0) {
            ftEnt.seekPtr = 0;
        } else if (ftEnt.seekPtr > ftEnt.inode.length) {
            ftEnt.seekPtr = ftEnt.inode.length;
        }
        return 0;
    }
 }
 
