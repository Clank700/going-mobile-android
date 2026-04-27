package javax.microedition.lcdui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * J2ME Canvas compatibility layer for Android.
 * This is the core rendering class that games extend.
 * Maps J2ME Canvas lifecycle to Android SurfaceView.
 */
public abstract class Canvas extends Displayable {
    // Key code constants (J2ME uses different values than Android)
    public static final int KEY_NUM0 = 48;
    public static final int KEY_NUM1 = 49;
    public static final int KEY_NUM2 = 50;
    public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52;
    public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54;
    public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56;
    public static final int KEY_NUM9 = 57;
    public static final int KEY_STAR = 42;
    public static final int KEY_POUND = 35;
    
    // Game action constants
    public static final int UP = 1;
    public static final int DOWN = 6;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;
    
    private CanvasView canvasView;
    private boolean fullScreenMode = false;
    private int width;
    private int height;

    // Static defaults set by CanvasView.setGameDimensions() before any Canvas is created
    private static int defaultWidth = 240;
    private static int defaultHeight = 320;

    protected Canvas() {
        this.width = defaultWidth;
        this.height = defaultHeight;
    }

    static void setDefaultDimensions(int w, int h) {
        defaultWidth = w;
        defaultHeight = h;
    }
    
    /**
     * Called by the system to render the canvas.
     * Subclasses must implement this to draw game content.
     */
    protected abstract void paint(Graphics g);
    
    /**
     * Request a repaint of the canvas.
     */
    public final void repaint() {
        if (canvasView != null) {
            canvasView.requestRepaint();
        }
    }
    
    /**
     * Request a repaint of a specific region.
     */
    public final void repaint(int x, int y, int width, int height) {
        repaint(); // For simplicity, repaint entire canvas
    }
    
    /**
     * Force immediate repaint and wait for completion.
     */
    public final void serviceRepaints() {
        if (canvasView != null) {
            canvasView.serviceRepaints();
        }
    }
    
    /**
     * Called when a key is pressed.
     */
    protected void keyPressed(int keyCode) {
        // Override in subclass
    }
    
    /**
     * Called when a key is released.
     */
    protected void keyReleased(int keyCode) {
        // Override in subclass
    }
    
    /**
     * Called when a key is held down (repeated).
     */
    protected void keyRepeated(int keyCode) {
        // Override in subclass
    }
    
    /**
     * Called when pointer (touch) is pressed.
     */
    protected void pointerPressed(int x, int y) {
        // Override in subclass
    }
    
    /**
     * Called when pointer (touch) is released.
     */
    protected void pointerReleased(int x, int y) {
        // Override in subclass
    }
    
    /**
     * Called when pointer (touch) is dragged.
     */
    protected void pointerDragged(int x, int y) {
        // Override in subclass
    }
    
    /**
     * Called when canvas becomes visible.
     */
    protected void showNotify() {
        // Override in subclass
    }
    
    /**
     * Called when canvas becomes hidden.
     */
    protected void hideNotify() {
        // Override in subclass
    }
    
    /**
     * Called when canvas size changes.
     */
    protected void sizeChanged(int w, int h) {
        this.width = w;
        this.height = h;
    }
    
    /**
     * Get game action for a key code.
     */
    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case KEY_NUM2:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                return UP;
            case KEY_NUM8:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                return DOWN;
            case KEY_NUM4:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                return LEFT;
            case KEY_NUM6:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                return RIGHT;
            case KEY_NUM5:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
                return FIRE;
            case KEY_NUM1:
                return GAME_A;
            case KEY_NUM7:
                return GAME_C;
            case KEY_NUM9:
                return GAME_D;
            default:
                return 0;
        }
    }
    
    /**
     * Get key code for a game action.
     */
    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case UP: return KEY_NUM2;
            case DOWN: return KEY_NUM8;
            case LEFT: return KEY_NUM4;
            case RIGHT: return KEY_NUM6;
            case FIRE: return KEY_NUM5;
            case GAME_A: return KEY_NUM1;
            case GAME_B: return KEY_NUM3;
            case GAME_C: return KEY_NUM7;
            case GAME_D: return KEY_NUM9;
            default: return 0;
        }
    }
    
    /**
     * Get name of a key.
     */
    public String getKeyName(int keyCode) {
        if (keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) {
            return String.valueOf((char) keyCode);
        }
        switch (keyCode) {
            case KEY_STAR: return "*";
            case KEY_POUND: return "#";
            default: return "KEY_" + keyCode;
        }
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public boolean isDoubleBuffered() {
        return true; // Android SurfaceView is always double-buffered
    }
    
    public boolean hasPointerEvents() {
        return true; // All Android devices support touch
    }
    
    public boolean hasPointerMotionEvents() {
        return true;
    }
    
    public boolean hasRepeatEvents() {
        return true;
    }
    
    public void setFullScreenMode(boolean mode) {
        this.fullScreenMode = mode;
    }
    
    public boolean isFullScreenMode() {
        return fullScreenMode;
    }
    
    // Internal methods for Android integration
    void setCanvasView(CanvasView view) {
        this.canvasView = view;
    }
    
    CanvasView getCanvasView() {
        return canvasView;
    }
    
    void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    /**
     * Convert Android KeyEvent to J2ME key code.
     */
    public static int androidKeyToJ2ME(int androidKeyCode) {
        switch (androidKeyCode) {
            case KeyEvent.KEYCODE_0: return KEY_NUM0;
            case KeyEvent.KEYCODE_1: return KEY_NUM1;
            case KeyEvent.KEYCODE_2: return KEY_NUM2;
            case KeyEvent.KEYCODE_3: return KEY_NUM3;
            case KeyEvent.KEYCODE_4: return KEY_NUM4;
            case KeyEvent.KEYCODE_5: return KEY_NUM5;
            case KeyEvent.KEYCODE_6: return KEY_NUM6;
            case KeyEvent.KEYCODE_7: return KEY_NUM7;
            case KeyEvent.KEYCODE_8: return KEY_NUM8;
            case KeyEvent.KEYCODE_9: return KEY_NUM9;
            case KeyEvent.KEYCODE_STAR: return KEY_STAR;
            case KeyEvent.KEYCODE_POUND: return KEY_POUND;
            // D-pad and WASD map to number keys
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W: return KEY_NUM2;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S: return KEY_NUM8;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A: return KEY_NUM4;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D: return KEY_NUM6;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE: return KEY_NUM5;
            default: return androidKeyCode; // Pass through unknown keys
        }
    }
}
