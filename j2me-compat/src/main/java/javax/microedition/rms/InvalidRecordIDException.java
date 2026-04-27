package javax.microedition.rms;

/**
 * J2ME InvalidRecordIDException compatibility.
 */
public class InvalidRecordIDException extends RecordStoreException {
    public InvalidRecordIDException() {
        super();
    }
    
    public InvalidRecordIDException(String message) {
        super(message);
    }
}
