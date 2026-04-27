package javax.microedition.media;

/**
 * J2ME MediaException for Android compatibility.
 */
public class MediaException extends Exception {
    
    public MediaException() {
        super();
    }
    
    public MediaException(String message) {
        super(message);
    }
}
