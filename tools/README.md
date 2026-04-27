# Preparation Tool

`prepare-game.bat` turns a user-provided J2ME JAR into a buildable local Android
project.

The older `extract-game-assets.bat` name is kept as a wrapper for convenience,
but the real entry point is:

```bat
tools\prepare-game.bat "C:\path\to\Going Mobile 1.1.0.jar"
```

## Inputs

- `Going Mobile 1.1.0.jar`, supplied by the user
- `cfr.jar`, bundled at the repository root
- The class mapping and source fixups inside `prepare-game.ps1`

## Outputs

Generated outputs are ignored by git:

```text
going-mobile/app/src/main/assets/
going-mobile/app/src/main/java/com/ratchetclank/goingmobile/
```

The app wrapper `GoingMobileActivity.java` remains tracked. The generated source
files sit beside it locally after preparation.

## Pipeline

1. Validate the JAR path and print its SHA-256.
2. Extract non-class resources into Android assets.
3. Rewrite class references in the JAR so CFR emits useful package and class
   names.
4. Run CFR to decompile the remapped JAR.
5. Apply compatibility fixups for Android and known decompiler mistakes.
6. Delete temporary extraction files unless `-KeepIntermediate` is passed.

## Options

```powershell
tools\prepare-game.ps1 "C:\path\to\Going Mobile 1.1.0.jar"
tools\prepare-game.ps1 "C:\path\to\Going Mobile 1.1.0.jar" -Build
tools\prepare-game.ps1 "C:\path\to\Going Mobile 1.1.0.jar" -KeepIntermediate
```

`-Build` runs `gradlew.bat :going-mobile:app:assembleDebug` after generation.

`-KeepIntermediate` leaves the temporary `extracted/` directory in place for
debugging the tool.

## Known Fixups

The original bytecode decompiles imperfectly. The tool currently repairs:

- J2ME resource loading calls so they use `CanvasView.openResource`.
- Java reserved-word and type issues introduced by decompilation.
- Several missing local variable declarations in large control-flow methods.
- `SndManager` stream loading so Android assets are read correctly.
- `ratchetandclank` save-data reads where a decompiled `String` should be a
  `RecordStore`.
- `GameCanvas` shop control flow around the `lbl47` decompiler artifact.
- `GameCanvas.E()` crate physics around the `lbl42` artifact, fixing stacked
  crates that previously stayed suspended after support crates were broken.

When fixing generated game code, prefer adding a targeted fixup here instead of
editing ignored generated files directly.

## Troubleshooting

If the tool fails before decompilation, check that the JAR path is correct and
quoted if it contains spaces.

If Java cannot be found, run from an Android Studio terminal or set `JAVA_HOME`
to a JDK. Android Studio's bundled JBR works.

If the Android build fails after generation, compare the generated file and line
with `prepare-game.ps1`. Most failures should be solved by another source fixup.

If the game crashes on loading, run a fresh prepare step first. The working
workflow generates source, not direct Android execution of the original JAR.
