package javax.microedition.lcdui;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

/**
 * J2ME Graphics compatibility layer for Android.
 * Wraps Android Canvas to provide J2ME-compatible drawing API.
 */
public class Graphics {
    // Anchor constants
    public static final int TOP = 1;
    public static final int BOTTOM = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int HCENTER = 16;
    public static final int VCENTER = 32;
    public static final int BASELINE = 64;
    
    // Style constants
    public static final int SOLID = 0;
    public static final int DOTTED = 1;
    
    private android.graphics.Canvas canvas;
    private Paint paint;
    private Paint textPaint;
    private int translateX = 0;
    private int translateY = 0;
    private int clipX, clipY, clipWidth, clipHeight;
    private int color = 0xFF000000;
    private Font font;
    private int initialSaveCount;

    public Graphics(android.graphics.Canvas canvas) {
        this.canvas = canvas;
        this.paint = new Paint();
        this.paint.setAntiAlias(false);
        this.textPaint = new Paint();
        this.textPaint.setAntiAlias(true);
        this.font = Font.getDefaultFont();

        // Reset any leftover clip from previous paint calls, then save clean state
        canvas.restoreToCount(1);
        initialSaveCount = canvas.save();

        // Initialize clip to canvas bounds
        clipX = 0;
        clipY = 0;
        clipWidth = canvas.getWidth();
        clipHeight = canvas.getHeight();
    }
    
    public void setColor(int red, int green, int blue) {
        this.color = 0xFF000000 | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
        paint.setColor(this.color);
        textPaint.setColor(this.color);
    }
    
    public void setColor(int color) {
        this.color = 0xFF000000 | (color & 0x00FFFFFF);
        paint.setColor(this.color);
        textPaint.setColor(this.color);
    }
    
    public int getColor() {
        return color & 0x00FFFFFF;
    }
    
    public int getRedComponent() {
        return (color >> 16) & 0xFF;
    }
    
    public int getGreenComponent() {
        return (color >> 8) & 0xFF;
    }
    
    public int getBlueComponent() {
        return color & 0xFF;
    }
    
