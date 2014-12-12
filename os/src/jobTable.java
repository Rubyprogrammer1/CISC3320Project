/*
 * This class represents a job table of process control blocks. It contains a tree map data structure, jobTable,
 * of processControlBlock objects, along with functions necessary to work with jobTable. os.java works
 * with jobTable in order to keep track of jobs entering/leaving the system.
 */

import java.util.TreeMap;
import java.util.*;

public class jobTable{
    private static int MAX_SIZE;
    private static int jobsInTable;
    private static TreeMap JobTable;

    /*
     * jobTable constructor
     */
    public jobTable(int MAX_SIZE){
        this.MAX_SIZE = MAX_SIZE; //sets the maximum size of the job table to the parameter passed
        jobsInTable = 0;
        JobTable = new TreeMap(); //creates the jobTable data structure
    }
    
    /*
     * Adds a processControlBlock object to the job table if capacity allows, incrementing the number of jobs in the table
     * Returns void
     */
    public void addJob(processControlBlock job){
        if(jobsInTable < MAX_SIZE){
            JobTable.put(job.getJobNumber(), job);
            jobsInTable++;
        }
        else
            System.out.println("JOB TABLE FULL");
    }
    
    /*
     * Removes a processControlBlock object from the table, decrements the number of jobs in the table, and returns void
     */
    public void removeJob(processControlBlock job){
        if(jobsInTable > 0){
            JobTable.remove(job.getJobNumber());
            jobsInTable--;
        }
    }
    
    /*
     * Returns a processControlBlock object from the job table given its job number
     */
    public processControlBlock getJob(int jobNumber){
		return (processControlBlock) JobTable.get(jobNumber);
	}
	
    /*
     * Returns a boolean value indicating whether the job table contains a job with the passed job number
     */
	public boolean contains(int jobNumber){
        return JobTable.containsKey(jobNumber);
	}
     
    /*
     * Prints the job table and returns void
     */      
    public static void printJobTable(){
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
