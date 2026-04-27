package javax.microedition.media;

/**
 * J2ME Player interface stub for Android compatibility.
 * Represents an audio/video player.
 */
public interface Player {
    
    public static final int UNREALIZED = 100;
    public static final int REALIZED = 200;
    public static final int PREFETCHED = 300;
    public static final int STARTED = 400;
    public static final int CLOSED = 0;
    public static final long TIME_UNKNOWN = -1;
    
    void realize() throws MediaException;
    
    void prefetch() throws MediaException;
    
    void start() throws MediaException;
    
    void stop() throws MediaException;
    
    void deallocate();
    
    void close();
    
    long setMediaTime(long now) throws MediaException;
    
    long getMediaTime();
    
    int getState();
    
    long getDuration();
    
    String getContentType();
    
    void setLoopCount(int count);
    
    void addPlayerListener(PlayerListener playerListener);
    
    void removePlayerListener(PlayerListener playerListener);
}
