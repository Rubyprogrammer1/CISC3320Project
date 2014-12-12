/* 
 * This class represents an operating system that works with a program sos, which simulates a job stream that sends jobs
 * and job requests.
 * os contains interrupt handlers and other functions to handle the life cycle of each job and to respond to job
 * requests.
 *
 */

import java.util.ArrayList;
import java.util.List;

public class os{

    /* variable declarations */
    private static jobTable JobTable;
    private static freeSpaceTable FreeSpaceTable;
    private static List<processControlBlock> readyQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> drumToMainQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> mainToDrumQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> ioQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> longTermScheduler = new ArrayList<processControlBlock>();  //NOT USED
    private static processControlBlock lastRunningJobPCB, lastJobToIo, lastJobToDrum;
    private static processControlBlock currentlyWorkingJob;

    private static int blockCount;
    private static boolean currentlyDoingIo, DoingSwap;
    
    private static final int ROUNDROBINSLICE = 9;
    private static final int BLOCKTHRESHOLD = 1;

    /*
     * startup is called only one time when the simulation begins.
     *
     * startup initializes the class variables.
     */
    public static void startup(){
        JobTable = new jobTable(50);
        FreeSpaceTable = new freeSpaceTable(100);
        
        lastRunningJobPCB = null;
        lastJobToIo = null;
        lastJobToDrum = null;
        currentlyWorkingJob = null;
                        
        currentlyDoingIo = false;
        DoingSwap = false;
        
        blockCount = 0;
        
        sos.offtrace();
    }
    
    /*
     *  Crint is called when a new job arrives on the drum.
     *  When Crint is called, it is passed information about the job in the p array.
     *
     *  In this function, the new job on the drum is:
     *      Given a PCB
     *      Placed on the job table
     *      Allocated space in memory
     *      Scheduled by the CPU
     *
     *  Crint returns void.
     */

    public static void Crint(int a[], int p[]){
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            readyQueue.add(lastRunningJobPCB);
        }
        
        currentlyWorkingJob = new processControlBlock(p[1], p[2], p[3], p[4], p[5]);
        JobTable.addJob(currentlyWorkingJob);
        MemoryManager(currentlyWorkingJob);
        
