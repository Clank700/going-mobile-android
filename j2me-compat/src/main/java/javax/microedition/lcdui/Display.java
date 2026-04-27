package javax.microedition.lcdui;

import android.app.Activity;

import javax.microedition.midlet.MIDlet;

/**
 * J2ME Display compatibility layer.
 * Manages which Displayable is currently shown.
 */
public class Display {
    private static Display instance;
    private MIDlet midlet;
    private Activity activity;
    private Displayable current;
    private CanvasView canvasView;
    
    private Display(MIDlet midlet) {
        this.midlet = midlet;
    }
    
    /**
     * Get the Display for a MIDlet.
     */
    public static Display getDisplay(MIDlet midlet) {
        if (instance == null) {
            instance = new Display(midlet);
        }
        return instance;
    }
    
    /**
     * Set the current Displayable to show.
     */
    public void setCurrent(Displayable displayable) {
        if (current != null && current instanceof Canvas) {
            ((Canvas) current).hideNotify();
        }
        
        this.current = displayable;
        displayable.setDisplay(this);
        
        if (displayable instanceof Canvas && canvasView != null) {
            Canvas canvas = (Canvas) displayable;
            canvas.setCanvasView(canvasView);
            canvasView.setCanvas(canvas);
            canvas.showNotify();
        }
    }
    
    /**
     * Set current with an alert to show first.
     */
    public void setCurrent(Alert alert, Displayable nextDisplayable) {
        // Simplified - just show the next displayable
        setCurrent(nextDisplayable);
    }
    
    /**
     * Get the current Displayable.
     */
    public Displayable getCurrent() {
        return current;
    }
    
    /**
     * Check if color is supported.
     */
    public boolean isColor() {
        return true;
    }
    
    /**
     * Get number of colors (or grays) supported.
     */
    public int numColors() {
        return 16777216; // 24-bit color
    }
    
    /**
     * Get number of alpha levels supported.
     */
    public int numAlphaLevels() {
        return 256;
    }
    
    /**
     * Get best image width for a given type.
     */
    public int getBestImageWidth(int imageType) {
        return canvasView != null ? canvasView.getWidth() : 240;
    }
    
    /**
     * Get best image height for a given type.
     */
    public int getBestImageHeight(int imageType) {
        return canvasView != null ? canvasView.getHeight() : 320;
    }
    
    /**
     * Flash the backlight.
     */
    public boolean flashBacklight(int duration) {
        // Not implemented on modern devices
        return false;
    }
    
    /**
     * Vibrate the device.
     */
    public boolean vibrate(int duration) {
        // Could be implemented with Vibrator service
        return false;
    }
    
    // Internal methods
    public void setActivity(Activity activity) {
        this.activity = activity;
    }
    
    public Activity getActivity() {
        return activity;
    }
    
    public void setCanvasView(CanvasView view) {
        this.canvasView = view;
    }
    
    public CanvasView getCanvasView() {
        return canvasView;
    }
    
    public static void reset() {
        instance = null;
    }
}
