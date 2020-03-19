
public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    
    /**
     * 0: unused
     * 1: used
     * 2: marked for deletion
     */
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    Inode( ) {                                     // a default constructor
        length = 0;
        count = 0;
        flag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    Inode( short iNumber ) {
        int blockNumber = iNumber / 16 + 1;
        int offset = iNumber * iNodeSize % 512;
        // System.out.println("Inode.Inode():data = " + iNumber + " " + blockNumber + " " + offset);

        byte [] temp = new byte[512];
        SysLib.rawread(blockNumber,temp);

        length = SysLib.bytes2int(temp, offset);
        count = SysLib.bytes2short(temp,offset + 4);
        flag = SysLib.bytes2short(temp,offset + 6);

        offset = offset + 8;

        for(int i = 0; i < directSize; i++){
            direct[i] = SysLib.bytes2short(temp,offset);
            offset += 2;
        }
        indirect = SysLib.bytes2short(temp,offset);
    }

    int toDisk( short iNumber ) {                  // save to disk as the i-th inode
        // design it by yourself.
        byte [] temp = new byte[512];

        int blockNumber = iNumber / 16 + 1;
        int offset = iNumber * 32 % 512;

        SysLib.rawread(blockNumber, temp);

        SysLib.int2bytes(length,temp,offset);
        SysLib.short2bytes(count,temp,offset + 4);
        SysLib.short2bytes(flag,temp, offset + 6);

        offset = offset + 8;

        for(int i = 0; i < directSize; i++){
            SysLib.short2bytes(direct[i], temp, offset);
            offset += 2;
        }

        SysLib.short2bytes(indirect,temp,offset);
        SysLib.rawwrite(blockNumber,temp);
        return iNumber;
    }
}