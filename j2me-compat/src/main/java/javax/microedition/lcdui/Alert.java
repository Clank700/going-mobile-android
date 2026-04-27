package javax.microedition.lcdui;

/**
 * Placeholder Alert class.
 * Full implementation not needed for game porting.
 */
public class Alert extends Displayable {
    public static final int FOREVER = -2;
    
    public static final AlertType ALARM = new AlertType();
    public static final AlertType CONFIRMATION = new AlertType();
    public static final AlertType ERROR = new AlertType();
    public static final AlertType INFO = new AlertType();
    public static final AlertType WARNING = new AlertType();
    
    private String title;
    private String text;
    private AlertType type;
    private int timeout = FOREVER;
    
    public Alert(String title) {
        this.title = title;
    }
    
    public Alert(String title, String alertText, Image alertImage, AlertType alertType) {
        this.title = title;
        this.text = alertText;
        this.type = alertType;
    }
    
    public void setString(String str) {
        this.text = str;
    }
    
    public String getString() {
        return text;
    }
    
    public void setTimeout(int time) {
        this.timeout = time;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    @Override
    public int getWidth() {
        return 240;
    }
    
    @Override
    public int getHeight() {
        return 320;
    }
    
}
