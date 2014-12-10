import java.util.TreeMap;
import java.util.*;

public class jobTable{
    private static int MAX_SIZE;
    private static int jobsInTable;
    private static TreeMap JobTable;

    public jobTable(int MAX_SIZE){
        this.MAX_SIZE = MAX_SIZE;
        jobsInTable = 0;
        JobTable = new TreeMap();
    }
    
    public void addJob(processControlBlock job){
        if(jobsInTable < MAX_SIZE){
            JobTable.put(job.getJobNumber(), job);
            jobsInTable++;
        }
        else
            System.out.println("JOB TABLE FULL");
    }
    
    public void removeJob(processControlBlock job){
        if(jobsInTable > 0){
            JobTable.remove(job.getJobNumber());
            jobsInTable--;
        }
    }
    
    public processControlBlock getJob(int jobNumber){
		return (processControlBlock) JobTable.get(jobNumber);
	}
	
	public boolean contains(int jobNumber){
        return JobTable.containsKey(jobNumber);
	}
           
    public void printJobTable(){
        processControlBlock temp;
    
        Set set = JobTable.entrySet();
        
        Iterator i = set.iterator();
        
        
        System.out.println("#    " + "Size" + " Arrived " + "           CPU TU " + " MAX CPUT " + " IOP" + " Priority " + " Blocked " + " Latched " + " INCORE " + " Terminated " + " Address");
        while(i.hasNext()){
            Map.Entry me = (Map.Entry)i.next();
            temp = (processControlBlock) me.getValue();
            temp.printProcess();
        }
    } 
    
}
