# Ratchet & Clank: Going Mobile Android Port Kit

This repository is a small Android port kit for the 2005 J2ME game
Ratchet & Clank: Going Mobile.

It does not contain the game JAR, extracted assets, decompiled game source,
or APKs. Users bring their own legal copy of the original J2ME JAR and run
the local preparation tool.

## What This Is

- A native Android project, not a J2ME emulator.
- A J2ME compatibility layer that maps MIDP APIs onto Android.
- A preparation script that extracts a user-provided JAR, remaps class names,
  decompiles the bytecode, and applies Android compatibility fixups.
- Documentation for the generated source and known obfuscated symbols.

## What This Is Not

- It is not an official Sony, Insomniac, or Handheld Games project.
- It is not a place to upload or share copyrighted game content.
- It is not a prebuilt APK distribution.

## Quick Start

Requirements:

- Windows PowerShell
- Android Studio, or a local Android SDK and Gradle
- Java available through Android Studio or `JAVA_HOME`
- A legal copy of `Going Mobile 1.1.0.jar`

Run:

```bat
tools\prepare-game.bat "C:\path\to\Going Mobile 1.1.0.jar"
gradlew.bat :going-mobile:app:assembleDebug
```

The debug APK is written to:

```text
going-mobile/app/build/outputs/apk/debug/app-debug.apk
```

You can also open this folder in Android Studio, sync Gradle, and run the
`going-mobile:app` module on a connected device.

## Repository Layout

```text
android-port-github/
|-- going-mobile/app/       Android app wrapper and generated game target
|-- j2me-compat/            J2ME API compatibility layer for Android
|-- tools/                  JAR preparation scripts
|-- docs/ARCHITECTURE.md    Port pipeline and runtime architecture
|-- DEOBFUSCATION.md        Class map and symbol notes for generated code
|-- LEGAL.md                Project boundaries and contributor rules
|-- THIRD_PARTY_NOTICES.md  Bundled tool notices
```

Generated files are intentionally ignored:

- `going-mobile/app/src/main/assets/*`
- `going-mobile/app/src/main/java/com/ratchetclank/goingmobile/*`
- APKs and Gradle build output
- original JAR files

Only `GoingMobileActivity.java` and `.gitkeep` placeholders are tracked in the
generated game package.

## How The Port Works

The original game is Java ME bytecode. The preparation tool performs this local
pipeline:

```text
user JAR
  -> extract assets
  -> remap obfuscated class names in bytecode
  -> decompile with CFR
  -> apply Android/J2ME compatibility source fixups
  -> build through the Android Gradle project
```

At runtime, the generated Java game code calls classes in `j2me-compat`.
Those classes implement the parts of J2ME used by this game:

- `Canvas`, `Graphics`, `Image`, `Font`, and `Display`
- `MIDlet` lifecycle bridging
- `RecordStore` backed by Android preferences
- `Manager` and `Player` backed by Android audio APIs

For more detail, read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Verified State

The current workflow has been verified locally with `Going Mobile 1.1.0.jar`:

- `tools\prepare-game.bat` completes successfully
- `gradlew.bat :going-mobile:app:assembleDebug` builds successfully
- The generated APK launches on a physical Android phone
- The known stacked crate falling bug is fixed in generated source

Device gameplay testing is still valuable. If you find a regression, include
the JAR SHA-256 printed by `prepare-game.bat`, device model, Android version,
and the steps to reproduce.

## Development

Use these docs as your map:

- [tools/README.md](tools/README.md) explains the preparation tool.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) explains the port design.
- [DEOBFUSCATION.md](DEOBFUSCATION.md) tracks known class and symbol meanings.
- [CONTRIBUTING.md](CONTRIBUTING.md) explains how to contribute without adding
  proprietary game content.

The generated game source is useful for local debugging, but do not commit it.
Portable fixes should usually go into `tools/prepare-game.ps1` or
`j2me-compat/`, so every user can regenerate the same working project from
their own JAR.

## License

The port infrastructure in this repository is licensed under the MIT License.
See [LICENSE](LICENSE).

Game content and generated game source are not part of this license and are not
distributed by this repository.
