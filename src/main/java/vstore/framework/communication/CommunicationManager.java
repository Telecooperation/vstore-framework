package vstore.framework.communication;

import vstore.framework.communication.threads.DeleteFilesThread;

public class CommunicationManager {
	
	private static CommunicationManager instance;
	private static DeleteFilesThread thDelete;
	
	private CommunicationManager() {
		
	}
	
	public static synchronized CommunicationManager get() {
		if(instance == null) {
			instance = new CommunicationManager();
		}
		return instance;
	}
	
	public void runDeletions() {
		if(thDelete != null && thDelete.isAlive()) { return; }
		
		//Start a new thread for deletion of files
		thDelete = new DeleteFilesThread();
		thDelete.start();
	}
}
