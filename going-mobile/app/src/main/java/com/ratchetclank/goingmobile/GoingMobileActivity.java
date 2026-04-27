package com.ratchetclank.goingmobile;

import javax.microedition.lcdui.MIDletActivity;
import javax.microedition.midlet.MIDlet;

/**
 * Android Activity that hosts the Going Mobile game.
 * 
 * Going Mobile was originally released for J2ME phones in 2005.
 * Screen resolution: 176x220 (original), scaled up for modern displays.
 */
public class GoingMobileActivity extends MIDletActivity {
    
    @Override
    protected MIDlet createMIDlet() {
        return new ratchetandclank();
    }
    
    @Override
    protected int getGameWidth() {
        return 176; // Original J2ME screen width
    }
    
    @Override
    protected int getGameHeight() {
        return 220; // Original J2ME screen height
    }
}
