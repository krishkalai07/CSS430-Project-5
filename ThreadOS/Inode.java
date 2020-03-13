public class Inode {
   private final static int iNodeSize = 32;       // fix to 32 bytes
   private final static int directSize = 11;      // # direct pointers

   public int length;                             // file size in bytes
   public short count;                            // # file-table entries pointing to this
   public short flag;                             // 0 = unused, 1 = used, ...
   public short direct[] = new short[directSize]; // direct pointers
   public short indirect;                         // a indirect pointer
 
   Inode() {                                     // a default constructor
      length = 0;
      count = 0;
      flag = 1;
      for ( int i = 0; i < directSize; i++ ) {
         direct[i] = -1;
      }
      indirect = -1;
   }
 
   Inode(short iNumber) {                       // retrieving inode from disk
      int blockNumber = 1 + iNumber / 16;
      int iNodeIndex = iNumber % 16;

      byte[] barr = new byte[512];

      if (rawread(blockNumber, barr) == Kernel.error) {
         SysLib.cerr("Failed to rawread for Inode");
         throw new Exception();
      }

      // the spot we need to start from is (inodeIndex * 32) to (indodeIndex * 32 + 31)
      byte[] iNodeData = new byte[iNodeSize];
      System.arraycopy(barr, iNodeIndex * 32, iNodeData, 0, 32);

      length = SysLib.bytes2int(barr, 0);
      count = SysLib.bytes2short(barr, 4);
      flag = SysLib.bytes2short(barr, 6);

      int offset = 8;
      for (int i = 0; i < directSize; i++, offset += 2) {
         direct[i] = SysLib.bytes2short(barr, offset);
      }
      indirect = SysLib.bytes2short(barr, offset);
    }
 
   int toDisk(short iNumber) {                  // save to disk as the i-th inode
       // design it by yourself.
      int blockNumber = 1 + iNumber / 16;
      int iNodeIndex = iNumber % 16;

      byte[] barr = new byte[512];
      SysLib.rawread(blockNumber, barr);

      byte[] iNodeData = new byte[iNodeSize];
      SysLib.int2bytes(length, barr, 0);
      SysLib.short2Bytes(count, barr, 4);
      SysLib.short2Bytes(flag, barr, 6);

      int offset = 8;
      for (int i = 0; i < directSize; i++, offset += 2) {
         SysLib.short2Bytes(data[i], barr, offset);
      }

      SysLib.short2Bytes(indirect, barr, offset);
   
      System.arraycopy(barr, 0, iNodeData, blockNumber * 32, 32);

      SysLib.rawwrite(blockNumber, barr);
    }
 }
