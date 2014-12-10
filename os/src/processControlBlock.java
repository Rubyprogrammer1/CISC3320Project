public class processControlBlock{
    
    private int jobNumber;
    private int jobSize;
    private int timeArrived;
    private int lastTimeProcessing;
    private int cpuTimeUsed;
    private int maxCpuTime;
    private int ioPending;
    private int priority;
    private int address;
    private boolean blocked;
    private boolean latched;
    private boolean inCore;
    private boolean terminated;
    
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
        address = -1;
    }
    
    public int getJobNumber(){
        return jobNumber;
    }
    
    public int getJobSize(){
        return jobSize;
    }
    
    public void setAddress(int address){
        this.address = address;
    }
    
    public int getAddress(){
        return address;
    }
    
    public void setLastTimeProcessing(int lastTimeProcessing){
        this.lastTimeProcessing = lastTimeProcessing;
    }
    
    public int getMaxCpuTime(){
        return maxCpuTime;
    }
    
    public int getCpuTimeUsed(){
        return cpuTimeUsed;
    }
    
    public int getCpuTimeLeft(){
        return maxCpuTime - cpuTimeUsed;
    }
    
    public void incrementIoCount(){
        ioPending++;
    }
    
    public void decrementIoCount(){
        ioPending--;
    }
    
    public int getIoCount(){
        return ioPending;
    }
    
    public void blockJob(){
        blocked = true;
    }
    
    public void unblockJob(){
        blocked = false;
    }
    
    public boolean getBlockedStatus(){
        return blocked;
    }
    
    public void latchJob(){
        latched = true;
    }
    
    public void unlatchJob(){
        latched = false;
    }
    
    public boolean getLatchedStatus(){
        return latched;
    }
    
    public void putInCore(){
        inCore = true;
    }
    
    public void removeInCore(){
        inCore = false;
    }
    
    public boolean getInCoreStatus(){
        return inCore==true;
    }
    
    public void terminateJob(){
        terminated = true;
    }
    
    public boolean getTerminatedStatus(){
        return terminated;
    }
    
    public int getPriority(){
        return priority;
    }
    
    
    public void calculateTimeProcessed(int currentTime){
        cpuTimeUsed = cpuTimeUsed + currentTime - lastTimeProcessing;
    }
    
    public void printProcess(){
        System.out.println(jobNumber + "      " + jobSize + "      " + timeArrived + "      " + cpuTimeUsed + "      " + maxCpuTime + "      " + ioPending + "      " + priority + "      " + blocked  + "      " + latched + "      " + inCore + "      " + terminated + "      " + address);    
    }

}
