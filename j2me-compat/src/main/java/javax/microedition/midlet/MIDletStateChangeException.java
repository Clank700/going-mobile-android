package javax.microedition.midlet;

/**
 * J2ME MIDletStateChangeException compatibility.
 */
public class MIDletStateChangeException extends Exception {
    
    public MIDletStateChangeException() {
        super();
    }
    
    public MIDletStateChangeException(String message) {
        super(message);
    }
}
