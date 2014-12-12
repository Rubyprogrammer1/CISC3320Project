/*
 * This class represents a process control block for a job. processControlBlock has member variables
 * for job properties that we want to keep track of. A new instance of this class is created in os.java 
 * for every new job that enters the system.
 */

public class processControlBlock{
    
    /* variable declarations */
    private int jobNumber;
    private int jobSize;
    private int timeArrived;
    private int lastTimeProcessing;
    private int cpuTimeUsed;
    private int maxCpuTime;
    private int ioPending;
    private int priority;
    private int address;
    private int timesBlocked;
    private boolean blocked;
    private boolean latched;
    private boolean inCore;
    private boolean terminated;
    
    /*
     * Constructor that sets the appropriate member variables with information about the job
     */
    public processControlBlock(int jobNumber, int priority, int jobSize, int maxCpuTime, int timeArrived){
        this.jobNumber = jobNumber;
        this.priority = priority;
        this.jobSize = jobSize;
        this.maxCpuTime = maxCpuTime;
        this.timeArrived = timeArrived;
        cpuTimeUsed = 0;
        ioPending = 0;
        blocked = false;
        latched = false;
        inCore = false;
        terminated = false;
        timesBlocked = 0;
        address = -1;
    }    
    
    /*
     * Accessor methods
     */
     
    public int getTimesBlocked(){
        return timesBlocked;
    }    
    public int getTimeArrived(){
        return timeArrived;
    }
    public int getJobNumber(){
        return jobNumber;
    }    
    public int getJobSize(){
        return jobSize;
    }
    public int getAddress(){
        return address;
    }
    public int getMaxCpuTime(){
        return maxCpuTime;
    }
    public int getIoCount(){
        return ioPending;
    }
    public int getCpuTimeUsed(){
        return cpuTimeUsed;
    }
    public int getCpuTimeLeft(){
        return maxCpuTime - cpuTimeUsed;
    }    
    public int getPriority(){
        return priority;
    }    
    public boolean getBlockedStatus(){
        return blocked;
    }
    public boolean getInCoreStatus(){
        return inCore==true;
    }    
    public boolean getTerminatedStatus(){
        return terminated;
    }
    public boolean getLatchedStatus(){
        return latched;
    }        
            
    /*
     * Mutator methods
     */            
                   
    public void setAddress(int address){
        this.address = address;
    }
    
    public void setLastTimeProcessing(int lastTimeProcessing){
        this.lastTimeProcessing = lastTimeProcessing;
    }
    
    public void calculateTimeProcessed(int currentTime){
        cpuTimeUsed = cpuTimeUsed + currentTime - lastTimeProcessing;
    }
    
    public void incTimesBlocked(){
        timesBlocked++;
    }    

    public void incrementIoCount(){
        ioPending++;
    }
    
    public void decrementIoCount(){
        ioPending--;
    }

    public void blockJob(){
        blocked = true;
    }
    
    public void unblockJob(){
        blocked = false;
    }

    public void latchJob(){
        latched = true;
    }
    
    public void unlatchJob(){
        latched = false;
    }
    
    public void putInCore(){
        inCore = true;
    }
    
    public void removeInCore(){
        inCore = false;
    }

    public void terminateJob(){
        terminated = true;
    }
    
    /* Prints the process control block */
    public void printProcess(){
        System.out.println(jobNumber + "      " + jobSize + "      " + timeArrived + "      " + cpuTimeUsed + "      " + maxCpuTime + "      " + ioPending + "      " + priority + "      " + blocked  + "      " + latched + "      " + inCore + "      " + terminated + "      " + address + "      " + timesBlocked);    
    }

}
