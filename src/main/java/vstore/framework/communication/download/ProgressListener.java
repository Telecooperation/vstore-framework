package vstore.framework.communication.download;

public interface ProgressListener {
    void update(long bytesRead, long contentLength, boolean done);
}
