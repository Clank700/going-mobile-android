package javax.microedition.rms;

/**
 * J2ME RecordStoreNotFoundException compatibility.
 */
public class RecordStoreNotFoundException extends RecordStoreException {
    public RecordStoreNotFoundException() {
        super();
    }
    
    public RecordStoreNotFoundException(String message) {
        super(message);
    }
}
