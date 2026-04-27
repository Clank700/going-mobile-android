package javax.microedition.lcdui;

/**
 * J2ME TextField stub for Android compatibility.
 * TextField is a text input item used in Forms.
 */
public class TextField extends Item {
    
    public static final int ANY = 0;
    public static final int EMAILADDR = 1;
    public static final int NUMERIC = 2;
    public static final int PHONENUMBER = 3;
    public static final int URL = 4;
    public static final int PASSWORD = 0x10000;
    public static final int CONSTRAINT_MASK = 0xFFFF;
    
    private String text;
    private int maxSize;
    private int constraints;
    
    public TextField(String label, String text, int maxSize, int constraints) {
        this.text = text != null ? text : "";
        this.maxSize = maxSize;
        this.constraints = constraints;
    }
    
    public String getString() {
        return text;
    }
    
    public void setString(String text) {
        this.text = text != null ? text : "";
    }
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public int setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return maxSize;
    }
    
    public int size() {
        return text.length();
    }
    
    public int getConstraints() {
        return constraints;
    }
    
    public void setConstraints(int constraints) {
        this.constraints = constraints;
    }
    
    public void insert(String src, int position) {
        if (src != null && position >= 0 && position <= text.length()) {
            text = text.substring(0, position) + src + text.substring(position);
        }
    }
    
    public void delete(int offset, int length) {
        if (offset >= 0 && offset + length <= text.length()) {
            text = text.substring(0, offset) + text.substring(offset + length);
        }
    }
    
    public int getCaretPosition() {
        return text.length();
    }
    
    public void setChars(char[] data, int offset, int length) {
        if (data != null) {
            text = new String(data, offset, length);
        }
    }
    
    public int getChars(char[] data) {
        text.getChars(0, text.length(), data, 0);
        return text.length();
    }
}
