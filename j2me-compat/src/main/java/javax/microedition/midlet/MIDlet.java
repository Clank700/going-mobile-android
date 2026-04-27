package javax.microedition.midlet;

import android.app.Activity;

import javax.microedition.lcdui.Display;

/**
 * J2ME MIDlet compatibility layer.
 * Base class that games extend, provides lifecycle methods.
 */
public abstract class MIDlet {
    private Activity activity;
    private boolean destroyed = false;
    
    protected MIDlet() {
    }
    
    /**
     * Called when the MIDlet should start execution.
     */
    protected abstract void startApp() throws MIDletStateChangeException;
    
    /**
     * Called when the MIDlet should pause.
     */
    protected abstract void pauseApp();
    
    /**
     * Called when the MIDlet should be destroyed.
     * @param unconditional If true, the MIDlet must cleanup and exit.
     */
    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;
    
    /**
     * Request that the MIDlet be put in paused state.
     */
    public final void notifyPaused() {
        pauseApp();
    }
    
    /**
     * Request that the MIDlet be destroyed.
     */
    public final void notifyDestroyed() {
        if (!destroyed) {
            destroyed = true;
            try {
                destroyApp(true);
            } catch (MIDletStateChangeException e) {
                // Ignore - unconditional destroy
            }
            if (activity != null) {
                activity.finish();
            }
        }
    }
    
    /**
     * Get an application property from the JAD file.
     * For Android, we return null or can map to AndroidManifest metadata.
     */
    public final String getAppProperty(String key) {
        // Common J2ME properties - return reasonable defaults
        switch (key) {
            case "MIDlet-Name":
                return activity != null ? activity.getTitle().toString() : "Game";
            case "MIDlet-Version":
                return "1.0";
            case "MIDlet-Vendor":
                return "Unknown";
            default:
                return null;
        }
    }
    
    /**
     * Request to start the MIDlet from paused state.
     */
    public final void resumeRequest() {
        try {
            startApp();
        } catch (MIDletStateChangeException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Check if the MIDlet is paused.
     */
    public boolean isPaused() {
        return false; // Simplified
    }
    
    /**
     * Platform request (open URL, make call, etc.)
     * Returns true if the MIDlet must exit to handle the request.
     */
    public final boolean platformRequest(String url) throws Exception {
        // Could implement to open URL in browser
        return false;
    }
    
    /**
     * Check for pending platform request.
     */
    public final int checkPermission(String permission) {
        return 1; // ALLOWED
    }
    
    // Internal methods for Android integration
    public void setActivity(Activity activity) {
        this.activity = activity;
        Display display = Display.getDisplay(this);
        display.setActivity(activity);
    }
    
    public Activity getActivity() {
        return activity;
    }
    
    /**
     * Called by Android Activity to start the MIDlet.
     */
    public void onStart() {
        try {
            startApp();
        } catch (MIDletStateChangeException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Called by Android Activity when pausing.
     */
    public void onPause() {
        pauseApp();
    }
    
    /**
     * Called by Android Activity when resuming.
     */
    public void onResume() {
        try {
            startApp();
        } catch (MIDletStateChangeException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Called by Android Activity when destroying.
     */
    public void onDestroy() {
        notifyDestroyed();
    }
}