    public void fillRect(int x, int y, int width, int height) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(x + translateX, y + translateY, 
                       x + translateX + width, y + translateY + height, paint);
    }
    
    public void drawRect(int x, int y, int width, int height) {
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(x + translateX, y + translateY,
                       x + translateX + width, y + translateY + height, paint);
    }
    
    public void drawLine(int x1, int y1, int x2, int y2) {
        canvas.drawLine(x1 + translateX, y1 + translateY,
                       x2 + translateX, y2 + translateY, paint);
    }
    
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(x + translateX, y + translateY,
                      x + translateX + width, y + translateY + height,
                      -startAngle, -arcAngle, false, paint);
    }
    
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawArc(x + translateX, y + translateY,
                      x + translateX + width, y + translateY + height,
                      -startAngle, -arcAngle, true, paint);
    }
    
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(x + translateX, y + translateY,
                            x + translateX + width, y + translateY + height,
                            arcWidth / 2f, arcHeight / 2f, paint);
    }
    
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(x + translateX, y + translateY,
                            x + translateX + width, y + translateY + height,
                            arcWidth / 2f, arcHeight / 2f, paint);
    }
    
    public void drawString(String str, int x, int y, int anchor) {
        if (str == null) return;
        
        textPaint.setTextSize(font.getHeight());
        
        float drawX = x + translateX;
        float drawY = y + translateY;
        
        // Horizontal anchor
        if ((anchor & HCENTER) != 0) {
            drawX -= textPaint.measureText(str) / 2;
        } else if ((anchor & RIGHT) != 0) {
            drawX -= textPaint.measureText(str);
        }
        
        // Vertical anchor - J2ME uses baseline by default
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        if ((anchor & TOP) != 0) {
            drawY -= fm.top;
        } else if ((anchor & BOTTOM) != 0) {
            drawY -= fm.bottom;
        } else if ((anchor & VCENTER) != 0) {
            drawY -= (fm.top + fm.bottom) / 2;
        }
        // BASELINE is default - no adjustment needed
        
        canvas.drawText(str, drawX, drawY, textPaint);
    }
    
    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
        drawString(str.substring(offset, offset + len), x, y, anchor);
    }
    
    public void drawChar(char character, int x, int y, int anchor) {
        drawString(String.valueOf(character), x, y, anchor);
    }
    
    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        drawString(new String(data, offset, length), x, y, anchor);
    }
    
    public void drawImage(Image img, int x, int y, int anchor) {
        if (img == null) return;
        
        Bitmap bitmap = img.getBitmap();
        int drawX = x + translateX;
        int drawY = y + translateY;
        
        // Horizontal anchor
        if ((anchor & HCENTER) != 0) {
            drawX -= bitmap.getWidth() / 2;
        } else if ((anchor & RIGHT) != 0) {
            drawX -= bitmap.getWidth();
        }
        
        // Vertical anchor
        if ((anchor & VCENTER) != 0) {
            drawY -= bitmap.getHeight() / 2;
        } else if ((anchor & BOTTOM) != 0) {
            drawY -= bitmap.getHeight();
        }
        
        canvas.drawBitmap(bitmap, drawX, drawY, paint);
    }
    
    public void drawRegion(Image src, int x_src, int y_src, int width, int height,
                          int transform, int x_dest, int y_dest, int anchor) {
        if (src == null) return;
        
        Bitmap srcBitmap = src.getBitmap();
        Bitmap region = Bitmap.createBitmap(srcBitmap, x_src, y_src, width, height);
        
        // Apply transform
        region = applyTransform(region, transform);
        
        int drawX = x_dest + translateX;
        int drawY = y_dest + translateY;
        
        // Apply anchor
        if ((anchor & HCENTER) != 0) {
            drawX -= region.getWidth() / 2;
        } else if ((anchor & RIGHT) != 0) {
            drawX -= region.getWidth();
        }
        if ((anchor & VCENTER) != 0) {
            drawY -= region.getHeight() / 2;
        } else if ((anchor & BOTTOM) != 0) {
            drawY -= region.getHeight();
        }
        
        canvas.drawBitmap(region, drawX, drawY, paint);
    }
    
    private Bitmap applyTransform(Bitmap src, int transform) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        
        switch (transform) {
            case Sprite.TRANS_NONE:
                return src;
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
        
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }
    
    public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) {
        // Implementation for copying areas within the same canvas
        // This is complex and rarely used - basic implementation
    }
    
    public void setClip(int x, int y, int width, int height) {
        clipX = x;
        clipY = y;
        clipWidth = width;
        clipHeight = height;
        // J2ME setClip replaces the clip; Android clipRect intersects.
        // Restore to clean state then apply new clip.
        canvas.restoreToCount(initialSaveCount);
        initialSaveCount = canvas.save();
        canvas.clipRect(x + translateX, y + translateY,
                       x + translateX + width, y + translateY + height);
    }
    
    public void clipRect(int x, int y, int width, int height) {
        canvas.clipRect(x + translateX, y + translateY,
                       x + translateX + width, y + translateY + height);
    }
    
    public int getClipX() { return clipX; }
    public int getClipY() { return clipY; }
    public int getClipWidth() { return clipWidth; }
    public int getClipHeight() { return clipHeight; }
    
    public void translate(int x, int y) {
        translateX += x;
        translateY += y;
    }
    
    public int getTranslateX() { return translateX; }
    public int getTranslateY() { return translateY; }
    
    public void setFont(Font font) {
        this.font = font;
        if (font != null) {
            textPaint.setTextSize(font.getHeight());
            textPaint.setTypeface(font.getTypeface());
        }
    }
    
    public Font getFont() {
        return font;
    }
    
    public void setStrokeStyle(int style) {
        // SOLID or DOTTED
        if (style == DOTTED) {
            paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{2, 2}, 0));
        } else {
            paint.setPathEffect(null);
        }
    }
    
    public int getStrokeStyle() {
        return paint.getPathEffect() == null ? SOLID : DOTTED;
    }
    
    public void setGrayScale(int value) {
        setColor(value, value, value);
    }
    
    public int getGrayScale() {
        return (getRedComponent() + getGreenComponent() + getBlueComponent()) / 3;
    }
    
    public int getDisplayColor(int color) {
        return color;
    }
    
    // Internal access for advanced operations
    public android.graphics.Canvas getAndroidCanvas() {
        return canvas;
    }
}
