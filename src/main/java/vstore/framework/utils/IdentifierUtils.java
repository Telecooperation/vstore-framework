package vstore.framework.utils;

import java.io.File;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import vstore.framework.file.FileManager;

public class IdentifierUtils {

	private static final Logger LOGGER = LogManager.getLogger(IdentifierUtils.class);

    /**
     * Generates the file which contains the device identifier.
     */
	public static void generateDeviceIdentifier() {
        if(identifierFileNotExists()) {
            String uuid = getNewUniqueIdentifier();
            LOGGER.info("Identifier file did not exist, generated new device identifier:" + uuid);
            LOGGER.debug("Writing device identifier to file...");
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
		    LOGGER.warn("Device identifier file did not exist upon calling getDeviceIdentifier()");
			generateDeviceIdentifier();
        }
	    LOGGER.debug("Getting device identifier from file");
		String uuid =  FileUtils.readStringFromFile(new File(FileManager.get().getVStoreDir(), "device")).trim();
		return uuid;
	}
}
