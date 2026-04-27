package javax.microedition.lcdui;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Base Android Activity that hosts a MIDlet.
 * Games should extend this and provide their MIDlet class.
 */
public abstract class MIDletActivity extends Activity {
    private MIDlet midlet;
    private CanvasView canvasView;
    private Display display;
    
    /**
     * Subclasses must implement this to create their MIDlet.
     */
    protected abstract MIDlet createMIDlet();
    
    /**
     * Override to set game dimensions (default is 240x320).
     */
    protected int getGameWidth() {
        return 240;
    }
    
    protected int getGameHeight() {
        return 320;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen, no title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Allow rendering into display cutout areas (notch, camera hole)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // Initialize context for resource loading
        CanvasView.setAppContext(this);

        // Create canvas view
        canvasView = new CanvasView(this);
        canvasView.setGameDimensions(getGameWidth(), getGameHeight());
        setContentView(canvasView);

        // Immersive fullscreen mode (must be after setContentView - DecorView needs to exist)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        // Reset display singleton
        Display.reset();

        // Create MIDlet
        midlet = createMIDlet();
        midlet.setActivity(this);

        // Connect display to canvas view
        display = Display.getDisplay(midlet);
        display.setActivity(this);
        display.setCanvasView(canvasView);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Re-apply immersive mode when window regains focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        if (midlet != null) {
            midlet.onStart();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        canvasView.start();
        if (midlet != null) {
            midlet.onResume();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        canvasView.stop();
        if (midlet != null) {
            midlet.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (midlet != null) {
            midlet.onDestroy();
        }
        super.onDestroy();
    }
    
    /**
     * Get the canvas view.
     */
    public CanvasView getCanvasView() {
        return canvasView;
    }
    
    /**
     * Get the MIDlet.
     */
    public MIDlet getMIDlet() {
        return midlet;
    }
}
