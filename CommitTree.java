import java.util.Date;
import java.io.Serializable;
import java.util.HashSet;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Set;

public class CommitTree implements Serializable {
	private Commit activePointer;
	private int size = -1;
	public boolean justRemoved = false;
	public HashMap<String, Commit> branches;
	public HashMap<Integer, Commit> idToCommit;
	public HashMap<String, TreeSet<Integer>> messageToId;
	public String activeBranch;
	
    public CommitTree(Date createTime) {
    	if (this.size == -1) {
    		branches = new HashMap<String, Commit>();
    		idToCommit = new HashMap<Integer, Commit>();
    		messageToId = new HashMap<String, TreeSet<Integer>>();
            HashSet<String> empty = new HashSet();
    		activeBranch = "master";
    		Commit first = createNode(null, 0, "initial commit", createTime, empty);
    	}
    }

    public Commit createNode(Commit previousC, int id, String info, Date time, HashSet<String> newFiles) {
    	Commit node = new Commit(previousC, id, info, time, newFiles);
    	addNode(node);
    	this.size += 1;
    	return node;
    }

    public void addNode(Commit n) {
    	this.activePointer = n;
    	branches.put(activeBranch, n);
    	idToCommit.put(n.commitId, n);
    	// Adding new ids to the messageToId map
    	TreeSet<Integer> oldIds = messageToId.get(n.getMessage());
    	TreeSet<Integer> newIds = new TreeSet<Integer>();
    	newIds.add(n.commitId);
        if (oldIds != null) {
            newIds.addAll(oldIds);
        }
    	messageToId.put(n.getMessage(), newIds);
    }

    public void branchAdd(String bname) {
    	// Creating  a new branch in the map.
    	this.branches.put(bname, this.activePointer);
    }

    public void changeActivePointer(String bname) {
    	this.activePointer = this.branches.get(bname);
    	this.activeBranch = bname;
    }

    public boolean lastCommitContainsFile(String filename) {
    	Set<String> filesInLastCommit = (Set<String>) this.activePointer.filesToPath.keySet();
    	if (filesInLastCommit != null && filesInLastCommit.contains(filename)) {
    		return true;
    	}
    	return false;
    }

    public int lastCommitId() {
    	return this.size;
    }

    public Commit lastCommit() {
    	return this.activePointer;
    }

    public void printLog() {
    	Commit ptr = activePointer;
    	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	String formattedDate;
    	while (ptr != null) {
			System.out.println("====");	
			System.out.println("Commit " + ptr.commitId + ".");
    		formattedDate = df.format(ptr.timeStamp);
    		System.out.println(formattedDate);
    		System.out.println(ptr.cMessage);
            System.out.println();
    		ptr = ptr.prev;
    	}
    }

    public void removeFuture(String file, String action) {
	    Commit currentNode = this.activePointer;
	    boolean condition = currentNode != null && currentNode.filesToPath.containsKey(file);
    	if (action == "remove" && condition) {
	    	currentNode.fInheritanceCheck.put(file, false);
            this.justRemoved = true;
    	} else if (action == "revert" && condition) {
	    	currentNode.fInheritanceCheck.put(file, true);
    	}
    }

    public Commit findSplitNode(Commit first, Commit second) {
    	Commit ptrOne = first, ptrSecond = second;
    	TreeSet<Integer> firstIds = new TreeSet<Integer>();
    	TreeSet<Integer> secondIds = new TreeSet<Integer>();

    	while (ptrOne != null) {
    		firstIds.add(ptrOne.commitId);
    		ptrOne = ptrOne.prev;
    	}
    	while (ptrSecond != null) {
    		secondIds.add(ptrSecond.commitId);
    		ptrSecond = ptrSecond.prev;
    	}
		TreeSet<Integer> intersection = new TreeSet<Integer>();
		for (Integer ctr : firstIds) {
		  if(secondIds.contains(ctr)) {
		  	intersection.add(ctr);
		  }
		}
		return idToCommit.get(intersection.last());
    }

	public class Commit implements Serializable{
		private Commit prev;
		public int commitId;
		private String cMessage;
		public Date timeStamp;	
		private HashSet<String> addedFiles;
		private HashSet<String> inheritedFiles = new HashSet<String>(); 
		private HashSet<String> removedFiles = new HashSet<String>();
		public HashMap<String, String> filesToPath = new HashMap<String,String>();
		private HashMap<String, Boolean> fInheritanceCheck = new HashMap<String, Boolean>();

		public Commit(Commit previousC, int id, String info, Date time, HashSet<String> newFiles) {
			this.prev = previousC;
			this.commitId = id;
			this.cMessage = info;
			this.timeStamp = time;
			this.addedFiles = newFiles;
			// Setting default inheritability of added files for future commits and path attachements.
			for (String file : addedFiles) {
				String path = "./.gitlet/" + this.commitId + "/" + file;
				this.filesToPath.put(file, path);
				this.fInheritanceCheck.put(file, true);
			}
			// Saving inheritable files from the previous commit as Inherited Files and path attachements.
			if (this.prev != null && this.prev.filesToPath != null) {
				for (String file : this.prev.filesToPath.keySet()) {
					if (this.prev.fInheritanceCheck.get(file) && (!this.filesToPath.containsKey(file))) {
						this.inheritedFiles.add(file);
						this.filesToPath.put(file, this.prev.filesToPath.get(file));
						this.fInheritanceCheck.put(file, true);							
					} else {
						// Files that cannot be inherited, are termed "removed" since last commit.
						this.removedFiles.add(file);
					}
				}
			}
		}

		public String getMessage() {
			return this.cMessage;
		}
	}

}
