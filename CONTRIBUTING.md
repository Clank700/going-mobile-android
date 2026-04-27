# Contributing

Thanks for helping make this easier to build and understand.

## The Main Rule

Do not commit copyrighted game content.

That includes:

- Original JAR files
- Generated Java game source
- Extracted assets
- APKs or AABs
- Large pasted decompiled source snippets in docs or issues

## Good Contributions

- Fixes to `j2me-compat/`
- Improvements to `tools/prepare-game.ps1`
- Android build and device compatibility fixes
- Documentation for known obfuscated symbols
- Small, focused bug reports with reproduction steps

## Working On Generated Source Bugs

Generated source is ignored, but it is still useful locally.

Recommended workflow:

```bat
tools\prepare-game.bat "C:\path\to\Going Mobile 1.1.0.jar"
gradlew.bat :going-mobile:app:assembleDebug
```

If you need to change generated game code, first prove the local edit works.
Then move the fix into `tools/prepare-game.ps1` so it is applied every time a
user regenerates from their own JAR.

## Bug Reports

Please include:

- Device model
- Android version
- App version or commit hash
- JAR SHA-256 printed by `prepare-game.bat`
- Steps to reproduce
- Whether the issue happens from a clean regenerate

## Deobfuscation Notes

When adding to `DEOBFUSCATION.md`, document how you know what a symbol means.
Behavioral evidence is best: reads, writes, call sites, values, and visible
runtime effects.

Avoid guesses presented as facts.
