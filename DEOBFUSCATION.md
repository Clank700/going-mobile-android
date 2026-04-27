# Deobfuscation Reference

This file is a practical map for programmers reading the source generated from
their own JAR. It documents names discovered from behavior, data flow, and J2ME
API usage.

Do not commit generated source, copied source from another project, assets, or
APKs. Contributions should document observations and improve local generation.

## Class Map

The original game has 10 classes. `prepare-game.ps1` remaps the class names in
bytecode before decompilation so CFR emits readable Java file names.

| Original | Generated name | Role |
| --- | --- | --- |
| `a` | `EntPlayer` | Player entity, weapon data, player animation data |
| `b` | `SndManager` | Sound manager and background audio thread |
| `c` | `LevelData` | Level and tile data holder |
| `d` | `EntEnemy` | Enemy entity data and behavior |
| `e` | `GameUI` | Small UI text/position holder |
| `f` | `MenuFont` | Menus, splash screens, loading flow |
| `g` | `GameCanvas` | Main game engine, rendering, input, physics |
| `h` | `EntBullet` | Projectile data and movement |
| `i` | `EntBase` | Base entity state shared by player and enemies |
| `ratchetandclank` | `ratchetandclank` | MIDlet entry point |

## Runtime Entry Points

| File | Important members | Notes |
| --- | --- | --- |
| `GoingMobileActivity` | `createMIDlet()` | Android wrapper. Returns `new ratchetandclank()`. |
| `ratchetandclank` | `startApp`, `pauseApp`, `destroyApp` | Original MIDlet lifecycle. |
| `MenuFont` | `run`, `paint`, `c(int)` | Menu thread, loading screen, progress callback. |
| `GameCanvas` | `run`, `paint`, `E`, `K`, `k(int)` | Main gameplay loop and physics. |

## GameCanvas Fields

The field names remain mostly obfuscated. These are the useful ones currently
known:

| Field | Type | Meaning |
| --- | --- | --- |
| `g` | `ratchetandclank` | MIDlet reference |
| `ae` | `LevelData` | Current level data |
| `af` | `Random` | Random number generator |
| `ar` | `EntEnemy[]` | Enemy instances |
| `as` | `EntPlayer` | Player instance |
| `at` | `EntBullet[]` | Player projectile pool |
| `au` | `EntBullet[]` | Enemy projectile pool |
| `bg` | `int` | Bolt/currency count used in shops |
| `ez` | `int` | Selected shop/weapon index in related menu code |

## Crate And Box Fields

These arrays all use crate index IDs.

| Field | Type | Meaning |
| --- | --- | --- |
| `bu` | `short[]` | Crate X position |
| `bv` | `short[]` | Crate Y position |
| `bw` | `short[]` | Crate content/type, `-1` after consumed |
| `bx` | `short[]` | Temporary falling Y offset |
| `by` | `short[]` | Landing Y position for a falling crate |
| `bz` | `short[]` | Upward stack link, crate on top of this one |
| `bA` | `short[]` | Downward support link, crate below this one |
| `bB` | `boolean[]` | Destroyed/support-needs-update flag |

Useful methods:

| Method | Meaning |
| --- | --- |
| `k(int)` | Handles crate destruction and updates stack links. |
| `K()` | Recalculates crate stack positions. |
| `E()` | Per-frame physics/collision update, including crate falling. |

The known crate bug came from a CFR artifact in `GameCanvas.E()`: the generated
code treated `/* GOTO lbl42 */` like a comment and nested the falling update
inside the wrong branch. `prepare-game.ps1` now restructures that block so a
crate with a broken support keeps applying `bx += 4` until it lands.

## Entity Base Fields

| Field | Type | Meaning |
| --- | --- | --- |
| `ab` | `int` | Fixed-point X/world position source |
| `ac` | `short` | X velocity or offset, depending on entity state |
| `ad` | `short` | Y velocity or offset, depending on entity state |
| `aj` | `short` | Animation frame counter |
| `ak` | `short` | Animation sequence index |
| `al` | `byte` | Current animation ID |
| `ao` | `boolean` | Active/facing/state flag depending on subclass |
| `Z` | `byte` | Entity type or state |

Methods:

| Method | Meaning |
| --- | --- |
| `u()` | Returns tile X position from fixed-point state. |
| `a(byte)` | Selects an animation sequence and resets counters. |

## Player Notes

| Field | Type | Meaning |
| --- | --- | --- |
| `EntPlayer.e` | `short[]` | Weapon stats by weapon and level. |
| `EntPlayer.i` | `int[]` | Weapon purchase prices. |
| `P` | `byte` | Bitfield of owned/unlocked weapons. |
| `Q` | `byte[]` | Weapon upgrade levels. |
| `N` | `short[]` | Weapon ammo/energy values. |
| `R` | `int` | Shop-related total used when buying all ammo. |

## Sound Notes

| Field | Type | Meaning |
| --- | --- | --- |
| `SndManager.a` | `String[]` | Sound effect base names. |
| `SndManager.b` | `boolean` | Sound effects enabled. |
| `SndManager.c` | `boolean` | Music enabled. |
| `SndManager.d` | `Player` | Current music player. |
| `SndManager.e` | `byte[][]` | Cached WAV effect data. |
| `SndManager.h` | `int` | Queued sound effect index, `-1` when idle. |

## Source Fixup Table

These generated-code repairs are applied by `tools/prepare-game.ps1`.

| Area | Why it exists |
| --- | --- |
| Resource loading | Original J2ME class resource lookup does not work the same way after Android packaging. |
| `SndManager.a(String)` | Makes sound bytes load from Android assets safely. |
| `ratchetandclank` save reads | CFR sometimes reuses a `String` local where the bytecode really needs `RecordStore`. |
| `GameCanvas` shop block | Fixes an unreachable `lbl47` artifact. |
| `GameCanvas.E()` crate block | Fixes stacked crates not falling after the support below is broken. |
| Cast fixes | Restores byte/short casts required by Java compilation. |
| Missing locals | Adds declarations CFR omitted in large control-flow methods. |

## How To Add Discoveries

Good deobfuscation notes should be based on observable behavior:

- Trace where a field is read and written.
- Record the type and owner class.
- Explain the evidence briefly.
- Prefer names that describe game behavior over guesses from memory.

Example:

```text
GameCanvas.by, short[]: landing Y position for crates.
Evidence: E() compares bv[index] + bx[index] against by[index], then snaps
bv[index] to by[index] and clears bx[index] when the crate lands.
```
