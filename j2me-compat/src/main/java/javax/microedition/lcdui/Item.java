package javax.microedition.lcdui;

/**
 * J2ME Item stub for Android compatibility.
 */
public abstract class Item {
    public static final int LAYOUT_DEFAULT = 0;
    public static final int LAYOUT_LEFT = 1;
    public static final int LAYOUT_RIGHT = 2;
    public static final int LAYOUT_CENTER = 3;
    
    private String label;
    
    public Item() {}
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getLayout() { return LAYOUT_DEFAULT; }
    public void setLayout(int layout) {}
    public int getMinimumHeight() { return 0; }
    public int getMinimumWidth() { return 0; }
    public int getPreferredHeight() { return -1; }
    public int getPreferredWidth() { return -1; }
    public void setPreferredSize(int width, int height) {}
}
