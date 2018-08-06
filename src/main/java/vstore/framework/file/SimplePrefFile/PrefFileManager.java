package vstore.framework.file.SimplePrefFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import vstore.framework.error.ErrorCode;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.FileManager;

/**
 * Entry point for getting the preference files of the framework.
 */
public class PrefFileManager {

    private static HashMap<String, PrefFile> prefFiles = new HashMap<>();

    public static PrefFile getPrefFile(String filename) throws VStoreException {
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
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            prefFiles.put(filename, new PrefFile(fileObj));
        }
        return prefFiles.get(filename);
    }
}
