# Legal And Repository Boundaries

This is not legal advice. It is a plain-language description of how this
repository is intended to stay useful without distributing game content.

## Repository Contents

This repository may contain:

- Android project files and Gradle configuration
- The `j2me-compat` compatibility layer
- Local preparation tools
- Documentation
- Empty placeholder files
- Third-party tools listed in `THIRD_PARTY_NOTICES.md`

This repository must not contain:

- The original game JAR
- Extracted sprites, sounds, music, level data, or text assets
- Decompiled or regenerated game source
- Built APKs, AABs, or other redistributable game packages
- Download links to copyrighted game files

The `.gitignore` is configured to keep generated content out of commits, but
contributors are still responsible for checking their diffs before pushing.

## User Responsibility

Users must supply their own copy of the original J2ME game and run the
preparation tool locally. Do not distribute generated APKs or generated source
unless you have permission from the relevant rights holders.

## Contributor Rules

- Keep fixes in `j2me-compat/`, docs, Gradle files, or `tools/prepare-game.ps1`.
- Do not paste generated game source into issues or pull requests.
- Use short snippets only when needed to explain a fix.
- Do not add assets or binaries extracted from the game.
- Do not add instructions that point people to unauthorized downloads.

## Copyright Notice

Ratchet & Clank and the original Going Mobile game content are owned by their
respective rights holders. This project is unofficial and is not affiliated
with, sponsored by, or endorsed by Sony Interactive Entertainment, Insomniac
Games, or Handheld Games.

The port infrastructure in this repository is licensed separately under the
repository license. That license does not apply to the original game content.
