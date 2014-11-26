import java.util.LinkedList;
import java.util.ListIterator;

public class FreeSpaceTable {
	
	 public  LinkedList<int[]> FST;
	 public int address;
	 public int[] freeSpace, entry, nextEntry;
	 
	 //constructor
	 public FreeSpaceTable() {
		 
		 FST= new LinkedList<int[]>();
		 int []firstEntry =  {100, 0}; //[0]=size, [1]=address
		 FST.add(firstEntry);
	 
	 }

	 //locate a free space according to the best fit strategy
	 //if there is no free space >= to the job size, return -1
	 public int findSpace(int jobSize){
		 
		 ListIterator<int[]> itr = FST.listIterator();
		 int index=0;
		 
		 while(itr.hasNext()){
			 
		     freeSpace = itr.next();
		     if(freeSpace[0] >= jobSize) {
		    	 
		    	 address = freeSpace[1];
		    	 
		    	 freeSpace[0] -= jobSize;
		    	 freeSpace[1] += jobSize;
		    
		    	 FST.set(index, freeSpace); //update the free space table
		    	 
		    	 return address;
		     }
		     
		     index++;
		     
		 }

		 return -1;
	 }
	 
	 //check if a free space at a given index is adjacent to another free space in memory
	 //if it is, join the two free spaces and update FST
	 public void combineAdjacentFragments (int index){

		 if(index!=FST.size()-1){
			 
		 	entry=FST.get(index);
		 	nextEntry=FST.get(index+1);
		 	
			 if (entry[0]+entry[1]==nextEntry[1]-1){
				 
				 FST.remove(index+1);   	//delete next entry
				 entry[0]+=nextEntry[0];	//update entry's size
				 
				 FST.set(index, entry);		//set replaces the entry at index
			 }	
		 } 
	 }

	 //add a free space to the table in its appropriate spot and update
	 //the table accordingly
	 public void addFreeSpace(int size, int location){
		 
		 ListIterator<int[]> itr = FST.listIterator();
		 int index=0;
		 int[] newSpace={size, location};
		 
		 while(itr.hasNext()){
			   
		     entry = itr.next();
		     
		     if (entry[1]==location){		//if the entry's location matches to ours
		    	 FST.add(index, newSpace);	//add does NOT replace the value at index
		    	 combineAdjacentFragments(index);
		     }
		     
		     index++;
		 }
	 }
	 
	 //remove a free space from the table - locate the job's free space according
	 //to its location (this will happen when a  job terminates and its resources
	 //must be freed up)
	 public void removeFreeSpace(int location){
		 
		 ListIterator<int[]> itr = FST.listIterator();
		 int index=0;
		 
		 while(itr.hasNext()){
			   
		     entry = itr.next();
		     
		     if (entry[1]==location){//if the entry's location matches to ours
		    	 FST.remove(index);
		     }
		 
		     index++;
		 }
		 return;
	 }
}