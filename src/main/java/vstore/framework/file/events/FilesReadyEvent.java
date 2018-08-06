package vstore.framework.file.events;

import java.util.ArrayList;
import java.util.List;

import vstore.framework.file.VStoreFile;

/**
 * This event gets published when your file request you made to the framework is ready.
 * If you want your app to get notified, you need to subscribe to this event.
 */
public class FilesReadyEvent {
    private List<VStoreFile> mFiles;
    private boolean mOnlyPendingFiles;
    private boolean mOnlyPrivateFiles;

    /**
     * Constructs a new FilesReadyEvent.
     * @param fileList A list containing the files from the database.
     * @param onlyPendingFiles Set this to true, if the event contains only files that have an upload pending.
     * @param onlyPrivateFiles Set this to true, if the event contains only private files.
     */
    public FilesReadyEvent(List<VStoreFile> fileList,
                           boolean onlyPendingFiles, boolean onlyPrivateFiles) {
        if(fileList != null) {
            mFiles = new ArrayList<>(fileList);
            mOnlyPendingFiles = onlyPendingFiles;
            mOnlyPrivateFiles = onlyPrivateFiles;
        }
    }

    /**
     * @return Returns the files provided with this event.
     */
    public List<VStoreFile> getFiles() {
        return mFiles;
    }

    /**
     * @return True, if the file list in this event contains only files that have a
     * currently pending upload. False otherwise.
     */
    public boolean isOnlyPendingFiles() {
        return mOnlyPendingFiles;
    }

    /**
     * @return True, if the file list in this event contains only files that are flagged as
     * private. False otherwise.
     */
    public boolean isOnlyPrivateFiles() {
        return mOnlyPrivateFiles;
    }
}
