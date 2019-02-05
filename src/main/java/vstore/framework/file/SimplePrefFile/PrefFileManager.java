package vstore.framework.file.SimplePrefFile;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vstore.framework.error.ErrorCode;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.FileManager;

/**
 * Entry point for getting the preference files of the framework.
 */
public class PrefFileManager {

    private static final Logger LOGGER = LogManager.getLogger(PrefFileManager.class);

    private static HashMap<String, PrefFile> prefFiles = new HashMap<>();

    public static PrefFile getPrefFile(String filename) throws VStoreException {
        LOGGER.debug("Getting pref file " + filename);
        if(!prefFiles.containsKey(filename))
        {
            File pref_root = FileManager.get().getPrefFileDir();
            if (pref_root == null) {
                throw new VStoreException(ErrorCode.BASE_DIRECTORY_DOES_NOT_EXIST, "");
            }

            File fileObj = new File(pref_root, filename + ".json");
            if (!fileObj.exists())
            {
                try {
                    fileObj.createNewFile();
                    FileWriter fw = new FileWriter(fileObj.getAbsoluteFile());
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write("{}");
                    bw.close();
                } catch (IOException e) {
                    System.out.println("Error creating file");
                    e.printStackTrace();
                    return null;
                }
            }
            prefFiles.put(filename, new PrefFile(fileObj));
        }
        return prefFiles.get(filename);
    }
}
