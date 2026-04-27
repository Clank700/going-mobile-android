package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Nokia DirectUtils compatibility.
 * Provides utility methods for Nokia-specific features.
 */
public class DirectUtils {
    
    /**
     * Get DirectGraphics from a standard Graphics object.
     */
    public static DirectGraphics getDirectGraphics(Graphics g) {
        return new DirectGraphics(g);
    }
    
    /**
     * Create a mutable image with specified color.
     */
    public static Image createImage(int width, int height, int argbColor) {
        Image img = Image.createImage(width, height);
        Graphics g = img.getGraphics();
        g.setColor(argbColor);
        g.fillRect(0, 0, width, height);
        return img;
    }
    
    /**
     * Create an image from byte data.
     */
    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        return Image.createImage(imageData, imageOffset, imageLength);
    }
}
