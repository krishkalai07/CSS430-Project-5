
public class SuperBlock {
    /**
     * Total number of blocks in the disk. 
     * For this assignment, it is <eq> 1000.
     */
    public int totalBlocks;

    /**
     * The number of inodes in the disk. Is <eq> to number of files.
     */
    public int totalInodes;

    /**
     * First avaiable block (free or not). Must be <ge> 2.
     */
    public int freeList;


    public SuperBlock( int diskSize ) {
        byte [] data = new byte[512]; //byte array of super block

        SysLib.rawread(0,data);
        totalBlocks = SysLib.bytes2int(data,0);
        totalInodes = SysLib.bytes2int(data,4);
        freeList = SysLib.bytes2int(data,8);

        if(totalBlocks != diskSize || totalInodes == 0 || freeList < 2){
            //invalid
            totalBlocks = diskSize;
            totalInodes = 64;
            //this.format(totalInodes); //FIXME: Don't forget to uncomment before submission
        }

    }

    /**
     * Formats the disk by writing superblock's data to disk, and clearing all the data in blocks 1 to 999.
     */
    public boolean format (int files){
        byte [] data = new byte[512];

        totalInodes = files;
        freeList = totalInodes/16 + 1;

        SysLib.int2bytes(totalBlocks,data,0);
        SysLib.int2bytes(totalInodes,data,4);
        SysLib.int2bytes(freeList,data,8);
        SysLib.rawwrite(0,data);

        // System.out.println("SuperBlock.format():totalBlocks" + totalBlocks);
        // System.out.println("SuperBlock.format():totalInodes" + totalInodes);
        // System.out.println("SuperBlock.format():freeList" + freeList);

        byte [] temp = new byte[512];
        for(int i = 1; i < totalBlocks; i++){
            SysLib.rawwrite(i,temp);
        }
        return true;
    }

    public void sync(){
        //write back to disk to update free list and variables
        byte [] temp = new byte[512];
        SysLib.int2bytes(totalBlocks,temp,0);
        SysLib.int2bytes(totalInodes,temp,4);
        SysLib.int2bytes(freeList,temp,8);
        SysLib.rawwrite(0,temp);
    }
}