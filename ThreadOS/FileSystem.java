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
        // System.out.println("open: " + mode);
        return filetable.falloc(filename, mode);
    }

    boolean close(FileTableEntry ftEnt) {
        // Closes the file corresponding to fd, commits all file transactions on this file, 
        // and unregisters fd from the user file descriptor table of the calling thread's TCB. 
        // The return value is 0 in success, otherwise -1.
        // filetable.ffree(ftEnt);
        ftEnt.inode.flag = 0;
        ftEnt.inode.count--;
        // System.out.println("close::seek: " + ftEnt.seekPtr);
        return true;
    }

    int fsize(FileTableEntry ftEnt) {
        return ftEnt.inode.length;
    }

    int read(FileTableEntry ftEnt, byte[] buffer) {
        int temp = ftEnt.seekPtr;
        System.out.println("FS::read::seekPtr = " + temp);
        Inode inode = ftEnt.inode;
        int directIndex = 0;
        while (temp > 512) {
            short inumber = inode.indirect;
            inode = new Inode(inumber);
            temp = temp / 512;
            directIndex++;
        }
        System.out.println("FS::read::directIndex = " + directIndex);
        System.out.println("FS::read::direct = " + java.util.Arrays.toString(inode.direct));
        // System.out.println("p6");
        byte[] readFromDisk = new byte[512];
        if (SysLib.rawread(inode.direct[directIndex], readFromDisk) == -1) {
            return -1;
        }
        // System.out.println("seekptr bfr: " + ftEnt.seekPtr);
        // System.out.println(java.util.Arrays.toString(readFromDisk));
        // System.out.println(java.util.Arrays.toString(buffer));
        System.arraycopy(readFromDisk, temp, buffer, 0, buffer.length);
        ftEnt.seekPtr += buffer.length;
        // System.out.println("seekptr aft: " + ftEnt.seekPtr);
        // System.out.println(java.util.Arrays.toString(readFromDisk));
        // System.out.println(java.util.Arrays.toString(buffer));
        return buffer.length;
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

        // Test for availability
        if (ftEnt.inode.flag == 0) {
            return -1;
        }

        Inode inode = ftEnt.inode;
        // int blockIndex = ftEnt.seekPtr / 512;
        int temp = ftEnt.seekPtr;
        int directIndex = 0;
        while (temp > 512) {
            short inumber = inode.indirect;
            inode = new Inode(inumber);
            temp = temp / 512;
            directIndex++;
        }
        if (directIndex < ftEnt.inode.direct.length) {       // direct
            //System.out.println("first");
            int bufferIndex = 0;

            for(;bufferIndex < buffer.length;) {
                int blockNumber = inode.direct[directIndex];
                System.out.println("--i am BEFORE rawread--");
                System.out.println(" direct Index: " + directIndex);

                SysLib.rawread(blockNumber, readFromDisk);
                System.out.println("i am AFTER rawread");

                takenBlocks.add((short)blockNumber);
                if (buffer.length >= 512) {
                    System.out.println("buffer Index: " + bufferIndex + ", seekPtr: " + ftEnt.seekPtr);
                    
                    System.arraycopy(buffer, bufferIndex, readFromDisk, 0, 512); // changed ftEnt.seekPtr to 0
                    bufferIndex += 512;
                    ftEnt.seekPtr += 512;
                    inode.direct[++directIndex] = findNextFreeBlock();
                }
                else {
                    System.arraycopy(buffer, bufferIndex, readFromDisk, ftEnt.seekPtr, buffer.length);
                    bufferIndex += buffer.length - bufferIndex;
                    ftEnt.seekPtr += buffer.length - bufferIndex;
                }
                SysLib.rawwrite(blockNumber, readFromDisk);
            }
            ftEnt.inode.length += buffer.length;
            return buffer.length;
        }

        //TODO: indirect

        return -1;
    }

    private short findNextFreeBlock() {
        for(short i = 1; i < 1000; i++) {
            if (!takenBlocks.contains(i)) {
                return i;
            }
        }
        return -1;
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
        //System.out.println("sawfwfwfwefef");

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
        //System.out.println("seek: " + ftEnt.seekPtr);
        return ftEnt.seekPtr;
    }
 }
 
