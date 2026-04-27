package javax.microedition.media;

import java.io.InputStream;
import java.io.IOException;

/**
 * J2ME Manager stub for Android compatibility.
 * Factory for creating Players - returns stub implementations.
 */
public class Manager {
    
    public static final String TONE_DEVICE_LOCATOR = "device://tone";
    public static final String MIDI_DEVICE_LOCATOR = "device://midi";
    
    private Manager() {
        // Private constructor - static factory methods only
    }
    
    /**
     * Create a Player from a locator string.
     */
    public static Player createPlayer(String locator) throws IOException, MediaException {
        return new StubPlayer();
    }
    
    /**
     * Create a Player from an InputStream with content type.
     */
    public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException {
        if (type != null && (type.contains("midi") || type.contains("mid"))) {
            return new MediaPlayerWrapper(stream, type);
        }
        return new SoundPoolPlayer(stream, type);
    }
    
    /**
     * Play a single tone.
     */
    public static void playTone(int note, int duration, int volume) throws MediaException {
        // Stub - no actual implementation
    }
    
    /**
     * Get supported content types for a protocol.
     */
    public static String[] getSupportedContentTypes(String protocol) {
        return new String[0];
    }
    
    /**
     * Get supported protocols for a content type.
     */
    public static String[] getSupportedProtocols(String contentType) {
        return new String[0];
    }
    
    /**
     * Stub Player implementation that does nothing.
     */
    private static class StubPlayer implements Player {
        private int state = UNREALIZED;
        
        @Override
        public void realize() throws MediaException {
            state = REALIZED;
        }
        
        @Override
        public void prefetch() throws MediaException {
            state = PREFETCHED;
        }
        
        @Override
        public void start() throws MediaException {
            state = STARTED;
        }
        
        @Override
        public void stop() throws MediaException {
            state = PREFETCHED;
        }
        
        @Override
        public void deallocate() {
            state = REALIZED;
        }
        
        @Override
        public void close() {
            state = CLOSED;
        }
        
        @Override
        public long setMediaTime(long now) throws MediaException {
            return now;
        }
        
        @Override
        public long getMediaTime() {
            return TIME_UNKNOWN;
        }
        
        @Override
        public int getState() {
            return state;
        }
        
        @Override
        public long getDuration() {
            return TIME_UNKNOWN;
        }
        
        @Override
        public String getContentType() {
            return "audio/unknown";
        }
        
        @Override
        public void setLoopCount(int count) {
        }
        
        @Override
        public void addPlayerListener(PlayerListener playerListener) {
        }
        
        @Override
        public void removePlayerListener(PlayerListener playerListener) {
        }
    }
}
