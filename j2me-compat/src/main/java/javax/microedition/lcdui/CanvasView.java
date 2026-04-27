package javax.microedition.lcdui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Android SurfaceView that hosts a J2ME Canvas.
 * Handles rendering, input translation, and on-screen controls.
 */
public class CanvasView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static Context appContext;

    private Canvas canvas;
    private SurfaceHolder holder;
    private Thread renderThread;
    private volatile boolean running = false;
    private volatile boolean repaintRequested = true;

    // Double buffering - guarded by paintLock
    private Bitmap backBuffer;
    private android.graphics.Canvas backBufferCanvas;

    // Synchronization: protects backBuffer read/write and surface blitting
    private final Object paintLock = new Object();

    // Frame rate
    private static final int TARGET_FPS = 30;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

    // Game scaling
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private int offsetX = 0;
    private int offsetY = 0;
    private int gameWidth = 240;
    private int gameHeight = 320;

    // On-screen control buttons (positions in screen coordinates)
    private boolean controlsReady = false;
    private RectF btnUp, btnDown, btnLeft, btnRight;
    private RectF btnFire, btnMenu, btnBack;
    private RectF btnWeapon, btnStar, btnJumpLeft, btnJumpRight, btnGadget;
    private Paint btnFillPaint, btnOutlinePaint, btnTextPaint, btnPressedPaint;

    // Multi-touch: pointer ID -> J2ME key code (guarded by pointerLock)
    private final SparseIntArray pointerKeys = new SparseIntArray();
    private final Object pointerLock = new Object();

    // Key event queue: delivers key events on the render thread to avoid ANR
    // (GameCanvas.keyPressed is synchronized and can block the UI thread)
    private static final int EVT_PRESSED = 1;
    private static final int EVT_RELEASED = 2;
    private static final int EVT_REPEATED = 3;
    private final ConcurrentLinkedQueue<int[]> keyEventQueue = new ConcurrentLinkedQueue<>();

    public CanvasView(Context context) {
        super(context);
        appContext = context.getApplicationContext();
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        initPaints();
    }

    private void initPaints() {
        btnFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnFillPaint.setColor(0x40FFFFFF);
        btnFillPaint.setStyle(Paint.Style.FILL);

        btnOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnOutlinePaint.setColor(0xBBFFFFFF);
        btnOutlinePaint.setStyle(Paint.Style.STROKE);
        btnOutlinePaint.setStrokeWidth(3);

        btnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnTextPaint.setColor(0xFFFFFFFF);
        btnTextPaint.setTextAlign(Paint.Align.CENTER);

        btnPressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnPressedPaint.setColor(0x80FFFFFF);
        btnPressedPaint.setStyle(Paint.Style.FILL);
    }

    // =========================================================================
    // Static helpers
    // =========================================================================

    public static Context getAppContext() {
        return appContext;
    }

    public static void setAppContext(Context context) {
        appContext = context.getApplicationContext();
    }

    /**
     * Open a resource from Android assets, replacing J2ME's getResourceAsStream().
     * Strips leading "/" from path since Android assets don't use it.
     */
    public static InputStream openResource(String path) {
        try {
            String assetPath = path.startsWith("/") ? path.substring(1) : path;
            Context context = getAppContext();
            if (context == null) return null;
            return context.getAssets().open(assetPath);
        } catch (IOException e) {
            android.util.Log.w("J2ME", "Resource not found: " + path);
            return null;
        }
    }

    // =========================================================================
    // Canvas management
    // =========================================================================

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
        if (canvas != null) {
            canvas.setCanvasView(this);
            canvas.setDimensions(gameWidth, gameHeight);
        }
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setGameDimensions(int width, int height) {
        this.gameWidth = width;
        this.gameHeight = height;

        // Set static defaults so any Canvas created hereafter gets correct dimensions
        Canvas.setDefaultDimensions(width, height);

        synchronized (paintLock) {
            backBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            backBufferCanvas = new android.graphics.Canvas(backBuffer);
        }
        if (canvas != null) {
            canvas.setDimensions(width, height);
        }
        updateScaling();
    }

    // =========================================================================
    // Scaling and button layout
    // =========================================================================

    private void updateScaling() {
        int viewW = getWidth();
        int viewH = getHeight();
        if (viewW == 0 || viewH == 0) return;

        // Game occupies the top ~55% of the screen, controls go below
        float gameAreaH = viewH * 0.55f;
        float scale = Math.min((float) viewW / gameWidth, gameAreaH / gameHeight);
        scaleX = scale;
        scaleY = scale;

        float gamePixW = gameWidth * scale;
        float gamePixH = gameHeight * scale;
        offsetX = (int) ((viewW - gamePixW) / 2);
        offsetY = (int) (viewH * 0.01f);

        // --- Control area below the game ---
        float ctrlTop = offsetY + gamePixH + viewH * 0.015f;
        float ctrlBottom = viewH - viewH * 0.02f;
        float ctrlH = ctrlBottom - ctrlTop;
        if (ctrlH < 10) return;

        float unit = Math.min(viewW / 9f, ctrlH / 4.5f);

        // D-pad: left quarter of screen
        float dpadCX = viewW * 0.22f;
        float dpadCY = ctrlTop + ctrlH * 0.58f;
        float btnSz = unit * 1.3f;
        float half = btnSz / 2;
        float gap = unit * 0.12f;

        btnUp = new RectF(dpadCX - half, dpadCY - half - gap - btnSz,
                          dpadCX + half, dpadCY - half - gap);
        btnDown = new RectF(dpadCX - half, dpadCY + half + gap,
                            dpadCX + half, dpadCY + half + gap + btnSz);
        btnLeft = new RectF(dpadCX - half - gap - btnSz, dpadCY - half,
                            dpadCX - half - gap, dpadCY + half);
        btnRight = new RectF(dpadCX + half + gap, dpadCY - half,
                             dpadCX + half + gap + btnSz, dpadCY + half);

        // Fire button: right side, large circle
        float fireCX = viewW * 0.78f;
        float fireCY = dpadCY;
        float fireR = unit * 1.4f;
        btnFire = new RectF(fireCX - fireR, fireCY - fireR,
                            fireCX + fireR, fireCY + fireR);

        // Menu and Back: smaller buttons at top of control area
        float smW = unit * 2.2f;
        float smH = unit * 0.9f;
        btnMenu = new RectF(viewW * 0.05f, ctrlTop,
                            viewW * 0.05f + smW, ctrlTop + smH);
        btnBack = new RectF(viewW * 0.95f - smW, ctrlTop,
                            viewW * 0.95f, ctrlTop + smH);

        // Weapon button: above-right of FIRE
        float smallW = unit * 1.5f;
        float smallH = unit * 0.85f;
        btnWeapon = new RectF(fireCX, fireCY - fireR - gap - smallH,
                              fireCX + smallW, fireCY - fireR - gap);

        // Star button: above-left of FIRE
        btnStar = new RectF(fireCX - smallW - gap, fireCY - fireR - gap - smallH,
                            fireCX - gap, fireCY - fireR - gap);

        // Gadget button: below FIRE
        btnGadget = new RectF(fireCX - smallW / 2, fireCY + fireR + gap,
                              fireCX + smallW / 2, fireCY + fireR + gap + smallH);

        // Jump left: diagonal top-left of D-pad
        float jBtnSz = btnSz * 0.85f;
        float jHalf = jBtnSz / 2;
        float jlCX = dpadCX - half - gap - jHalf;
        float jlCY = dpadCY - half - gap - jHalf;
        btnJumpLeft = new RectF(jlCX - jHalf, jlCY - jHalf,
                                jlCX + jHalf, jlCY + jHalf);

        // Jump right: diagonal top-right of D-pad
        float jrCX = dpadCX + half + gap + jHalf;
        float jrCY = dpadCY - half - gap - jHalf;
        btnJumpRight = new RectF(jrCX - jHalf, jrCY - jHalf,
                                 jrCX + jHalf, jrCY + jHalf);

        btnTextPaint.setTextSize(unit * 0.55f);
        btnOutlinePaint.setStrokeWidth(Math.max(2f, unit * 0.05f));

        controlsReady = true;
    }

    // =========================================================================
    // Repaint
    // =========================================================================

    public void requestRepaint() {
        repaintRequested = true;
    }

    /**
     * Synchronous repaint called from the game thread.
     * Paints to backBuffer then blits to screen.
     */
    public void serviceRepaints() {
        if (canvas == null || backBufferCanvas == null) return;
        synchronized (paintLock) {
            Graphics g = new Graphics(backBufferCanvas);
            canvas.paint(g);
            repaintRequested = false;
        }
        blitToScreen();
    }

    private void blitToScreen() {
        android.graphics.Canvas surfaceCanvas = holder.lockCanvas();
        if (surfaceCanvas != null) {
            try {
                surfaceCanvas.drawColor(0xFF000000);
                synchronized (paintLock) {
                    if (backBuffer != null) {
                        android.graphics.Matrix matrix = new android.graphics.Matrix();
                        matrix.postScale(scaleX, scaleY);
                        matrix.postTranslate(offsetX, offsetY);
                        surfaceCanvas.drawBitmap(backBuffer, matrix, null);
                    }
                }
                drawControls(surfaceCanvas);
            } finally {
                holder.unlockCanvasAndPost(surfaceCanvas);
            }
        }
    }

    // =========================================================================
    // Control drawing
    // =========================================================================

    private void drawControls(android.graphics.Canvas c) {
        if (!controlsReady) return;

        boolean upP = isKeyPressed(50);
        boolean downP = isKeyPressed(56);
        boolean leftP = isKeyPressed(52);
        boolean rightP = isKeyPressed(54);
        boolean fireP = isKeyPressed(53);
        boolean menuP = isKeyPressed(-6);
        boolean backP = isKeyPressed(-7);

        drawRoundButton(c, btnUp, "\u25B2", upP);
        drawRoundButton(c, btnDown, "\u25BC", downP);
        drawRoundButton(c, btnLeft, "\u25C4", leftP);
        drawRoundButton(c, btnRight, "\u25BA", rightP);
        drawCircleButton(c, btnFire, "FIRE", fireP);
        drawRoundButton(c, btnMenu, "MENU", menuP);
        drawRoundButton(c, btnBack, "BACK", backP);

        boolean weaponP = isKeyPressed(35);
        boolean starP = isKeyPressed(42);
        boolean jumpLP = isKeyPressed(49);
        boolean jumpRP = isKeyPressed(51);
        boolean gadgetP = isKeyPressed(55);

        drawRoundButton(c, btnWeapon, "WPN", weaponP);
        drawRoundButton(c, btnStar, "\u2605", starP);
        drawRoundButton(c, btnJumpLeft, "\u2196", jumpLP);
        drawRoundButton(c, btnJumpRight, "\u2197", jumpRP);
        drawRoundButton(c, btnGadget, "GAD", gadgetP);
    }

    private boolean isKeyPressed(int keyCode) {
        synchronized (pointerLock) {
            for (int i = 0; i < pointerKeys.size(); i++) {
                if (pointerKeys.valueAt(i) == keyCode) return true;
            }
        }
        return false;
    }

    private void drawRoundButton(android.graphics.Canvas c, RectF r, String label, boolean pressed) {
        float rad = Math.min(r.width(), r.height()) * 0.25f;
        c.drawRoundRect(r, rad, rad, pressed ? btnPressedPaint : btnFillPaint);
        c.drawRoundRect(r, rad, rad, btnOutlinePaint);
        Paint.FontMetrics fm = btnTextPaint.getFontMetrics();
        c.drawText(label, r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2, btnTextPaint);
    }

    private void drawCircleButton(android.graphics.Canvas c, RectF r, String label, boolean pressed) {
        float cx = r.centerX(), cy = r.centerY();
        float rad = Math.min(r.width(), r.height()) / 2;
        c.drawCircle(cx, cy, rad, pressed ? btnPressedPaint : btnFillPaint);
        c.drawCircle(cx, cy, rad, btnOutlinePaint);
        Paint.FontMetrics fm = btnTextPaint.getFontMetrics();
        c.drawText(label, cx, cy - (fm.ascent + fm.descent) / 2, btnTextPaint);
    }

    // =========================================================================
    // Surface callbacks
    // =========================================================================

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (backBuffer == null) {
            setGameDimensions(gameWidth, gameHeight);
        }
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        updateScaling();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    public void start() {
        if (!running) {
            running = true;
            renderThread = new Thread(this, "J2ME-RenderThread");
            renderThread.start();
        }
    }

    public void stop() {
        running = false;
        if (renderThread != null) {
            try {
                renderThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            renderThread = null;
        }
    }

    // =========================================================================
    // Render loop
    // =========================================================================

    @Override
    public void run() {
        while (running) {
            long now = System.currentTimeMillis();

            // Deliver queued key events (from UI thread touch input)
            processKeyQueue();

            // Paint game content if requested
            if (repaintRequested && canvas != null && backBufferCanvas != null) {
                synchronized (paintLock) {
                    if (repaintRequested) {
                        repaintRequested = false;
                        Graphics g = new Graphics(backBufferCanvas);
                        canvas.paint(g);
                    }
                }
            }

            // Blit game + controls to screen
            blitToScreen();

            long elapsed = System.currentTimeMillis() - now;
            long sleepTime = FRAME_TIME - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // =========================================================================
    // Key event queue (prevents ANR from blocking UI thread)
    // =========================================================================

    private void processKeyQueue() {
        int[] evt;
        while ((evt = keyEventQueue.poll()) != null) {
            if (canvas == null) continue;
            switch (evt[0]) {
                case EVT_PRESSED:  canvas.keyPressed(evt[1]);  break;
                case EVT_RELEASED: canvas.keyReleased(evt[1]); break;
                case EVT_REPEATED: canvas.keyRepeated(evt[1]); break;
            }
        }
    }

    private void queueKeyPress(int keyCode) {
        keyEventQueue.add(new int[]{EVT_PRESSED, keyCode});
    }

    private void queueKeyRelease(int keyCode) {
        keyEventQueue.add(new int[]{EVT_RELEASED, keyCode});
    }

    // =========================================================================
    // Touch input (multi-touch, queued to render thread)
    // =========================================================================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (canvas == null) return false;

        int action = event.getActionMasked();
        int ptrIdx = event.getActionIndex();
        int ptrId = event.getPointerId(ptrIdx);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int key = screenToKey(event.getX(ptrIdx), event.getY(ptrIdx));
                if (key != 0) {
                    synchronized (pointerLock) {
                        pointerKeys.put(ptrId, key);
                    }
                    queueKeyPress(key);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pid = event.getPointerId(i);
                    int newKey = screenToKey(event.getX(i), event.getY(i));
                    int oldKey;
                    synchronized (pointerLock) {
                        oldKey = pointerKeys.get(pid, 0);
                    }
                    if (newKey != oldKey) {
                        if (oldKey != 0) {
                            queueKeyRelease(oldKey);
                        }
                        synchronized (pointerLock) {
                            if (newKey != 0) {
                                pointerKeys.put(pid, newKey);
                            } else {
                                pointerKeys.delete(pid);
                            }
                        }
                        if (newKey != 0) {
                            queueKeyPress(newKey);
                        }
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int oldKey;
                synchronized (pointerLock) {
                    oldKey = pointerKeys.get(ptrId, 0);
                    pointerKeys.delete(ptrId);
                }
                if (oldKey != 0) {
                    queueKeyRelease(oldKey);
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                synchronized (pointerLock) {
                    for (int i = 0; i < pointerKeys.size(); i++) {
                        int key = pointerKeys.valueAt(i);
                        if (key != 0) queueKeyRelease(key);
                    }
                    pointerKeys.clear();
                }
                break;
            }
        }
        return true;
    }

    /**
     * Map screen coordinates to a J2ME key code.
     * Checks on-screen buttons first, then falls back to game area 3x3 grid.
     */
    private int screenToKey(float sx, float sy) {
        if (controlsReady) {
            if (btnUp.contains(sx, sy))    return 50;  // KEY_NUM2 -> UP
            if (btnDown.contains(sx, sy))  return 56;  // KEY_NUM8 -> DOWN
            if (btnLeft.contains(sx, sy))  return 52;  // KEY_NUM4 -> LEFT
            if (btnRight.contains(sx, sy)) return 54;  // KEY_NUM6 -> RIGHT
            if (btnFire.contains(sx, sy))  return 53;  // KEY_NUM5 -> FIRE
            if (btnMenu.contains(sx, sy))  return -6;  // Left soft key (MENU)
            if (btnBack.contains(sx, sy))  return -7;  // Right soft key (BACK)
            if (btnWeapon != null && btnWeapon.contains(sx, sy))       return 35;  // KEY_POUND -> WEAPON
            if (btnStar != null && btnStar.contains(sx, sy))           return 42;  // KEY_STAR
            if (btnJumpLeft != null && btnJumpLeft.contains(sx, sy))   return 49;  // KEY_NUM1 -> JUMP LEFT
            if (btnJumpRight != null && btnJumpRight.contains(sx, sy)) return 51;  // KEY_NUM3 -> JUMP RIGHT
            if (btnGadget != null && btnGadget.contains(sx, sy))       return 55;  // KEY_NUM7 -> GADGET
        }

        // Fallback: touch inside game area uses 3x3 grid
        float gx = (sx - offsetX) / scaleX;
        float gy = (sy - offsetY) / scaleY;
        if (gx >= 0 && gx < gameWidth && gy >= 0 && gy < gameHeight) {
            return touchToKey((int) gx, (int) gy);
        }

        return 0;
    }

    private int touchToKey(int gameX, int gameY) {
        float xRatio = (float) gameX / gameWidth;
        float yRatio = (float) gameY / gameHeight;

        if (yRatio < 0.33f) {
            if (xRatio < 0.33f) return -7;
            if (xRatio > 0.66f) return -6;
            return 50;
        } else if (yRatio > 0.66f) {
            if (xRatio < 0.4f)  return 52;
            if (xRatio > 0.6f)  return 54;
            return 56;
        } else {
            if (xRatio < 0.33f) return 52;
            if (xRatio > 0.66f) return 54;
            return 53;
        }
    }

    // =========================================================================
    // Physical keyboard (also queued to render thread)
    // =========================================================================

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (canvas == null) return false;
        int j2meKey = Canvas.androidKeyToJ2ME(keyCode);
        if (event.getRepeatCount() > 0) {
            keyEventQueue.add(new int[]{EVT_REPEATED, j2meKey});
        } else {
            queueKeyPress(j2meKey);
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (canvas == null) return false;
        queueKeyRelease(Canvas.androidKeyToJ2ME(keyCode));
        return true;
    }
}
