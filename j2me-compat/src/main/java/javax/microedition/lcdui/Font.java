package javax.microedition.lcdui;

import android.graphics.Typeface;

/**
 * J2ME Font compatibility layer for Android.
 */
public class Font {
    // Face constants
    public static final int FACE_SYSTEM = 0;
    public static final int FACE_MONOSPACE = 32;
    public static final int FACE_PROPORTIONAL = 64;
    
    // Style constants
    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_UNDERLINED = 4;
    
    // Size constants
    public static final int SIZE_SMALL = 8;
    public static final int SIZE_MEDIUM = 0;
    public static final int SIZE_LARGE = 16;
    
    // Font specifiers
    public static final int FONT_STATIC_TEXT = 0;
    public static final int FONT_INPUT_TEXT = 1;
    
    private int face;
    private int style;
    private int size;
    private Typeface typeface;
    private int height;
    private android.graphics.Paint measurePaint;
    
    private Font(int face, int style, int size) {
        this.face = face;
        this.style = style;
        this.size = size;
        
        // Map J2ME face to Android typeface
        Typeface baseTypeface;
        switch (face) {
            case FACE_MONOSPACE:
                baseTypeface = Typeface.MONOSPACE;
                break;
            case FACE_PROPORTIONAL:
            case FACE_SYSTEM:
            default:
                baseTypeface = Typeface.SANS_SERIF;
                break;
        }
        
        // Apply style
        int androidStyle = Typeface.NORMAL;
        if ((style & STYLE_BOLD) != 0 && (style & STYLE_ITALIC) != 0) {
            androidStyle = Typeface.BOLD_ITALIC;
        } else if ((style & STYLE_BOLD) != 0) {
            androidStyle = Typeface.BOLD;
        } else if ((style & STYLE_ITALIC) != 0) {
            androidStyle = Typeface.ITALIC;
        }
        
        this.typeface = Typeface.create(baseTypeface, androidStyle);
        
        // Map J2ME size to pixel height
        switch (size) {
            case SIZE_SMALL:
                this.height = 12;
                break;
            case SIZE_LARGE:
                this.height = 20;
                break;
            case SIZE_MEDIUM:
            default:
                this.height = 16;
                break;
        }
        
        // Create paint for measuring
        measurePaint = new android.graphics.Paint();
        measurePaint.setTypeface(typeface);
        measurePaint.setTextSize(height);
    }
    
    public static Font getFont(int face, int style, int size) {
        return new Font(face, style, size);
    }
    
    public static Font getDefaultFont() {
        return new Font(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
    }
    
    public static Font getFont(int specifier) {
        return getDefaultFont();
    }
    
    public int getStyle() { return style; }
    public int getSize() { return size; }
    public int getFace() { return face; }
    public int getHeight() { return height; }
    
    public int getBaselinePosition() {
        android.graphics.Paint.FontMetrics fm = measurePaint.getFontMetrics();
        return (int) -fm.top;
    }
    
    public boolean isPlain() { return style == STYLE_PLAIN; }
    public boolean isBold() { return (style & STYLE_BOLD) != 0; }
    public boolean isItalic() { return (style & STYLE_ITALIC) != 0; }
    public boolean isUnderlined() { return (style & STYLE_UNDERLINED) != 0; }
    
    public int charWidth(char ch) {
        return (int) measurePaint.measureText(String.valueOf(ch));
    }
    
    public int charsWidth(char[] ch, int offset, int length) {
        return (int) measurePaint.measureText(ch, offset, length);
    }
    
    public int stringWidth(String str) {
        return (int) measurePaint.measureText(str);
    }
    
    public int substringWidth(String str, int offset, int len) {
        return (int) measurePaint.measureText(str, offset, offset + len);
    }
    
    public Typeface getTypeface() {
        return typeface;
    }
}