        cpuScheduler(a, p);
    }

    
    /*
     * Dskint is called after the disk finishes an IO operation (after siodisk is called).
     * 
     * The status variable currentlyDoingIo is set to false.
     * The job that just did IO is removed from the ready queue (if the job is not null).
     * 
     * If the job is in the job table:
     *  Its IO count is decremented
     *  Its latched status is undone
     *  If the job's IO count is zero:
     *      The terminated status is checked
     *      If the job is terminated,
     *          it is removed from memory and the job table
     *      If the job is not terminated, its block status is checked
     *      If the job is blocked, the job:
     *          Has its status changed to unblocked
     *          Has its block count decremented
     *          Is removed from the ready queue
     *      If the job is not terminated or blocked, it is added to the ready queue
     *
     *  The IO manager and CPU scheduler are then called.
     *
     * Dskint returns void.
     */
    public static void Dskint(int a[], int p[]){   
        currentlyDoingIo = false;
        
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            readyQueue.add(lastRunningJobPCB);
        }

        if(JobTable.contains(lastJobToIo.getJobNumber())){            
            lastJobToIo.decrementIoCount();
            lastJobToIo.unlatchJob();         
            
            if(lastJobToIo.getIoCount() == 0){
                if(lastJobToIo.getTerminatedStatus()){
                    FreeSpaceTable.addSpace(lastJobToIo);
                    JobTable.removeJob(lastJobToIo);               
                }else if(lastJobToIo.getBlockedStatus()){
                    lastJobToIo.unblockJob();
                    blockCount--;
                    readyQueue.add(lastJobToIo);                
                }else{
                    readyQueue.add(lastJobToIo);
                } 
            }
        }
        
        ioManager(); 
        cpuScheduler(a, p);
    }   

    /*
     * Drmint is called after the drum swaps job into or out of memory (after siodrum is called).
     * 
     * If the last job on the drum is NOT in core, the job is placed in core and onto 
     * the ready queue. The job is also placed on the IO queue for as many instances
     * of IO it wants to complete.
     * 
     * If the job is in core, the job is: 
     *  Removed from memory 
     *  Added to the drum to main queue
     *  Removed from the main to drum queue
     * 
     * The swapper and CPU scheduler are then called.
     *
     * Drmint returns void.
     */
    public static void Drmint(int a[], int p[]){
        DoingSwap = false;
             
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            readyQueue.add(lastRunningJobPCB);
        }
    
        currentlyWorkingJob = lastJobToDrum;
        
        if(!currentlyWorkingJob.getInCoreStatus()){
            currentlyWorkingJob.putInCore();
            readyQueue.add(currentlyWorkingJob);            
            for(int i = 0; i < currentlyWorkingJob.getIoCount(); i++)
                ioQueue.add(currentlyWorkingJob);
        }else{
            currentlyWorkingJob.removeInCore();
            FreeSpaceTable.addSpace(currentlyWorkingJob);            
            drumToMainQueue.add(currentlyWorkingJob);
            mainToDrumQueue.remove(currentlyWorkingJob);                            
        }
        
        Swapper();        
        cpuScheduler(a, p);        
    }
    
    /*
     * Tro is called when a job has run out of time (when a job finishes its time slice).
     * 
     * If the job used more time than its maximum time allowed,
     *  its IO count is checked.
     *     If its IO count is greater than zero, 
     *        the job is terminated.
     *     Otherwise, 
     *        it is removed from memory and from the job table.
     *     The swapper and IO manager are then called.
     * Otherwise, the job is added to the ready queue.
     *
     * The CPU scheduler is then called (whether or not the job used more than its maximum CPU time).

     * Tro returns void.
     */
    public static void Tro(int a[], int p[]){
        lastRunningJobPCB.calculateTimeProcessed(p[5]);

        if(lastRunningJobPCB.getCpuTimeUsed() >= lastRunningJobPCB.getMaxCpuTime()){
            if(lastRunningJobPCB.getIoCount() > 0){
                lastRunningJobPCB.terminateJob();
            }else{                                                                                                                                                
                FreeSpaceTable.addSpace(lastRunningJobPCB);
                JobTable.removeJob(lastRunningJobPCB);
            } 
            
            Swapper();            
            ioManager();        
        }else{
            readyQueue.add(currentlyWorkingJob);
        }

        cpuScheduler(a, p);
    }  

    /*
     * Svc is called when the running job wants service. A job can request service to terminate, 
     * do IO, or be blocked until all outstanding IO is completed. Svc is passed the parameter 
     * which indicates the type of service requested.
     *
     * If the job wants to terminate (a=5), we:
     *      remove the job from the ready queue
     *      remove the job from the drum queue
     *      terminate the job if its IO count is greater than zero
     *      remove the job from memory and the job table if its IO count is zero
     *  
     *
     * If the job wants to do IO (a=6), we:
     *      increment its IO counter
     *      add the job to the IO queue
     *      do IO (call ioManager())
     *
     * If the job wants to wait until all outstanding IO is completed (a=7), we:
     *      check if the IO count is not zero
     *      if it is not zero, we:
     *          remove the job from the ready queue
     *          block the job
     *          increment the block count 
     *          if the block count is greater than the block threshold and the last
     *          running job is not lathced, the job is added to the main to drum queue
     *          and the swapper is called.
     *
     * The CPU scheduler is then called.
     *
     * Svc returns void.
     */
    public static void Svc(int a[], int p[]){
        lastRunningJobPCB.calculateTimeProcessed(p[5]);
        readyQueue.add(lastRunningJobPCB);        
        
        if(a[0] == 5){
            while(readyQueue.contains(lastRunningJobPCB))
                readyQueue.remove(lastRunningJobPCB);
            
            while(mainToDrumQueue.contains(lastRunningJobPCB))
                mainToDrumQueue.remove(lastRunningJobPCB);          
            
            if(lastRunningJobPCB.getIoCount() > 0){
                lastRunningJobPCB.terminateJob();
            }else{
                lastRunningJobPCB.removeInCore();
                FreeSpaceTable.addSpace(lastRunningJobPCB);
                JobTable.removeJob(lastRunningJobPCB);                          
            }
        }
        else if(a[0] == 6){
            lastRunningJobPCB.incrementIoCount();
            ioQueue.add(lastRunningJobPCB);
            ioManager();
        }
        else if(a[0] == 7){
            if(lastRunningJobPCB.getIoCount() != 0){      
                readyQueue.remove(lastRunningJobPCB);
                lastRunningJobPCB.blockJob();

                blockCount++;                       
                if(blockCount > BLOCKTHRESHOLD && !lastRunningJobPCB.getLatchedStatus()){
                    mainToDrumQueue.add(lastRunningJobPCB);
                    Swapper();
                }         
            }
        }
        
        cpuScheduler(a, p);
    }  
    
    /*
     * ioManager is called by interrupt handlers to manage and perform IO requests.
     *
     * ioManager checks that no job is currently doing IO and that the queue is not empty.
     * If these conditions are met, the following tasks are performed on EVERY job currently
     * in the IO queue.
     *         The job is removed from the IO queue
     *         The job is latched
     *         siodisk is called on the job
     *
     * ioManager returns void.
     */
    public static void ioManager(){
        if(!currentlyDoingIo && ioQueue.size() != 0){
            for(int i = 0; i < ioQueue.size(); i++){
                currentlyWorkingJob = ioQueue.get(i);
                ioQueue.remove(i);
                
                lastJobToIo = currentlyWorkingJob;
                currentlyDoingIo = true;
                currentlyWorkingJob.latchJob();
                
                sos.siodisk(currentlyWorkingJob.getJobNumber()); 
                break;
            }                    
        }
    }
    
    /*
     *  MemoryManager is called by Crint in order to add a new job to memory.
     *
     *  MemoryManager adds the new job to drumToMain queue and then calls swapper.
     *
     *  MemoryManager returns void.
     */
    public static void MemoryManager(processControlBlock job){
        drumToMainQueue.add(job);
        Swapper();
    }        

    /*
     * Swapper is called by MemoryManager and various interrupt handlers. It moves jobs into and out of memory.
     *
     * Swapping into memory (long and short term scheduling): 
     *    Every job in drumToMainQueue is examined. The job is placed
     *    is placed into memory if it can find an adequate space in the
     *    free space table. siodrum is then called and the job is removed
     *    from drumToMainQueue.
     *
     *
     * Swapping out of memory (to make room for jobs to swap in):
     *    Every job in mainToDrumQueue is examined. The job to remove from
     *    memory is the one that is not null, not latched, and has the longest time left
     *    of all jobs on mainToDrumQueue. siodrum is called on that job. It is removed
     *    from the ready queue, the IO queue, and the main to drum queue.
     *
     * Swapper returns void.
     */

    public static void Swapper(){
        int tempAddress;
        int lowest = 0;
        int lowestIndex =0;
    
        // Swap in
        if(!DoingSwap){
            for(int i = 0; i < drumToMainQueue.size(); i++){
                currentlyWorkingJob = drumToMainQueue.get(i);
                tempAddress = FreeSpaceTable.findSpaceForJob(currentlyWorkingJob);
                if(currentlyWorkingJob.getAddress() >= 0){
                    DoingSwap = true;
                    sos.siodrum(currentlyWorkingJob.getJobNumber(), currentlyWorkingJob.getJobSize(), currentlyWorkingJob.getAddress(), 0);
                    lastJobToDrum = currentlyWorkingJob;
                    drumToMainQueue.remove(i);
                    break;
                }
            }
        }
        
        // Swap out
        if(!DoingSwap){
            for(int i = 0; i < mainToDrumQueue.size(); i++){                
                currentlyWorkingJob = mainToDrumQueue.get(i);
                
                if(currentlyWorkingJob != null && !mainToDrumQueue.get(i).getLatchedStatus() && currentlyWorkingJob.getCpuTimeLeft() > lowest){               
                    lowestIndex = i;
                    lowest = currentlyWorkingJob.getCpuTimeLeft();
                }
            }
            
            if(lowest > 0){
                DoingSwap = true;
                currentlyWorkingJob = mainToDrumQueue.get(lowestIndex);
                sos.siodrum(currentlyWorkingJob.getJobNumber(), currentlyWorkingJob.getJobSize(), currentlyWorkingJob.getAddress(), 1);
                lastJobToDrum = currentlyWorkingJob;
                
                readyQueue.remove(lastJobToDrum);                    
                ioQueue.remove(lastJobToDrum);
                mainToDrumQueue.remove(lastJobToDrum);                
            }                        
        }        
    }
        
    /*
     * cpuScheduler is called by interrupt handlers in order run the next job.
     * 
     * cpuScheduler decides which job to run next and then dispatches that job. 
     * 
     * cpuScheduler picks the job with the shortest size that is not blocked or terminated.
     * This job is dispatched and removed from the ready queue.
     *
     * If every job is blocked, null, and terminated, there are no jobs to run and the CPU is idle. 
     *
     * cpuScheduler returns void.
     */
    public static void cpuScheduler(int a[], int p[]){
        int lowest = 0;
        int lowestIndex = 0;
        
        for(int i = 0; i < readyQueue.size(); i++){
            if(lowest == 0 && !readyQueue.get(i).getBlockedStatus()){
                currentlyWorkingJob = readyQueue.get(i);
                lowest = currentlyWorkingJob.getJobSize();
                lowestIndex = i;
            }
            
            currentlyWorkingJob = readyQueue.get(i);
            
            if(currentlyWorkingJob != null && !currentlyWorkingJob.getBlockedStatus() && !currentlyWorkingJob.getTerminatedStatus() && currentlyWorkingJob.getJobSize() < lowest){              
                lowestIndex = i;
                lowest = currentlyWorkingJob.getJobSize();
            }
        }
        
        if(lowest > 0){
            currentlyWorkingJob = readyQueue.get(lowestIndex);
            dispatcher(a, p);
            while(readyQueue.contains(currentlyWorkingJob))
                readyQueue.remove(currentlyWorkingJob);
            return;                
        }

        //otherwise, there are no jobs to run and the CPU is idle
        lastRunningJobPCB = null;
        a[0] = 1;
    }    

    /*
     * dispatcher is called by cpuScheduler.
     *
     * This function dispatches the next job, setting up the a and p values.
     * If the job's time left is greater than the time slice, it is given the
     * entire time slice.
     * Otherwise, it is given the amount of time it has left to complete.
     *
     * dispatcher returns void.
     */
    public static void dispatcher(int a[], int p[]){
        lastRunningJobPCB = currentlyWorkingJob;
        lastRunningJobPCB.setLastTimeProcessing(p[5]);
        
        if(lastRunningJobPCB.getCpuTimeLeft() > ROUNDROBINSLICE){
            a[0] = 2;
            p[2] = lastRunningJobPCB.getAddress();
            p[3] = lastRunningJobPCB.getJobSize();
            p[4] = ROUNDROBINSLICE;
            
        }else{
            a[0] = 2;
            p[2] = lastRunningJobPCB.getAddress();
            p[3] = lastRunningJobPCB.getJobSize();
            p[4] = lastRunningJobPCB.getCpuTimeLeft();
        }
    }
}