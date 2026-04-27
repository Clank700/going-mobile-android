# Architecture

This project uses generated Java source plus an Android compatibility layer.
It is not an emulator.

## High-Level Flow

```text
Original J2ME JAR supplied by user
  |
  | tools/prepare-game.ps1
  v
Generated Android project inputs
  |-- assets copied to going-mobile/app/src/main/assets/
  |-- Java source generated in com.ratchetclank.goingmobile
  |
  | Gradle
  v
Android APK
```

The generated game code is ignored by git. The repository only stores the
repeatable pipeline and the compatibility layer needed to build it locally.

## Modules

### `going-mobile:app`

The Android application module. It contains:

- `GoingMobileActivity`, a small wrapper that creates the generated MIDlet.
- Android manifest and app resources.
- Ignored generated game source and assets after running the tool.

`GoingMobileActivity.createMIDlet()` returns `new ratchetandclank()`, the
original J2ME MIDlet entry class after decompilation.

### `j2me-compat`

An Android implementation of the J2ME APIs used by the game.

Important packages:

| Package | Purpose |
| --- | --- |
| `javax.microedition.lcdui` | Canvas, drawing, images, fonts, commands, display bridge |
| `javax.microedition.midlet` | MIDlet lifecycle shim |
| `javax.microedition.rms` | RecordStore-style save data |
| `javax.microedition.media` | Sound and music playback |
| `com.nokia.mid.ui` | Small Nokia API compatibility surface |

## Runtime Model

The game still thinks it is running as a MIDlet:

1. Android starts `GoingMobileActivity`.
2. `MIDletActivity` creates a `CanvasView`.
3. `GoingMobileActivity` creates `ratchetandclank`.
4. The MIDlet creates menu/game canvases through J2ME-style APIs.
5. `CanvasView` drives painting and input on Android.

The compatibility layer adapts J2ME concepts to Android:

| J2ME concept | Android side |
| --- | --- |
| `Canvas` | `CanvasView` and Android drawing callbacks |
| `Graphics` | Wrapper over Android `Canvas` and `Paint` |
| `Image` | Android `Bitmap` |
| `RecordStore` | Android private app storage/preferences |
| `Player` | Android audio playback wrappers |

## Preparation Tool Internals

`tools/prepare-game.ps1` does more than unzip files. It also fixes parts of the
decompilation that Java and Android cannot use directly.

Major phases:

1. Extract assets from the JAR.
2. Remap class constants in `.class` files using the class map from
   `DEOBFUSCATION.md`.
3. Write a temporary remapped JAR.
4. Run CFR on the remapped JAR.
5. Apply source-level fixups.

The class remapping happens before decompilation so cross-class references stay
consistent.

## Known Generated-Code Repairs

Some bytecode patterns are valid, but CFR cannot always reconstruct them into
clean Java. The tool currently patches these cases:

- Resource loading through Android assets.
- `SndManager` stream handling.
- Save/load `RecordStore` local variables.
- `GameCanvas` shop flow around `lbl47`.
- `GameCanvas.E()` crate falling flow around `lbl42`.
- Missing locals and narrow numeric casts.

When a new generated-source bug is found, the preferred fix is:

1. Understand the bytecode/decompiled behavior.
2. Patch `prepare-game.ps1` with the narrowest reliable transformation.
3. Regenerate from the original JAR.
4. Build and test on device.

This keeps the repository clean while making the fix reproducible.
