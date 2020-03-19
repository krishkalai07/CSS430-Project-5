
public class FileSystem  {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    private java.util.Set<Short> consumedBlocks;

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

        consumedBlocks = new java.util.HashSet<>();
    }

    private short nextFreeBlock() {
        for (short i = (short)superblock.freeList; i < 1000; i++) {
            if (!consumedBlocks.contains(i)) {
                return i;
            }
        }

        return -1;
    }

    boolean format(int files) {
        return superblock.format(files);
    }

    FileTableEntry open(String filename, String mode) {
        return filetable.falloc(filename, mode);
    }

    boolean close(FileTableEntry ftEnt) {
        ftEnt.count--;
        if (ftEnt.count == 0) {
            filetable.ffree(ftEnt);
        }

        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }

    int fsize(FileTableEntry ftEnt) {
        return ftEnt.inode.length;
    }

    int read(FileTableEntry ftEnt, byte[] buffer) {
        // only valid modes are read and read/write
        // if thread count is 0, file is not open
        if (ftEnt.mode == "w" || ftEnt.mode == "a" || ftEnt.count == 0) {
            return -1;
        }
        
        // nothing to read if seek pointer is at EOF
        if (ftEnt.seekPtr == ftEnt.inode.length) {
            return 0;
        }

        int inodeDirectIndex = ftEnt.seekPtr / 512;
        int blockOffset = ftEnt.seekPtr % 512;
        int bufferOffset = 0;
        byte[] temp = new byte[512];

        // direct
        while (inodeDirectIndex < ftEnt.inode.direct.length && ftEnt.inode.direct[inodeDirectIndex] > 0 && bufferOffset < buffer.length) {
            int bytesRead = Math.min(temp.length - blockOffset, buffer.length - bufferOffset);
            SysLib.rawread(ftEnt.inode.direct[inodeDirectIndex], temp);
            System.arraycopy(temp, blockOffset, buffer, bufferOffset, bytesRead);

            bufferOffset += bytesRead;
            ftEnt.seekPtr += bytesRead;
            blockOffset = 0;
            inodeDirectIndex++;
        }
        
        // indirect
        if (inodeDirectIndex >= ftEnt.inode.direct.length && bufferOffset < buffer.length) {
            byte[] indirectBlock = new byte[512];
            SysLib.rawread(ftEnt.inode.indirect, indirectBlock);
            int indirectOffset = (inodeDirectIndex - 11) * 2;

            while(bufferOffset < buffer.length){
                short indirectBlockNum = SysLib.bytes2short(indirectBlock, indirectOffset);
                SysLib.rawread(indirectBlockNum, temp);
                int bytesRead = Math.min(temp.length - blockOffset, buffer.length - bufferOffset);
                System.arraycopy(temp, blockOffset, buffer, bufferOffset, bytesRead);
                bufferOffset += bytesRead;
                ftEnt.seekPtr += bytesRead;

                indirectOffset += 2;
            }
            
        }

        return bufferOffset;
    }

    int write(FileTableEntry ftEnt, byte[] buffer) {
        // cannot write when in read mode
        if (ftEnt.mode == "r" || ftEnt.count == 0) {
            return -1;
        }
        
        int inodeDirectIndex = ftEnt.seekPtr / 512;
        int blockOffset = ftEnt.seekPtr % 512;
        int bufferOffset = 0;
        byte[] temp = new byte[512];

        if (inodeDirectIndex > 512 + 11) {
            // fully out of space
            return -1;
        }

        //allocate the first block if necessary
        
        // direct section
        while (inodeDirectIndex < ftEnt.inode.direct.length && bufferOffset < buffer.length) {
            if (ftEnt.inode.direct[inodeDirectIndex] <= 0) {
                short nextBlock = nextFreeBlock();
                ftEnt.inode.direct[inodeDirectIndex] = nextBlock;
                consumedBlocks.add(nextBlock);
            }

            SysLib.rawread(ftEnt.inode.direct[inodeDirectIndex], temp);
            
            int bytesRead = Math.min(temp.length - blockOffset, buffer.length - bufferOffset);

            System.arraycopy(buffer, bufferOffset, temp, blockOffset, bytesRead);
            SysLib.rawwrite(ftEnt.inode.direct[inodeDirectIndex], temp);

            bufferOffset += bytesRead;
            ftEnt.seekPtr += bytesRead;
            ftEnt.inode.length = Math.max(ftEnt.seekPtr, ftEnt.inode.length)
            // if (ftEnt.seekPtr > ftEnt.inode.length) {
            //     ftEnt.inode.length = ftEnt.seekPtr;
            // }
            blockOffset = 0;
            inodeDirectIndex++;
        }
        
        //indirect
        if (inodeDirectIndex >= ftEnt.inode.direct.length && bufferOffset < buffer.length) {
            // allocate the indirect block
            int indirectOffset = 0;
            if (ftEnt.inode.indirect <= 0) {
                short nextBlock = nextFreeBlock();
                ftEnt.inode.indirect = nextBlock;
                consumedBlocks.add(nextBlock);
            }
            else {
                indirectOffset = (inodeDirectIndex - 11) * 2;
            }
            
            byte[] indirectBlock = new byte[512];
            SysLib.rawread(ftEnt.inode.indirect, indirectBlock);

            while(bufferOffset < buffer.length){
                short indirectBlockNum = SysLib.bytes2short(indirectBlock, indirectOffset);
                // allocate the next block
                if (indirectBlockNum <= 0) {
                    indirectBlockNum = nextFreeBlock();
                    SysLib.short2bytes(indirectBlockNum, indirectBlock, indirectOffset);
                    consumedBlocks.add(indirectBlockNum);
                }
                SysLib.rawread(indirectBlockNum, temp);
                int bytesRead = Math.min(temp.length - blockOffset, buffer.length - bufferOffset);
                System.arraycopy(buffer, bufferOffset, temp, blockOffset, bytesRead);
                SysLib.rawwrite(indirectBlockNum, temp);
                ftEnt.seekPtr += bytesRead;
                ftEnt.inode.length = Math.max(ftEnt.seekPtr, ftEnt.inode.length)
                // if (ftEnt.seekPtr > ftEnt.inode.length) {
                //     ftEnt.inode.length = ftEnt.seekPtr;
                // }
                bufferOffset += bytesRead;
                blockOffset = 0;
                indirectOffset += 2;
            } 
            SysLib.rawwrite(ftEnt.inode.indirect, indirectBlock);
        }
        
        // ftEnt.inode.toDisk(ftEnt.iNumber);
        return bufferOffset;
    }

    boolean delete(String filename) 
    {
        short iNumber = directory.namei(filename);
        if (iNumber == -1) {
            // file does not exist
            return false;
        }
        Inode inode = new Inode(iNumber);
        if (filetable.fcontains(iNumber)) {
            inode.flag = 2;
            return false;
        }

        if (!directory.ifree(iNumber)) {
            return false;
        }
        
        for (short blockNum : inode.direct) {
            consumedBlocks.remove((short)blockNum);
        }

        if (inode.indirect > 0) {
            byte[] blocks = new byte[512];
            SysLib.rawread(inode.indirect, blocks);
            short s = 0;
            int offset = 0;
            while ((s = SysLib.bytes2short(blocks, offset)) >= 0) {
                consumedBlocks.remove((short)s);
                offset += 2;
            }
        }

        return true;
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    int seek(FileTableEntry ftEnt, int offset, int whence) {
        // big brain time
        return (ftEnt.seekPtr = Math.max(0, Math.min(whence == SEEK_SET ? 0 + offset : whence == SEEK_CUR ? ftEnt.seekPtr + offset : ftEnt.inode.length + offset, ftEnt.inode.length)));

        // little uni-cell 
        // if(whence == SEEK_SET){
        //     ftEnt.seekPtr = 0 + offset;
        // }else if(whence == SEEK_CUR){
        //     ftEnt.seekPtr+=offset;
        // }else if (whence == SEEK_END){
        //     ftEnt.seekPtr = ftEnt.inode.length + offset;
        // }

        // if(ftEnt.seekPtr < 0){
        //     ftEnt.seekPtr = 0;
        // }

        // if(ftEnt.seekPtr > ftEnt.inode.length){
        //     ftEnt.seekPtr = ftEnt.inode.length;
        // }

        // return ftEnt.seekPtr;
    }
}
