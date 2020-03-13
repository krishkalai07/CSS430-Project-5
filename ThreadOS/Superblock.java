public class SuperBlock {
   public int totalBlocks; // the number of disk blocks
   public int totalInodes; // the number of inodes
   public int freeList;    // the block number of the free list's head
  
   public SuperBlock( int diskSize ) {
      byte[] blockInfo = new byte[512];

      SysLib.rawread(0, blockInfo);

      totalBlocks = SysLib.bytes2int(blockInfo, 0);
      if (totalBlocks == 0) { // disk does not contain information
         // compute values
         // write to disk
         // totalBlocks = 
          
      } else { // disk already contains the information
         totalInodes = SysLib.bytes2int(blockInfo, 4);
         freeList = SysLib.bytes2int(blockInfo, 8);
      }
   }

}