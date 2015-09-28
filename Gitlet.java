import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Date;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.TreeSet;
import java.util.Scanner;


public class Gitlet {
	public static void initialize() {
		try {
			//Creating new .gitlet directory
			Path dir = Paths.get("./.gitlet");
			Files.createDirectory(dir);

			//Creating the CurrentStage.ser file
			Path stage = Paths.get("./.gitlet/CurrentStage.ser");
			Files.createFile(stage);
			HashSet<String> initAddSet = new HashSet<String>();
			Functions.serializeFile("./.gitlet/CurrentStage.ser", initAddSet);

			//Creating RemoveFiles.ser file
			Path rFile = Paths.get("./.gitlet/RemoveFiles.ser");
			Files.createFile(rFile);
			HashSet<String> initRemoveSet = new HashSet<String>();
			Functions.serializeFile("./.gitlet/RemoveFiles.ser", initRemoveSet);

			//Creating the CommitTree DataStructure and Seriazable file
			Date initTimeStamp = new Date();
			CommitTree commTree = new CommitTree(initTimeStamp);
			Path commPath = Paths.get("./.gitlet/commtree.ser");
			Files.createFile(commPath);
			Functions.serializeFile("./.gitlet/commtree.ser", commTree);
		} catch (IOException | UnsupportedOperationException | SecurityException e) {
			System.out.println("A gitlet version control system already exists in the current directory."); 
			e.printStackTrace();
		}
	}
	public static void add(String file) {
		File stagedFile = new File("./" + file);
        if (!stagedFile.exists()) {
        	System.out.println("File does not exist.");
        } else {
        	// The file exists and we check whether it has changed since last commit.
        	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
        	HashSet<String> removeSet = (HashSet<String>) Functions.deSerializeFile("./.gitlet/RemoveFiles.ser");
        	//System.out.println(comm.branches.get(comm.activeBranch));
		    int lastID = comm.lastCommit().commitId;
		    String lastCommitFilePath = "./.gitlet/" + lastID + "/" + file;
		    String ourFilePath = "./" + file;  
		    boolean contains = comm.lastCommitContainsFile(file);
			if (contains && Functions.compareFiles(ourFilePath, lastCommitFilePath)) {
				System.out.println("File has not been modified since the last commit.");
	        } else if (removeSet.contains(file)) {
	        	// If file was marked for removal earlier, change that.
	        	removeSet.remove(file);
	        	comm.removeFuture(file, "revert");
	        	Functions.serializeFile("./.gitlet/RemoveFiles.ser", removeSet);
	        	Functions.serializeFile("./.gitlet/commtree.ser", comm);
	        	// Stage the file.
	        	HashSet<String> addFilesSet = (HashSet<String>) Functions.deSerializeFile("./.gitlet/CurrentStage.ser");
	        	addFilesSet.add(file);
	        	Functions.serializeFile("./.gitlet/CurrentStage.ser", addFilesSet);
	        } else {
	        	HashSet<String> addFilesSet = (HashSet<String>) Functions.deSerializeFile("./.gitlet/CurrentStage.ser");
	        	addFilesSet.add(file);
	        	Functions.serializeFile("./.gitlet/CurrentStage.ser", addFilesSet);
	        }
		}
    }

