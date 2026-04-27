package javax.microedition.media;

/**
 * J2ME PlayerListener interface stub for Android compatibility.
 * Used by b.java for audio event callbacks.
 */
public interface PlayerListener {
    
    public static final String STARTED = "started";
    public static final String STOPPED = "stopped";
    public static final String END_OF_MEDIA = "endOfMedia";
    public static final String VOLUME_CHANGED = "volumeChanged";
    public static final String ERROR = "error";
    public static final String CLOSED = "closed";
    
    /**
     * Called when a player event occurs.
     */
    void playerUpdate(Player player, String event, Object eventData);
}
