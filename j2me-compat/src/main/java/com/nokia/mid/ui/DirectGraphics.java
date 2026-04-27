package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Nokia DirectGraphics compatibility layer.
 * Clone Home uses Nokia-specific APIs for advanced drawing.
 * Most methods delegate to standard Graphics or provide stubs.
 */
public class DirectGraphics {
    // Manipulation constants
    public static final int FLIP_HORIZONTAL = 0x2000;
    public static final int FLIP_VERTICAL = 0x4000;
    public static final int ROTATE_90 = 90;
    public static final int ROTATE_180 = 180;
    public static final int ROTATE_270 = 270;
    
    // Type constants for native pixel format
    public static final int TYPE_BYTE_1_GRAY = 1;
    public static final int TYPE_BYTE_1_GRAY_VERTICAL = -1;
    public static final int TYPE_BYTE_2_GRAY = 2;
    public static final int TYPE_BYTE_4_GRAY = 4;
    public static final int TYPE_BYTE_8_GRAY = 8;
    public static final int TYPE_BYTE_332_RGB = 332;
    public static final int TYPE_USHORT_4444_ARGB = 4444;
    public static final int TYPE_USHORT_444_RGB = 444;
    public static final int TYPE_USHORT_555_RGB = 555;
    public static final int TYPE_USHORT_1555_ARGB = 1555;
    public static final int TYPE_USHORT_565_RGB = 565;
    public static final int TYPE_INT_888_RGB = 888;
    public static final int TYPE_INT_8888_ARGB = 8888;
    
    private Graphics graphics;
    
    public DirectGraphics(Graphics graphics) {
        this.graphics = graphics;
    }
    
    /**
     * Set the alpha component for drawing operations.
     */
    public void setARGBColor(int argbColor) {
        // Extract RGB and set (ignore alpha for now - would need Paint.setAlpha)
        graphics.setColor(argbColor);
    }
    
    /**
     * Get the current alpha color.
     */
    public int getAlphaComponent() {
        return 0xFF; // Fully opaque
    }
    
    /**
     * Get native pixel format.
     */
    public int getNativePixelFormat() {
        return TYPE_INT_8888_ARGB;
    }
    
    /**
     * Draw an image with transformation.
     */
    public void drawImage(Image img, int x, int y, int anchor, int manipulation) {
        // Apply manipulation to anchor
        int transform = 0;
        if ((manipulation & FLIP_HORIZONTAL) != 0) {
            transform = javax.microedition.lcdui.Sprite.TRANS_MIRROR;
        }
        if ((manipulation & FLIP_VERTICAL) != 0) {
            transform = javax.microedition.lcdui.Sprite.TRANS_MIRROR_ROT180;
        }
        if (manipulation == ROTATE_90) {
            transform = javax.microedition.lcdui.Sprite.TRANS_ROT90;
        } else if (manipulation == ROTATE_180) {
            transform = javax.microedition.lcdui.Sprite.TRANS_ROT180;
        } else if (manipulation == ROTATE_270) {
            transform = javax.microedition.lcdui.Sprite.TRANS_ROT270;
        }
        
        if (transform != 0) {
            graphics.drawRegion(img, 0, 0, img.getWidth(), img.getHeight(),
                              transform, x, y, anchor);
        } else {
            graphics.drawImage(img, x, y, anchor);
        }
    }
    
    /**
     * Draw pixels directly.
     */
    public void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength,
                          int x, int y, int width, int height, int manipulation, int format) {
        // Create a temporary image and draw it
        Image img = Image.createRGBImage(pixels, width, height, transparency);
        drawImage(img, x, y, Graphics.TOP | Graphics.LEFT, manipulation);
    }
    
    /**
     * Draw pixels from short array.
     */
    public void drawPixels(short[] pixels, boolean transparency, int offset, int scanlength,
                          int x, int y, int width, int height, int manipulation, int format) {
        // Convert shorts to ints
        int[] intPixels = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            intPixels[i] = shortToARGB(pixels[i], format);
        }
        drawPixels(intPixels, transparency, offset, scanlength, x, y, width, height, manipulation, TYPE_INT_8888_ARGB);
    }
    
    /**
     * Draw pixels from byte array.
     */
    public void drawPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength,
                          int x, int y, int width, int height, int manipulation, int format) {
        // Simplified implementation
    }
    
    /**
     * Get pixels from screen.
     */
    public void getPixels(int[] pixels, int offset, int scanlength,
                         int x, int y, int width, int height, int format) {
        // Would need to read from canvas - complex
    }
    
    /**
     * Get pixels as shorts.
     */
    public void getPixels(short[] pixels, int offset, int scanlength,
                         int x, int y, int width, int height, int format) {
        // Would need to read from canvas - complex
    }
    
    /**
     * Get pixels as bytes.
     */
    public void getPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength,
                         int x, int y, int width, int height, int format) {
        // Would need to read from canvas - complex
    }
    
    /**
     * Draw a filled polygon.
     */
    public void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor) {
        setARGBColor(argbColor);
        
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(xPoints[xOffset], yPoints[yOffset]);
        for (int i = 1; i < nPoints; i++) {
            path.lineTo(xPoints[xOffset + i], yPoints[yOffset + i]);
        }
        path.close();
        
        android.graphics.Canvas canvas = graphics.getAndroidCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(argbColor);
        paint.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawPath(path, paint);
    }
    
    /**
     * Draw a polygon outline.
     */
    public void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor) {
        setARGBColor(argbColor);
        
        for (int i = 0; i < nPoints - 1; i++) {
            graphics.drawLine(xPoints[xOffset + i], yPoints[yOffset + i],
                            xPoints[xOffset + i + 1], yPoints[yOffset + i + 1]);
        }
        // Close the polygon
        graphics.drawLine(xPoints[xOffset + nPoints - 1], yPoints[yOffset + nPoints - 1],
                        xPoints[xOffset], yPoints[yOffset]);
    }
    
    /**
     * Draw a filled triangle.
     */
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
        int[] xPoints = {x1, x2, x3};
        int[] yPoints = {y1, y2, y3};
        fillPolygon(xPoints, 0, yPoints, 0, 3, argbColor);
    }
    
    /**
     * Convert short pixel to ARGB based on format.
     */
    private int shortToARGB(short pixel, int format) {
        int p = pixel & 0xFFFF;
        int a, r, g, b;
        
        switch (format) {
            case TYPE_USHORT_565_RGB:
                r = ((p >> 11) & 0x1F) << 3;
                g = ((p >> 5) & 0x3F) << 2;
                b = (p & 0x1F) << 3;
                return 0xFF000000 | (r << 16) | (g << 8) | b;
                
            case TYPE_USHORT_4444_ARGB:
                a = ((p >> 12) & 0xF) << 4;
                r = ((p >> 8) & 0xF) << 4;
                g = ((p >> 4) & 0xF) << 4;
                b = (p & 0xF) << 4;
                return (a << 24) | (r << 16) | (g << 8) | b;
                
            case TYPE_USHORT_1555_ARGB:
                a = ((p >> 15) & 0x1) * 255;
                r = ((p >> 10) & 0x1F) << 3;
                g = ((p >> 5) & 0x1F) << 3;
                b = (p & 0x1F) << 3;
                return (a << 24) | (r << 16) | (g << 8) | b;
                
            default:
                return 0xFF000000 | p;
        }
    }
}