    public static void commit(String message) {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
	    HashSet<String> addedFiles = (HashSet<String>) Functions.deSerializeFile("./.gitlet/CurrentStage.ser");
	    if(message.length() == 0) {
	    	System.out.println("Please enter a commit message.");
	    } else if (addedFiles.size() == 0 && (!comm.justRemoved)) {
	    	System.out.println("No changes added to the commit.");
	    } else {
			int newCommitID = comm.lastCommitId() + 1;
			try{
				//Creating new commit directory within the .gitlet directory.
				Path cDir = Paths.get("./.gitlet/" + newCommitID);
				Files.createDirectory(cDir);

				//Copying Files into the new directory.
				for (String file : addedFiles) {
					Path toAd = Paths.get("./" + file);
					Path dest = Paths.get("./.gitlet/" + newCommitID + "/" + file);
					File dhr = new File("./.gitlet/" + newCommitID + "/" + file).getParentFile();
					if (!dhr.exists()) {
						dhr.mkdir();
					}
					Files.copy(toAd, dest, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				// System.out.println("Problem in creating new commit directory and saving files");
				throw new RuntimeException(e);
			}
			// Add this commit as a node in the commit tree.
			Date time = new Date();
			comm.createNode(comm.lastCommit(), newCommitID, message, time, addedFiles);
			comm.justRemoved = false;
	    	// Empty the CurrentStage File after commit. 
	    	HashSet<String> emptySet = new HashSet<String>();
	    	Functions.serializeFile("./.gitlet/CurrentStage.ser", emptySet);
	    	Functions.serializeFile("./.gitlet/RemoveFiles.ser", emptySet);
	    	Functions.serializeFile("./.gitlet/commtree.ser", comm);
    	}
    }

    public static void remove(String file) {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
	    HashSet<String> addedFiles = (HashSet<String>) Functions.deSerializeFile("./.gitlet/CurrentStage.ser");
	    if (addedFiles.size() == 0 && (!comm.lastCommitContainsFile(file))) {
	    	System.out.println("No reason to remove the file.");
	    } else  {
	    	if (addedFiles.contains(file)) {
	    		addedFiles.remove(file); // Removes the file if it has been staged earlier.
	    		Functions.serializeFile("./.gitlet/CurrentStage.ser", addedFiles);
	    	} else {
	    		comm.removeFuture(file, "remove"); // Wouldn't inherit the file in the next commit if present.
	    		Functions.serializeFile("./.gitlet/commtree.ser", comm);
		    	HashSet<String> removeSet = (HashSet<String>) Functions.deSerializeFile("./.gitlet/RemoveFiles.ser");
		    	removeSet.add(file);
		    	Functions.serializeFile("./.gitlet/RemoveFiles.ser", removeSet);
	    	}
	    }
    }

    public static void log() {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
	    comm.printLog();
    }

    public static void globallog() {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
    	int stop = comm.lastCommitId();
    	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	String formattedDate;
    	for (int ctr = stop; ctr >= 0; ctr -= 1) {
    		System.out.println("====");	
			System.out.println("Commit " + ctr + ".");
    		formattedDate = df.format(comm.idToCommit.get(ctr).timeStamp);
    		System.out.println(formattedDate);
    		System.out.println(comm.idToCommit.get(ctr).getMessage());
    		System.out.println();
    	}
    }

    public static void find(String message) {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
    	if (comm.messageToId != null && comm.messageToId.containsKey(message)) {
    		TreeSet<Integer> idList = comm.messageToId.get(message);
    		for (Integer id : idList) {
    			System.out.println(id);
    		}
    	} else {
    		System.out.println("Found no commit with that message.");
    	}
    }

    public static void status() {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
    	System.out.println("=== Branches ===");
    	System.out.println("*" + comm.activeBranch);
    	for (String branch : comm.branches.keySet()) {
    		if (!branch.equals(comm.activeBranch)) {
    			System.out.println(branch);
    		}
    	}
    	System.out.println();

    	System.out.println("=== Staged Files ===");
		HashSet<String> addFilesSet = (HashSet<String>) Functions.deSerializeFile("./.gitlet/CurrentStage.ser");
		for (String file : addFilesSet) {
			System.out.println(file);
		}
		System.out.println();

		System.out.println("=== Files Marked for Removal ===");
		HashSet<String> removeSet = (HashSet<String>) Functions.deSerializeFile("./.gitlet/RemoveFiles.ser");
		for (String file : removeSet) {
			System.out.println(file);
		}
		System.out.println();
    }

    public static void branch(String bname) {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
    	if (comm.branches.containsKey(bname)) {
    		System.out.println("A branch with that name already exists.");
    	} else {
    		comm.branchAdd(bname);
    	} 
    	Functions.serializeFile("./.gitlet/commtree.ser", comm);
    } 

    public static void checkout(String name) {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
    	Path pathToAd, dest;
    	// If the name is branch name.
    	if (comm.branches.containsKey(name)) {
    		if (name.equals(comm.activeBranch)) {
    			System.out.println("No need to checkout the current branch.");
    		} else {
    			// Switch to new branch.
	    		comm.changeActivePointer(name);
	    		Functions.serializeFile("./.gitlet/commtree.ser", comm);
	    		// Look up the file paths in the headNode of the new branch.
	    		HashMap<String, String> headNodePaths = comm.lastCommit().filesToPath;
		    	try{	
		    		// Restores all files in the working directory to their versions in the commit
		    		for (String file : headNodePaths.keySet()) {
						pathToAd = Paths.get(headNodePaths.get(file));
						dest = Paths.get("./" + file);
						Files.copy(pathToAd, dest, StandardCopyOption.REPLACE_EXISTING);
					}
				} catch (IOException e) {
					// System.out.println("Problem in checking out a new branch");
					throw new RuntimeException(e);
				}
			}
    	} else {
    		// Case for FIles.
    		HashMap<String, String> headPaths = comm.lastCommit().filesToPath;
    		try {
	    		if (headPaths.containsKey(name)) {
	    			pathToAd = Paths.get(headPaths.get(name));
					dest = Paths.get("./" + name);
					Files.copy(pathToAd, dest, StandardCopyOption.REPLACE_EXISTING);
	    		} else {
	    			System.out.println("File does not exist in the most recent commit, or no such branch exists.");
	    		}
			} catch (IOException e) {
					// System.out.println("Problem in checking out a file");
				throw new RuntimeException(e);

			}
    	}
    }

    public static void checkout(int id, String fname) {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
    	HashMap<String, String> headNodePaths;
    	if (comm.idToCommit != null && comm.idToCommit.get(id) != null) {
    		headNodePaths = comm.idToCommit.get(id).filesToPath;
	    	try {
		    	if (headNodePaths.containsKey(fname)) {
		    		Path pathToAd = Paths.get(headNodePaths.get(fname));
					Path dest = Paths.get("./" + fname);
					Files.copy(pathToAd, dest, StandardCopyOption.REPLACE_EXISTING);
		    	} else {
		    		System.out.println("File does not exist in that commit.");
		    	}
			} catch (IOException e) {
					// System.out.println("Problem in checking out a file");
				throw new RuntimeException(e);
			}
    	} else {
    		System.out.println("No commit with that id exists.");
    	}
    }

    public static void removeBranch(String bname) {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
    	if (!comm.branches.containsKey(bname)) {
    		System.out.println("A branch with that name does not exist.");
    	} else if (bname.equals(comm.activeBranch)) {
    		System.out.println("Cannot remove the current branch.");
    	} else {
    		comm.branches.remove(bname);
    		Functions.serializeFile("./.gitlet/commtree.ser", comm);
    	}
    }

    public static void reset(int id) {
		CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
    	HashMap<String, String> headNodePaths;
    	if (comm.idToCommit != null && comm.idToCommit.get(id) != null) {
    		headNodePaths = comm.idToCommit.get(id).filesToPath;
    		comm.addNode(comm.idToCommit.get(id));
    		Functions.serializeFile("./.gitlet/commtree.ser", comm);
	    	try {
		    	for (String fname : headNodePaths.keySet()) {
		    		Path pathToAd = Paths.get(headNodePaths.get(fname));
					Path dest = Paths.get("./" + fname);
					Files.copy(pathToAd, dest, StandardCopyOption.REPLACE_EXISTING);
		    	} 
			} catch (IOException e) {
					// System.out.println("Problem in resetting");
			}
    	} else {
    		System.out.println("No commit with that id exists.");
    	}
    }

    public static void merge(String bname) {
    	CommitTree comm = (CommitTree) Functions.deSerializeFile("./.gitlet/commtree.ser");
    	if (!comm.branches.containsKey(bname)) {
    		System.out.println("A branch with that name does not exist.");
    	} else if (bname.equals(comm.activeBranch)) {
    		System.out.println("Cannot merge a branch with itself.");
    	} else {
    		CommitTree.Commit otherNode = comm.branches.get(bname); 
    		CommitTree.Commit splitNode = comm.findSplitNode(comm.lastCommit(), otherNode);
	    	HashMap<String, String> referenceFiles = splitNode.filesToPath;
	    	HashMap<String, String> activeFiles = comm.lastCommit().filesToPath;
	    	HashMap<String, String> otherFiles = otherNode.filesToPath;
	    	HashMap<String, String> compareOne = new HashMap<String, String>();
	    	HashMap<String, String> compareTwo = new HashMap<String, String>();
	    	for (String file : activeFiles.keySet()) {
	    		if ((!referenceFiles.containsKey(file))){
	    			compareOne.put(file, activeFiles.get(file));
	    		} else if (!Functions.compareFiles(referenceFiles.get(file), activeFiles.get(file))) {
	    			compareOne.put(file, activeFiles.get(file));
	    		}
	    	}
	    	for (String file : otherFiles.keySet()) {
	    		if ((!referenceFiles.containsKey(file))){
	    			compareTwo.put(file, otherFiles.get(file));
	    		} else if (!Functions.compareFiles(referenceFiles.get(file), otherFiles.get(file))) {
	    			compareTwo.put(file, otherFiles.get(file));
	    		}
	    	}
	    	try {
		    	for (String file : compareTwo.keySet()) {
		    		if (compareOne.containsKey(file)){
		    			Path pathToAd = Paths.get(compareTwo.get(file));
						Path dest = Paths.get("./" + file + ".conflicted");
						Files.copy(pathToAd, dest, StandardCopyOption.REPLACE_EXISTING);
		    		} else {
		    			Path pathToAd = Paths.get(compareTwo.get(file));
						Path dest = Paths.get("./" + file);
						Files.copy(pathToAd, dest, StandardCopyOption.REPLACE_EXISTING);
		    		}
		    	}
		    } catch (IOException e) {
				// System.out.println("Problem in merging.");
			}
    	}
    }

    public static void main(String[] args) {
    	String command = args[0]; 
    	String opinion;
    	String[] tokens = new String[args.length - 1];
        System.arraycopy(args, 1, tokens, 0, args.length - 1);
        Scanner scan;
        // try{
            switch (command) {
                case "init": initialize(); break;
                case "add": add(tokens[0]); break;  
                case "commit": 
                	if (tokens.length == 0) {
                		System.out.println("Please enter a commit message.");
                	} else {
                		commit(tokens[0]); 
                	} break;
                case "rm": remove(tokens[0]); break;
                case "log": log(); break;
                case "global-log": globallog(); break;
                case "find": find(tokens[0]); break;
                case "status": status(); break;
                case "checkout": System.out.println("Warning: The command you entered may alter the files in your working directory."); 
                	System.out.println("Uncommitted changes may be lost. Are you sure you want to continue? (yes/no)");
                	scan = new Scanner(System.in);
                	opinion = scan.nextLine();
                	if (opinion.equals("yes") || opinion.equals("y")) {
                		if (tokens.length > 1) {
                			checkout(Integer.parseInt(tokens[0]), tokens[1]);
                		} else {
                			checkout(tokens[0]);
                		}
                	}
                    break;
                case "branch": branch(tokens[0]); break;
                case "rm-branch": removeBranch(tokens[0]); break;
                case "reset": System.out.println("Warning: The command you entered may alter the files in your working directory."); 
                	System.out.println("Uncommitted changes may be lost. Are you sure you want to continue? (yes/no)");
                	scan = new Scanner(System.in);
                	opinion = scan.nextLine();
                	if (opinion.equals("yes") || opinion.equals("y")) {
                		reset(Integer.parseInt(tokens[0]));
                	}
                	break;
                case "merge": System.out.println("Warning: The command you entered may alter the files in your working directory."); 
                	System.out.println("Uncommitted changes may be lost. Are you sure you want to continue? (yes/no)");
                	scan = new Scanner(System.in);
                	opinion = scan.nextLine();
                	if (opinion.equals("yes") || opinion.equals("y")) {
                		merge(tokens[0]);
                	}
                	break;
                case "rebase": System.out.println("Warning: The command you entered may alter the files in your working directory."); 
                	System.out.println("Uncommitted changes may be lost. Are you sure you want to continue? (yes/no)");
                	scan = new Scanner(System.in);
                	opinion = scan.nextLine();
                	if (opinion.equals("yes") || opinion.equals("y")) {
                	}
                	break;
                case "i-rebase": System.out.println("Warning: The command you entered may alter the files in your working directory."); 
                	System.out.println("Uncommitted changes may be lost. Are you sure you want to continue? (yes/no)");
                	scan = new Scanner(System.in);
                	opinion = scan.nextLine();
                	if (opinion.equals("yes") || opinion.equals("y")) {
                	}
                	break;             
                default:
                      System.out.println("Invalid command.");  
                      break;
                }
        // }catch (RuntimeException e) {
        //     System.out.println("Invalid/ Extra Input");
        //     System.out.println(e);
        // }
    }
}


