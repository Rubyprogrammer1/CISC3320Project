import java.util.*;

public class freeSpaceTable{
    private static int worstSpaceAddress;
    private static int worstSpaceSize;
    private static int SIZE_OF_MEMORY;
    private int[] FreeSpaceTable;
    
    public freeSpaceTable(int SIZE_OF_MEMORY){
        FreeSpaceTable = new int[SIZE_OF_MEMORY];
        this.SIZE_OF_MEMORY = SIZE_OF_MEMORY;
        
        // 0 = Free Space; 1 = Space Being Used
        for(int i = 0; i < SIZE_OF_MEMORY; i++)
            FreeSpaceTable[i] = 0;
            
        worstSpaceSize = SIZE_OF_MEMORY;
        worstSpaceAddress = 0;
    }
    
    public int findSpaceForJob(processControlBlock job){
        int tempAddress;
        if(job.getJobSize() <= worstSpaceSize){
            for(int i = worstSpaceAddress; i < worstSpaceAddress + job.getJobSize(); i++)
                FreeSpaceTable[i] = 1;
            
            tempAddress = worstSpaceAddress;
            job.setAddress(tempAddress);
            
            calculateWorstSpaceAddressAndSize();

            return tempAddress;
        }else{
                    calculateWorstSpaceAddressAndSize();
            return -1;
        }
    }
    
    public void addSpace(processControlBlock job){
        for(int i = job.getAddress(); i < job.getAddress() + job.getJobSize() ; i++)
            FreeSpaceTable[i] = 0;
            
        job.setAddress(-1);    
            
        calculateWorstSpaceAddressAndSize();
    }
    
    public void calculateWorstSpaceAddressAndSize(){
        int tempAddress = 0;
        int tempSize = 0;
        
        int largestSize = 0;
        int largestSizeAddress = 0;
        
        for(int i = 0; i < SIZE_OF_MEMORY; i++){
            if(FreeSpaceTable[i] == 0){
                tempSize++;
            }else{
                if(tempSize > largestSize){
                    largestSize = tempSize;
                    largestSizeAddress = tempAddress;
                }
                tempAddress = i+1;
                tempSize = 0;
            }            
        }
        
        
        
        if(largestSize == 0 && tempSize != 0){
            largestSize = tempSize;
            largestSizeAddress = tempAddress;
        }else if(tempSize > largestSize){
            largestSize = tempSize;
            largestSizeAddress = tempAddress;        
        }
        
        worstSpaceAddress = largestSizeAddress;
        worstSpaceSize = largestSize;
    }
    
    /**
        Return the address of the worst size available
    **/
    public int getWorstSpaceAddress(){
        return worstSpaceAddress;
    }
    
    /**
        Return the size of the worst size available
    **/
    public int getWorstSpaceSize(){
        return worstSpaceSize;
    }
    
    public void printFST(){
        for(int i = 0; i < SIZE_OF_MEMORY; i++){
            if(i%6 == 0)
            System.out.println("");
            System.out.print(i + " " + FreeSpaceTable[i] + "      ");
        }
    }
}
