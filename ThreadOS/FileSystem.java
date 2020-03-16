public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    java.util.HashSet<Short> takenBlocks; 
 
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

       takenBlocks = new java.util.HashSet<>();
       takenBlocks.add((short)0); // Adds the superblock
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
        return filetable.falloc(filename, mode);
    }

    boolean close(FileTableEntry ftEnt) {
        // Closes the file corresponding to fd, commits all file transactions on this file, 
        // and unregisters fd from the user file descriptor table of the calling thread's TCB. 
        // The return value is 0 in success, otherwise -1.
        // filetable.ffree(ftEnt);
        ftEnt.inode.flag = 0;
        ftEnt.inode.count--;
        return true;
    }

    /**
     * @return the size of file in the file table entry
     */
    int fsize(FileTableEntry ftEnt) {
        return ftEnt.inode.length;
    }

    /**
     * 
     * @param ftEnt associated with the file to be read
     * @param buffer byte[] to read the file into
     * @return the size of the file read into the buffer; -1 if fails reading
     */
    int read(FileTableEntry ftEnt, byte[] buffer) {
        int temp = ftEnt.seekPtr;
        Inode inode = ftEnt.inode;
        int directIndex = 0;
        while (temp > 512) {
            short inumber = inode.indirect;
            inode = new Inode(inumber);
            temp = temp / 512;
            directIndex++;
        }
     
        byte[] readFromDisk = new byte[512];
        if (SysLib.rawread(inode.direct[directIndex], readFromDisk) == -1) {
            return -1;
        }

        System.arraycopy(readFromDisk, temp, buffer, 0, buffer.length);
        ftEnt.seekPtr += buffer.length;
        return buffer.length;
    }

    /**
     * Writes the contents of buffer to the file indicated by fd, starting at the position indicated by
     * the seek pointer. The operation may overwrite existing data in the file and/or append to the end
     * of the file. SysLib.write increments the seek pointer by the number of bytes to have been written.
     * The return value is the number of bytes that have been written, or a negative value upon an error.
     * 
     * @param ftEnt associated with the file to be written 
     * @param buffer byte array that contains the contents to be wrriten
     * @return the size of written contents; -1 if fails writing
     */
    int write(FileTableEntry ftEnt, byte[] buffer) {
        // Test for availability
        if (ftEnt.inode.flag == 0) {
            return -1;
        }
        // params:(byte[] buffer, Inode inode, String mode, int blockIndex, int offset)
        int prevSeekPtr = ftEnt.seekPtr;
        int temp = writeByBlocksToDisk(buffer, ftEnt.inode, ftEnt.mode, ftEnt.seekPtr/ 512, ftEnt.seekPtr % 512);
        ftEnt.seekPtr += temp;
        return ftEnt.seekPtr - prevSeekPtr;


    }

    /**
     * 
     * @param buffer the data to be written to disk. (full array)
     * @param inode the inode pointing to the blocks to write to
     * @param blockIndex the block in inode.direct
     * @param offset the position in the block specified by blockIndex
     * 
     * @return a new seek pointer location
     * 
     * @since 1997/06/12
     */ 
    private int writeByBlocksToDisk(byte[] buffer, Inode inode, String mode, int blockIndex, int offset) {
        int bufferIndex = 0; //the location of the buffer to write
        int sum = 0;
        int pointerIndex = 0;
        byte[] readFromDisk = new byte[512];

        // Use the direct blocks
        while (pointerIndex < 11 && bufferIndex < buffer.length) {
            SysLib.rawread(inode.direct[pointerIndex], readFromDisk);
            takenBlocks.add(inode.direct[pointerIndex]);  
            
            if (bufferIndex >= 512) {
                // allocate a new block and write to it
                System.arraycopy(buffer, bufferIndex, readFromDisk, 0, 512);
                bufferIndex += 512;
                sum += 512;
                offset += 512;
                inode.length += 512;
                inode.direct[++pointerIndex] = findNextFreeBlock();
            } else {
                System.arraycopy(buffer, bufferIndex, readFromDisk, offset, buffer.length - bufferIndex);

                bufferIndex += buffer.length; 
                sum += bufferIndex % 512;
                inode.length += (buffer.length % 512);
                offset += buffer.length;
            }
           
            SysLib.rawwrite(inode.direct[pointerIndex], readFromDisk);  
            
        } 
        
        // indirect pointer
        if (inode.flag != 2 && bufferIndex < buffer.length && pointerIndex >= 11) {
            // Use the indirect block 
            if (inode.indirect == -1) {
                // allocate the new entry and inode
                FileTableEntry fte = filetable.falloc(null, mode);
                fte.inode.flag = 2;
                inode.indirect = fte.iNumber;   
            }
            Inode next = filetable.inodeFromiNumber(inode.indirect);

            sum += writeByBlocksToDisk(buffer, next, mode, blockIndex, offset);
        }
        return sum;
    }

    /** 
     * @return the next free block number; returns -1 if there is no free block
     */
    private short findNextFreeBlock() {
        for(short i = 1; i < 1000; i++) {
            if (!takenBlocks.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 
     * @param fileName to be deleted
     * @return true if it is closed and can be deleted; returns false otherwise
     */
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
 
    /**
     * Updates the seek pointer corresponding to fd as follows:
     *
     * If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes from the beginning of the file
     * If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current value plus the offset. The offset can be positive or negative.
     * If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the file plus the offset. The offset can be positive or negative.
     */
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;
 
    /** 
     * If the user attempts to set the seek pointer to a negative number you must clamp it to zero.
     * If the user attempts to set the pointer to beyond the file size, you must set the seek pointer to the end of the file. The offset loction of the seek pointer in the file is returned from the call to seek.
     * 
     * @param ftEnt
     * @param offset
     * @param whence
     * @return
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
        return ftEnt.seekPtr;
    }
 }
 
