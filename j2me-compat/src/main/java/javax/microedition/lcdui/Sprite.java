package javax.microedition.lcdui;

/**
 * J2ME Sprite transform constants.
 * Used by Graphics.drawRegion and Image.createImage with transforms.
 */
public class Sprite {
    public static final int TRANS_NONE = 0;
    public static final int TRANS_ROT90 = 5;
    public static final int TRANS_ROT180 = 3;
    public static final int TRANS_ROT270 = 6;
    public static final int TRANS_MIRROR = 2;
    public static final int TRANS_MIRROR_ROT90 = 7;
    public static final int TRANS_MIRROR_ROT180 = 1;
    public static final int TRANS_MIRROR_ROT270 = 4;
    
    // Prevent instantiation
    private Sprite() {}
}
