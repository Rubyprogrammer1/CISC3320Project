import java.util.ArrayList;

public class os {
	static ArrayList<jobtype> jobs = new ArrayList<jobtype>();
    	static int currentTime=0;

	public static void startup(){
		sos.offtrace();
	}
	public static void Crint(int []a,int[]p){
	p[5] = currentTime;
		jobtype job = new jobtype();
		p[1] = job.JobNo();
		p[2] = job.Priority();
		p[3] = job.Size();
		p[4] = job.MaxCpuTime();
		p[5] = job.TermCpuTime();
		jobs.add(job);
	}
	public static void Svc(int []a,int[]p){
	  p[5] = currentTime;
		if(a[0]==5){
			
		}
		if(a[0]==6){
		
		}
		if(a[0]==7){
		
		}
	}
	public static void Tro(int []a,int[]p){
	//Timer-Run-Out
		p[5] = currentTime;
	}
	public static void Dskint(int []a,int[]p){
	//Disk interrupt
		p[5] = currentTime;
	}
	public static void Drmint(int []a,int[]p){
	//Drum interrupt
		p[5] = currentTime;
	}
    
}