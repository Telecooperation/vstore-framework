package vstore.framework.utils;

import java.io.File;
import java.util.UUID;

import vstore.framework.file.FileManager;

public class IdentifierUtils {

    /**
     * Generates the file which contains the device identifier.
     */
	public static void generateDeviceIdentifier() {
        if(identifierFileNotExists()) {
            String uuid = getNewUniqueIdentifier();
            FileUtils.writeStringToFile(uuid, FileManager.get().getVStoreDir(), "device");
        }
	}

    /**
     * @return True, if the device identifier file does not exist yet.
     */
	private static boolean identifierFileNotExists() {
	    return !(new File(FileManager.get().getVStoreDir(), "device")).exists();
    }

    /**
     * @return A new unique identifier
     */
	public static String getNewUniqueIdentifier() {
		return UUID.randomUUID().toString();
	}

    /**
     * @return The unique identifier of this device.
     */
	public static String getDeviceIdentifier() {
		if(identifierFileNotExists()) {
		    generateDeviceIdentifier();
        }
	    return FileUtils.readStringFromFile(new File(FileManager.get().getVStoreDir(), "device")).trim();
	}
}
