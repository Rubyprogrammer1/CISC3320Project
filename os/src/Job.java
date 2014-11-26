public class Job {
	private long number, priority, size, maxCPUtime, currentTime, address, timeRemaining;
	private boolean latched, blocked, terminated, running, waiting, inCore;
	
	//constructor
	public Job(int number, int priority, int size, int maxCPUtime, int currentTime) {
		this.number=number;
		this.priority=priority;
		this.size=size;
		this.maxCPUtime=maxCPUtime;
		this.currentTime=currentTime;
	}

	//accessors and mutators for the long member variables
	public long getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public long getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public long getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public long getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public long getMaxCPUtime() {
		return maxCPUtime;
	}
	public void setMaxCPUtime(int maxCPUtime) {
		this.maxCPUtime = maxCPUtime;
	}
	public long getCurrentTime() {
		return currentTime;
	}
	public void setCurrentTime(int currentTime) {
		this.currentTime = currentTime;
	}

	public long getTimeRemaining() {
		return timeRemaining;
	}
	public void setTimeRemaining(int timeRemaining) {
		this.timeRemaining = timeRemaining;
	}
	
	//accessors and mutators for the boolean member variables
	public boolean isTerminated() {
		return terminated;
	}
	public void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}
	
	public boolean isWaiting() {
		return waiting;
	}
	public void setWaiting(boolean waiting) {
		this.waiting = waiting;
	}
	
	public boolean isBlocked() {
		return blocked;
	}
	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

	//a job which is currently doing IO cannot be swapped out - a latch bit is used
	//to indicate that a job is latched
	public boolean isLatched() {
		return latched;
	}
	
	public void setLatched(boolean latched) {
		this.latched = latched;
	}
	
	public boolean isInCore() {
		return inCore;
	}
	
	public void setInCore(boolean inCore) {
		this.inCore = inCore;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}
}