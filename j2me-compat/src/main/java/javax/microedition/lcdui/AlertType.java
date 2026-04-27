package javax.microedition.lcdui;

/**
 * J2ME AlertType stub for Android compatibility.
 */
public class AlertType {
    public static final AlertType ALARM = new AlertType();
    public static final AlertType CONFIRMATION = new AlertType();
    public static final AlertType ERROR = new AlertType();
    public static final AlertType INFO = new AlertType();
    public static final AlertType WARNING = new AlertType();
    
    protected AlertType() {}
    
    public boolean playSound(Display display) {
        return false; // Sound not implemented
    }
}
