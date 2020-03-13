public class Superblock {
   public int totalBlocks; // the number of disk blocks
   public int totalInodes; // the number of inodes
   public int freeList;    // the block number of the free list's head
   private byte[] superBlock;

   public SuperBlock( int diskSize ) {
        superBlock = new byte[512];
        SysLib.rawread()

        totalBlocks = SysLib.bytes2int(superBlock, 0);

        totalInodes = 16 * totalBlocks;

       // freeList = 1;
   }

}