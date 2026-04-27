package javax.microedition.lcdui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * J2ME Image compatibility layer for Android.
 * Wraps Android Bitmap to provide J2ME-compatible Image API.
 */
public class Image {
    private Bitmap bitmap;
    private boolean mutable;
    
    private Image(Bitmap bitmap, boolean mutable) {
        this.bitmap = bitmap;
        this.mutable = mutable;
    }
    
    /**
     * Creates an immutable image from a resource path.
     * In J2ME, resources are loaded from the JAR.
     * For Android, we load from assets.
     */
    public static Image createImage(String name) throws IOException {
        // Remove leading slash if present
        String assetPath = name.startsWith("/") ? name.substring(1) : name;
        
        Context context = CanvasView.getAppContext();
        if (context == null) {
            throw new IOException("Context not initialized");
        }
        
        try {
            InputStream is = context.getAssets().open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            
            if (bitmap == null) {
                throw new IOException("Failed to decode image: " + name);
            }
            
            return new Image(bitmap, false);
        } catch (IOException e) {
            throw new IOException("Failed to load image: " + name, e);
        }
    }
    
    /**
     * Creates an immutable image from byte array.
     */
    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, imageOffset, imageLength);
        return new Image(bitmap, false);
    }
    
    /**
     * Creates a mutable image with specified dimensions.
     */
    public static Image createImage(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        return new Image(bitmap, true);
    }
    
    /**
     * Creates an immutable copy of an existing image.
     */
    public static Image createImage(Image source) {
        Bitmap copy = source.bitmap.copy(Bitmap.Config.ARGB_8888, false);
        return new Image(copy, false);
    }
    
    /**
     * Creates an immutable image from a region of another image.
     */
    public static Image createImage(Image image, int x, int y, int width, int height, int transform) {
        Bitmap region = Bitmap.createBitmap(image.bitmap, x, y, width, height);
        
        // Apply transform
        if (transform != Sprite.TRANS_NONE) {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            switch (transform) {
                case Sprite.TRANS_ROT90:
                    matrix.postRotate(90);
                    break;
                case Sprite.TRANS_ROT180:
                    matrix.postRotate(180);
                    break;
                case Sprite.TRANS_ROT270:
                    matrix.postRotate(270);
                    break;
                case Sprite.TRANS_MIRROR:
                    matrix.preScale(-1, 1);
                    break;
                case Sprite.TRANS_MIRROR_ROT90:
                    matrix.preScale(-1, 1);
                    matrix.postRotate(90);
                    break;
                case Sprite.TRANS_MIRROR_ROT180:
                    matrix.preScale(-1, 1);
                    matrix.postRotate(180);
                    break;
                case Sprite.TRANS_MIRROR_ROT270:
                    matrix.preScale(-1, 1);
                    matrix.postRotate(270);
                    break;
            }
            region = Bitmap.createBitmap(region, 0, 0, region.getWidth(), region.getHeight(), matrix, true);
        }
        
        return new Image(region, false);
    }
    
    /**
     * Creates an immutable image from an InputStream.
     */
    public static Image createImage(InputStream stream) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        if (bitmap == null) {
            throw new IOException("Failed to decode image from stream");
        }
        return new Image(bitmap, false);
    }
    
    /**
     * Creates an RGB image from pixel data.
     */
    public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
        Bitmap.Config config = processAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        
        if (!processAlpha) {
            // Make all pixels opaque
            for (int i = 0; i < rgb.length; i++) {
                rgb[i] = rgb[i] | 0xFF000000;
            }
        }
        
        bitmap.setPixels(rgb, 0, width, 0, 0, width, height);
        return new Image(bitmap, false);
    }
    
    public int getWidth() {
        return bitmap.getWidth();
    }
    
    public int getHeight() {
        return bitmap.getHeight();
    }
    
    public boolean isMutable() {
        return mutable;
    }
    
    public Graphics getGraphics() {
        if (!mutable) {
            throw new IllegalStateException("Image is immutable");
        }
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        return new Graphics(canvas);
    }
    
    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
        bitmap.getPixels(rgbData, offset, scanlength, x, y, width, height);
    }
    
    // Internal access to Android Bitmap
    public Bitmap getBitmap() {
        return bitmap;
    }
}
