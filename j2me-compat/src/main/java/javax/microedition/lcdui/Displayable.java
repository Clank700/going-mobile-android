package javax.microedition.lcdui;

/**
 * J2ME Displayable base class.
 * Canvas and other screen types extend this.
 */
public abstract class Displayable {
    private String title;
    private Display display;
    
    protected Displayable() {
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getTitle() {
        return title;
    }
    
    public abstract int getWidth();
    public abstract int getHeight();
    
    public boolean isShown() {
        return display != null && display.getCurrent() == this;
    }
    
    // Internal - set by Display
    void setDisplay(Display display) {
        this.display = display;
    }
    
    Display getDisplay() {
        return display;
    }
    
    public void addCommand(Command cmd) {
        // Stub - commands not used on Android
    }

    public void removeCommand(Command cmd) {
        // Stub
    }

    public void setCommandListener(CommandListener listener) {
        // Stub - commands not used on Android
    }

    /**
     * Called when size changes.
     */
    protected void sizeChanged(int w, int h) {
        // Override in subclass
    }
}
