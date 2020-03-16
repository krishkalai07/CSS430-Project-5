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

    /**
     * Creates an Inode by reading the head (excluding block 0) blocks from disk.
     *
     * @param iNumber The index of the iNode in disk
     */
    Inode(short iNumber) {                       // retrieving inode from disk
        int blockNumber = 1 + iNumber / 16;

        byte[] barr = new byte[512];

        if (SysLib.rawread(blockNumber, barr) == Kernel.ERROR) {
            SysLib.cerr("Failed to rawread for Inode");
            throw new IllegalArgumentException("donghee makes me sleepy");
        }
        int offset = 0;
        for (int i = 0; i < directSize; i++, offset += 2) {
            direct[i] = SysLib.bytes2short(barr, offset);
        }
        indirect = SysLib.bytes2short(barr, offset);
    }

    /**
     * 
     * @param iNumber
     * @return
     */
    int toDisk(short iNumber) {                  // save to disk as the i-th inode
        // design it by yourself.
        int blockNumber = 1 + iNumber / 16;
        int iNodeIndex = iNumber % 16;

        byte[] barr = new byte[512];
        SysLib.rawread(blockNumber, barr);

        byte[] iNodeData = new byte[iNodeSize];
        SysLib.int2bytes(length, barr, 0);
        SysLib.short2bytes(count, barr, 4);
        SysLib.short2bytes(flag, barr, 6);

        int offset = 8;
        for (int i = 0; i < directSize; i++, offset += 2) {
            SysLib.short2bytes(direct[i], barr, offset);
        }

        SysLib.short2bytes(indirect, barr, offset);
        System.arraycopy(iNodeData, 0, barr, iNodeIndex * 32, 32);
        SysLib.rawwrite(blockNumber, barr);

        return 0; //FIXME: what does this actually return?
    }
}
