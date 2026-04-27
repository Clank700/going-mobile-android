package javax.microedition.rms;

/**
 * J2ME RecordStoreException compatibility.
 */
public class RecordStoreException extends Exception {
    public RecordStoreException() {
        super();
    }
    
    public RecordStoreException(String message) {
        super(message);
    }
}
