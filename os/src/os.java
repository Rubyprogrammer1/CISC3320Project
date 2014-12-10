import java.util.ArrayList;
import java.util.List;

public class os{
    private static jobTable JobTable;
    private static freeSpaceTable FreeSpaceTable;
    private static List<processControlBlock> readyQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> drumToMainQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> mainToDrumQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> ioQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> longTermScheduler = new ArrayList<processControlBlock>();
    private static processControlBlock lastRunningJobPCB;
    private static processControlBlock lastJobToIo;
    private static processControlBlock lastJobToDrum;
    private static processControlBlock currentlyWorkingJob;
    private static int roundRobinSlice;
    private static int blockCount;
    private static boolean currentlyDoingIo, DoingSwap;


    /*
        INITIALIZE VARIABLES, TABLES, AND ROUND ROBIN SLICE
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
        
        roundRobinSlice = 2;
        blockCount = 0;
        
        sos.offtrace();
    }
    
    public static void Crint(int a[], int p[]){
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            readyQueue.add(lastRunningJobPCB);
        }
        
        currentlyWorkingJob = new processControlBlock(p[1], p[2], p[3], p[4], p[5]);
        JobTable.addJob(currentlyWorkingJob);
        MemoryManager(currentlyWorkingJob, 0);
        
        // Only one chance a job goes on the ready queue from this function
        cpuScheduler(a, p);
    }
    
    public static void Svc(int a[], int p[]){
        lastRunningJobPCB.calculateTimeProcessed(p[5]);
        
        
        if(a[0] == 5){
            if(lastRunningJobPCB.getIoCount() > 0){
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
            
                lastRunningJobPCB.terminateJob();
            }else{
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
                while(ioQueue.contains(lastRunningJobPCB))
                    ioQueue.remove(lastRunningJobPCB);                            
                while(drumToMainQueue.contains(lastRunningJobPCB))
                    drumToMainQueue.remove(lastRunningJobPCB);
                while(mainToDrumQueue.contains(lastRunningJobPCB))
                    mainToDrumQueue.remove(lastRunningJobPCB);                    
                                                                                                                                                                  
                lastRunningJobPCB.removeInCore();
                FreeSpaceTable.addSpace(lastRunningJobPCB);
                JobTable.removeJob(lastRunningJobPCB);
                
                lastRunningJobPCB = null;                           
            }
        }
        else if(a[0] == 6){
            readyQueue.add(lastRunningJobPCB);
            lastRunningJobPCB.incrementIoCount();
            ioQueue.add(lastRunningJobPCB);
            ioManager();
        }
        else if(a[0] == 7){
            readyQueue.add(lastRunningJobPCB);
            if(lastRunningJobPCB.getIoCount() != 0){
                lastRunningJobPCB.printProcess();       
                lastRunningJobPCB.blockJob();
                blockCount++;       
                
                if(blockCount > 1 && lastRunningJobPCB.getCpuTimeLeft() > 2010){// || lastRunningJobPCB.getJobSize() > 40 || lastRunningJobPCB.getCpuTimeLeft() > 30000){
                    //if(ioQueue.size() > 1){
                        if(/*lastRunningJobPCB != ioQueue.get(0) &&*/ !lastRunningJobPCB.getLatchedStatus()){
                            mainToDrumQueue.add(lastRunningJobPCB);

                            Swapper();
                        }
                    //}
                }         
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);    
                if(readyQueue.size() > 0 )
                    readyQueue.get(0).printProcess();  
                       
            }
        }
        
        cpuScheduler(a, p);
    }
    
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
    
    public static void Drmint(int a[], int p[]){
        DoingSwap = false;
             
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            readyQueue.add(lastRunningJobPCB);
        }
    
        /* Set the currently working job equal to the first job off the job queue and remove it from the queue
        */
        currentlyWorkingJob = lastJobToDrum;
        
        if(!currentlyWorkingJob.getInCoreStatus()){
            currentlyWorkingJob.putInCore();
            if(currentlyWorkingJob.getIoCount() > 0)
                ioQueue.add(currentlyWorkingJob);
            readyQueue.add(currentlyWorkingJob);
            while(drumToMainQueue.contains(currentlyWorkingJob))
                drumToMainQueue.remove(currentlyWorkingJob);             
        }else{
            currentlyWorkingJob.removeInCore();
            FreeSpaceTable.addSpace(currentlyWorkingJob);
            drumToMainQueue.add(currentlyWorkingJob);
            while(readyQueue.contains(currentlyWorkingJob))
                readyQueue.remove(currentlyWorkingJob);
            while(mainToDrumQueue.contains(currentlyWorkingJob))
                mainToDrumQueue.remove(currentlyWorkingJob);                            
        }
        
        Swapper();        
        cpuScheduler(a, p);        
    }
    
    public static void Tro(int a[], int p[]){
        lastRunningJobPCB.calculateTimeProcessed(p[5]);

        /*  If the job exceeds its maximum time used:
          * - Check if there is IO left
            * - If IO left, remove from ready queue, check terminate bit
          * - If no IO left, remove from ready queue, IO queue, drum queue
            * - Set in core bit to 0, add space from job back to FST, remove job from jobTable
            * - Set last running job = null
            * - Run the MemoryManager function to attempt to put jobs from the long term scheduler into memory
        */
        if(lastRunningJobPCB.getCpuTimeUsed() >= lastRunningJobPCB.getMaxCpuTime()){
            if(lastRunningJobPCB.getIoCount() > 0){
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
            
                lastRunningJobPCB.terminateJob();
            }else{
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
                while(ioQueue.contains(lastRunningJobPCB))
                    ioQueue.remove(lastRunningJobPCB);  
                while(drumToMainQueue.contains(lastRunningJobPCB))
                    drumToMainQueue.remove(lastRunningJobPCB);       
                while(mainToDrumQueue.contains(lastRunningJobPCB))
                    mainToDrumQueue.remove(lastRunningJobPCB);                                        
                                                                                                                                                                  
                FreeSpaceTable.addSpace(lastRunningJobPCB);
                JobTable.removeJob(lastRunningJobPCB);
                
                lastRunningJobPCB = null;                           
            }        
        }
        /* If the job exceeds its time slice, place back on the ready drive
        */
        else{
            readyQueue.add(currentlyWorkingJob);
        }

        Swapper();
        cpuScheduler(a, p);
    }    
    
    public static void ioManager(){
        if(!currentlyDoingIo){
            if(ioQueue.size() != 0){

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
    }
    
    public static void MemoryManager(processControlBlock job, int function){
        
        
        if(function == 0){ // SEE IF THE JOB FITS IN FREE SPACE TALBLE, IF IT DOES, PLACE IN MEMORY
            drumToMainQueue.add(job);
        }
        
        Swapper();
    }        

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
                    while(drumToMainQueue.contains(lastJobToDrum))
                        drumToMainQueue.remove(i);
                    break;
                }
            }
        }
        
        // Swap out
        if(!DoingSwap){
            for(int i = 0; i < mainToDrumQueue.size(); i++){
                if(lowest == 0 && !mainToDrumQueue.get(i).getLatchedStatus()){
                    currentlyWorkingJob = mainToDrumQueue.get(i);
                    lowest = currentlyWorkingJob.getJobSize();
                    lowestIndex = i;
                }
                
                currentlyWorkingJob = mainToDrumQueue.get(i);
                
                if(currentlyWorkingJob != null && !mainToDrumQueue.get(i).getLatchedStatus()){               
                    if(currentlyWorkingJob.getJobSize() > lowest){
                        lowestIndex = i;
                        lowest = currentlyWorkingJob.getJobSize();
                    }
                }
            }
            
            if(lowest > 0){
                DoingSwap = true;
                //FreeSpaceTable.addSpace(currentlyWorkingJob);
                currentlyWorkingJob = mainToDrumQueue.get(lowestIndex);
                sos.siodrum(currentlyWorkingJob.getJobNumber(), currentlyWorkingJob.getJobSize(), currentlyWorkingJob.getAddress(), 1);
                lastJobToDrum = currentlyWorkingJob;
                while(readyQueue.contains(lastJobToDrum))
                    readyQueue.remove(lastJobToDrum);                    
                while(ioQueue.contains(lastJobToDrum))
                    ioQueue.remove(lastJobToDrum);
                mainToDrumQueue.remove(lastJobToDrum);                
            }                        
        }        
    }
    
    public static void dispatcher(int a[], int p[]){
        lastRunningJobPCB = currentlyWorkingJob;
        lastRunningJobPCB.setLastTimeProcessing(p[5]);
        
        if(lastRunningJobPCB.getCpuTimeLeft() > roundRobinSlice){
			a[0] = 2; // Set system to process a job
			p[2] = lastRunningJobPCB.getAddress();
			p[3] = lastRunningJobPCB.getJobSize();
			p[4] = roundRobinSlice;
			
		}else{
			a[0] = 2; // Set system to process a job
			p[2] = lastRunningJobPCB.getAddress();
			p[3] = lastRunningJobPCB.getJobSize();
			p[4] = lastRunningJobPCB.getCpuTimeLeft();
		}
    }
    
    public static void cpuScheduler(int a[], int p[]){
        boolean possible;
        boolean falseStuff = true;
        int lowest = 0;
        int lowestIndex = 0;
        
        for(int i = 0; i < readyQueue.size(); i++){
            if(lowest == 0 && !readyQueue.get(i).getBlockedStatus()){
                currentlyWorkingJob = readyQueue.get(i);
                lowest = currentlyWorkingJob.getCpuTimeLeft();
                lowestIndex = i;
            }
            
            currentlyWorkingJob = readyQueue.get(i);
            
            if(currentlyWorkingJob != null){
                if(currentlyWorkingJob.getBlockedStatus() == false ){                
                    if(currentlyWorkingJob.getTerminatedStatus() == false){
                        if(currentlyWorkingJob.getCpuTimeLeft() < lowest){
                            lowestIndex = i;
                            lowest = currentlyWorkingJob.getCpuTimeLeft();
                        }
                    }
                }
            }
        }
        
        if(lowest > 0){
            currentlyWorkingJob = readyQueue.get(lowestIndex);
            dispatcher(a, p);
            while(readyQueue.contains(currentlyWorkingJob))
                readyQueue.remove(currentlyWorkingJob);
            falseStuff = true;
            return;                
        }
        
        if(falseStuff == true){
            currentlyWorkingJob = null;
            lastRunningJobPCB = null;
            a[0] = 1;
        }
    }    
}
