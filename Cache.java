import java.util.*;

public class Cache {

    //Entry class has four members
    private class Entry {

        byte page[] = new byte[blockSize]; //the actual data
        int isDirty = 0; //if it has been written to
        int isValid = 0; //if it has been read/accessed
        int blockId = -1; //intial block id is set to -1 aka invalid

    }

    private Entry[] pageTable; //page table keeps track of all the info
    int blockSize; //block size
    int cacheBlocks; //cache block

    //Constructor to initialize cache
    public Cache(int blockSize, int cacheBlocks) {
        //initialize entry page table
        this.blockSize = blockSize;
        this.cacheBlocks = cacheBlocks;
        pageTable = new Entry[cacheBlocks];
        //set each element in page table to an entry instead of null
        for(int i = 0; i < pageTable.length;i++){
            pageTable[i] = new Entry();
        }
    }

    //find free page
    private int findFreePage() {
        //sequentially iterate and find the next unloaded page
        for(int i = 0; i < pageTable.length; i++){
            if(pageTable[i].blockId == -1){
                return i;
            }
        }
        return -1;
    }

    //enhanced second chance alorgitm for finding a victim page to replace
    private int nextVictim() {
        int i = 0;
        int current = -1;
        while(true) {
//            System.out.println(i);
            if (pageTable[i].isValid == 0 && pageTable[i].isDirty == 0){
               return i;
            }else if(pageTable[i].isValid == 0 && pageTable[i].isDirty == 1){
                if(current == -1) {
                    current = i;
                }
            }else{
                pageTable[i].isValid = 0;
            }

            if(i == this.pageTable.length-1){
               if(current != -1) {
                   return current;
               }else{
                   i = 0;
               }
            }
            i++;
        }
    }

    //read method
    public synchronized boolean read(int blockId, byte buffer[]) {
        if(blockId < 0){
            return false;
        }
        int i;
        for(i = 0; i < pageTable.length; i++){
            if(pageTable[i].blockId == blockId){
                //found match (in memory)
                //read
                System.arraycopy(pageTable[i].page, 0, buffer, 0, blockSize);
                pageTable[i].isValid = 1;
                return true;
            }
        }
        //didnt find match (not in memory)

        int nextFreePage = findFreePage();
        if(nextFreePage == -1){
            nextFreePage = nextVictim();
        }

        //replace in memory
        if(pageTable[nextFreePage].isDirty == 1){
            writeBack(nextFreePage);
        }
        SysLib.rawread(blockId,buffer);
        //read
        pageTable[nextFreePage].blockId = blockId;
        System.arraycopy(buffer, 0, pageTable[nextFreePage].page, 0, blockSize);
        //change bit
        pageTable[nextFreePage].isValid = 1;

        return true;
    }

    //write method
    public synchronized boolean write(int blockId, byte buffer[]) {
        if(blockId < 0){
            return false;
        }
        int i;
        for(i=0;i<pageTable.length;i++){
            if(pageTable[i].blockId == blockId){
                //write
                System.arraycopy(buffer, 0, pageTable[i].page, 0, buffer.length);
                pageTable[i].isValid = 1;
                pageTable[i].isDirty = 1;

                return true;
            }
        }
        //not in mem
        //option(open spots) 1: find free cache block and write to it
        //option(no open spots) 2: find victim and replace

        //replace in memory
        //write
        //change bit
        int nextFreePage = findFreePage();
        if(nextFreePage == -1){
            nextFreePage = nextVictim();
        }

        //replace in memory
        if(pageTable[nextFreePage].isDirty == 1){
            writeBack(nextFreePage);
        }
        //SysLib.rawread(blockId,buffer);
        //write
        pageTable[nextFreePage].blockId=blockId;
        System.arraycopy(buffer, 0, pageTable[nextFreePage].page, 0, buffer.length);
        //change bit
        pageTable[nextFreePage].isDirty = 1;
        pageTable[nextFreePage].isValid = 1;

        return true;
    }

    //sync updates the disk to match the data in the cache
    public synchronized void sync() {
        for(int i = 0; i < pageTable.length;i++) {
            if (pageTable[i].isDirty == 1) {
                writeBack(i);
                pageTable[i].isDirty = 0;
            }
        }
    }

    //syncs data in the cache with disk, then invalidates in cache blocks
    public synchronized void flush(){
        for(int i = 0; i < pageTable.length;i++){
            if(pageTable[i].isDirty == 1){
                writeBack(i);
                pageTable[i].isDirty = 0;
                pageTable[i].isValid = 0;
                pageTable[i].blockId = -1;
            }
        }
    }

    //sends the given block back to the disk
    private void writeBack(int victimEntry) {
        pageTable[victimEntry].isDirty = 0;
        int bId = pageTable[victimEntry].blockId;
        byte[] buff = pageTable[victimEntry].page;
        //add to disk
        SysLib.rawwrite(bId,buff);
    }
}
