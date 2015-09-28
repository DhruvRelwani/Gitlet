import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.File;
import java.io.IOException;

public class Functions {
	public static boolean compareFiles(String firstPath, String secondPath) {
		File first = new File(firstPath);
		File second = new File(secondPath);
		if (first.length() != second.length()) {
			return false;
		} else {
			try {
				FileInputStream fileInFirst = new FileInputStream(first);
				FileInputStream fileInSecond = new FileInputStream(second);
				for (int ctr = 0; ctr <= first.length(); ctr +=1) {
					if (fileInFirst.read() != fileInSecond.read()) {
						return false;
					}
				}
				return true;
			} 
			catch (IOException e) {
				// System.out.println("A problem in comparing files"); 
				return false;
			}
		}
	}

	public static void serializeFile(String path, Object dataStructure) {
		File sFile = new File(path);
		try (
	        FileOutputStream fileOut = new FileOutputStream(sFile);
	        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
	        )
		{
	        objectOut.writeObject(dataStructure);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.out.println("A problem in serializing files"); 
		}
	}

	public static Object deSerializeFile(String path) {
		File sFile = new File(path);
		try (
			FileInputStream fileIn = new FileInputStream(sFile);
		    ObjectInputStream objectIn = new ObjectInputStream(fileIn);
		    ) 
		{
		    return objectIn.readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			System.out.println("A problem in deserializing files");
			e.printStackTrace();
			return null; 
		}
	}
}
