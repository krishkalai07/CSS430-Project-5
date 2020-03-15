public class SuperBlock {
   public int totalBlocks; // the number of disk blocks
   public int totalInodes; // the number of inodes
   public int freeList;    // the block number of the free list's head
  
   public SuperBlock( int diskSize ) {
      byte[] superBlock = new byte[512];
      SysLib.rawread(0, superBlock);
      totalBlocks = SysLib.bytes2int(superBlock, 0);
      totalInodes = SysLib.bytes2int(superBlock, 4);
      freeList = SysLib.bytes2int(superBlock, 8);

      // Validate disk contents
      if (totalBlocks != diskSize || totalInodes == 0 || freeList < 1) {
         // Disk data is invalid
         totalInodes = 16;
         freeList = 1;
      }
  }

   public void sync(){ 
      byte[] superBlock = new byte[Disk.blockSize];
      SysLib.int2bytes(totalBlocks, superBlock, 0);
      SysLib.int2bytes(totalInodes, superBlock, 4);
      SysLib.int2bytes(freeList, superBlock, 8);
      SysLib.rawwrite(0, superBlock);
  }

   // write super block to disk
   /** 
    * 
    * @param fileCount number of files to be stored
    */
   public void format(int fileCount) { 
      totalBlocks = 1000;
      totalInodes = fileCount;
      freeList = fileCount / 16 +  1;

      sync();
   }
}
