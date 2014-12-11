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
    private static int blockCount;
    private static boolean currentlyDoingIo, DoingSwap;
    
    private static final int ROUNDROBINSLICE = 9;
    private static final int BLOCKTHRESHOLD = 1;


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
    
    public static void MemoryManager(processControlBlock job){
        drumToMainQueue.add(job);
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

        lastRunningJobPCB = null;
        a[0] = 1;
    }    
}
