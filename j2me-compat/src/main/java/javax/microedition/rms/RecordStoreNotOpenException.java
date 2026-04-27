package javax.microedition.rms;

/**
 * J2ME RecordStoreNotOpenException compatibility.
 */
public class RecordStoreNotOpenException extends RecordStoreException {
    public RecordStoreNotOpenException() {
        super();
    }
    
    public RecordStoreNotOpenException(String message) {
        super(message);
    }
}
