/*
 * This class represents a free space table and all of the functions necessary to work with it.
 *
 * The free space table is represented as an integer array data structure that is the size of memory. The array 
 * element at i indicates whether or not memory location i is free. 
 *      If the element is zero, memory location i is free. 
 *      If the element is one, the memory location i is being used.
 *
 *  The worst fit strategy is used to allocate free space.
 *
 */
import java.util.*;

public class freeSpaceTable{

    /* variable declarations */
    private static int worstSpaceAddress;
    private static int worstSpaceSize;
    private static int SIZE_OF_MEMORY;
    private int[] FreeSpaceTable;
    
    /* freeSpaceTable constructor */
    public freeSpaceTable(int SIZE_OF_MEMORY){
        FreeSpaceTable = new int[SIZE_OF_MEMORY]; //array is declared according to the memory size passed
        this.SIZE_OF_MEMORY = SIZE_OF_MEMORY;
        
        /*
         * The elements of freeSpaceTable are initilalized to zero, indicating
         * that all space is free
         */
        for(int i = 0; i < SIZE_OF_MEMORY; i++) 
            FreeSpaceTable[i] = 0;
            
        worstSpaceSize = SIZE_OF_MEMORY;
        worstSpaceAddress = 0;
    }
    
    /*
     * This function tries to find space for the passed processControlBlock object.
     * If space is found:
     *      The space allocated in memory is set to 1 in the freeSpaceTable array
     *      The starting address of the space is returned
     * If space is not found, the function returns -1
     *
     */
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
    
    /*
     * This function adds space to freeSpaceTable according to the size and address of the passed processControlBlock.  
     * The address and size of the largest space is re-calculated and updated. 
     * addSpace returns void.
     *
     */
    public void addSpace(processControlBlock job){
        for(int i = job.getAddress(); i < job.getAddress() + job.getJobSize() ; i++)
            FreeSpaceTable[i] = 0;
            
        job.setAddress(-1);    
            
        calculateWorstSpaceAddressAndSize();
    }
    
    /*
     * Calculates the largest free space and returns void
     *
     */
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
    
    /*
        Returns the address of the largest (worst) size available
    */
    public int getWorstSpaceAddress(){
        return worstSpaceAddress;
    }
    
    /*
        Returns the size of the worst (largest) size available
    */
    public int getWorstSpaceSize(){
        return worstSpaceSize;
    }
    
    /*
        Prints the free space table and returns void
    */
    public void printFST(){
        for(int i = 0; i < SIZE_OF_MEMORY; i++){
            if(i%6 == 0)
            System.out.println("");
            System.out.print(i + " " + FreeSpaceTable[i] + "      ");
        }
    }
}
