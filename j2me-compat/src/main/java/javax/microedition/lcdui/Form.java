package javax.microedition.lcdui;

/**
 * J2ME Form stub for Android compatibility.
 * Form is a container for UI items like TextFields.
 */
public class Form extends Displayable {
    
    public Form(String title) {
        // Stub constructor
    }
    
    public Form(String title, Item[] items) {
        // Stub constructor
    }
    
    public int append(Item item) {
        return 0;
    }
    
    public int append(String str) {
        return 0;
    }
    
    public void delete(int itemNum) {
    }
    
    public void deleteAll() {
    }
    
    public Item get(int itemNum) {
        return null;
    }
    
    public void set(int itemNum, Item item) {
    }
    
    public void insert(int itemNum, Item item) {
    }
    
    public int size() {
        return 0;
    }
    
    @Override
    public int getWidth() {
        return 176; // Default J2ME screen width
    }
    
    @Override
    public int getHeight() {
        return 220; // Default J2ME screen height
    }
}
