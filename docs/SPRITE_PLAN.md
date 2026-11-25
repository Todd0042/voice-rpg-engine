# Battle Sprites Overhaul: Plan

Goal: battle sprites that look like they belong on the painted backdrops and act like fighters. Heroes face
their foes instead of the camera, every combatant has real animations (idle, ready, charge, cast, attack,
hurt, fall, victory) synced to the battle's own timing, and the engine sets a **sprite contract** that every
game must meet.

Status: decided 2026-11-25 (section 7). S1-S3 built (engine fa1d235 and after); S4 (lineup) and S5 (graphical-studio) next.

---

## 1. Where things stand

| | Today |
|---|---|
| Format | Text grids (`"..KXXK.."`) with a palette in `characters/*.json` and `enemies/enemy_sprites.json` |
| Size | 20x26 cells for everything; bosses are the same grid scaled up (`Enemy.scale`) |
| View | Front-facing; enemies are the same art mirrored |
| Frames | 4 required (`idle_upright`, `idle_crouch`, `action_cast`, `damaged`) + optional `attack`, `hurt`, `death` (added in M6; no game supplies them) |
| Animation | Two-frame breathing on a 650 ms loop; everything else is a squash, lean, flash or knockback applied to one still frame |

Coverage per game:

| Game | Party sprites | Enemies | Distinct enemy sprites | Bosses |
|---|---|---|---|---|
| echoes-of-logos | 4 | 79 | **3** (`template_brute` 34, `template_sniper` 27, `template_caster` 18) | 11, all on template sprites |
| wallys-world | 22 | 60 | 32 | 19 |
| commander-america-rpg | 1 of 11 fighters | 82 | **0** (no `spriteId`: everyone gets the grey fallback) | 39 |
| superstore-saga | 1 of 2 | 14 | 5 (template sprites) | 1 |

The backdrops are pixel art at roughly 500x280 "art pixels" (1376x768 pictures at 2-3x). A 20x26 sprite is
too small and too front-on for that scene, and hand-typed text grids can't scale to dozens of frames per
character.

---

## 2. Decisions this plan makes (each one can be overruled)

1. **Pixel art, side view facing the foe**, as in classic 16-bit battles: heroes in 3/4 profile facing
   right, toward the enemies; enemies facing left. **All art is drawn facing right** and the engine mirrors
   the enemy side, so one sheet serves either side, and a hero who turns enemy in the story needs no new art.
2. **64x64 frame cell** (a human is about 44-52 px tall), **96x96 or 128x128 for bosses**. At the
   backdrops' density a 48 px hero stands about 17% of the arena height, about the size the art implies.
   Drawn with nearest-neighbour filtering at whole-number scales, so pixels stay crisp.
3. **Aseprite sheets as the format**: a PNG sheet plus Aseprite's JSON export (frame rectangles, per-frame
   durations, tags = animations, slices = anchor points). It's the standard pixel-animation format; LibreSprite,
   Pixelorama and most AI and pipeline tools export it, so no custom tooling is needed to make art.
4. **The battle drives the animations**: the existing battle cues pick the animation, and the effects clock
   runs it. A hit-stop freezes the frame, slow motion slows it, Reduce motion stays in charge.
5. **Text grids keep working** through the same animation path (a legacy source). Every game
   keeps running on the day the engine changes; the contract tightens per game as its art arrives.

---

## 3. The sprite contract (what every game must provide)

### 3.1 Files

```
assets/game/sprites/sheets/<spriteId>.png    # the sheet
assets/game/sprites/sheets/<spriteId>.json   # Aseprite JSON export (array or hash), tags = animations
assets/game/sprites/sprites.json             # optional: variants and per-sprite settings (below)
```

A character uses the sheet named by its `spriteId` (characters: their `id` unless `spriteId` says otherwise).
Event numbers count frames **within the tag, from 1** (as an artist reads them in Aseprite's timeline).

**Slices** (Aseprite named rectangles, the first key is used for every frame unless keyed per frame):

| Slice | Required | Used for |
|---|---|---|
| `feet` | yes | The point that stands on the floor (BattleStage places this, not the cell bottom) |
| `cast` | no (default: upper front of the body) | Where projectiles and the charge glow start |
| `head` | no (default: top centre) | Where the intent icon, taunt bubble, name and target marker sit |
| `hit` | no (default: body centre) | Where projectiles land and damage numbers rise |

**Frame events** go in a tag's *user data* (Aseprite's tag "User Data" field), e.g. `release=3` or
`impact=4`: the frame on which the spell leaves the hand or the blade connects.

### 3.2 Animations (Aseprite tag names)

**Party fighters**

| Tag | Required | Loop | Plays when |
|---|---|---|---|
| `idle` | **yes** | loop | Default (4-6 frames) |
| `ready` | **yes** | loop | Turn gauge full / acting hero (weapon up, focused) |
| `charge` | **yes** | loop | The player is speaking a chant (`ChargeStart`… `ChargeEnd`); plays faster as the voice level rises |
| `cast` | **yes** | once, `release=` | `Release` of a spell (magic schools) |
| `attack` | **yes** | once, `impact=` | `Release` of a physical move |
| `hurt` | **yes** | once | Hit by damage (`Impact` on this target) |
| `ko` | **yes** | once, holds last frame | `Death` (heroes can be revived) |
| `victory` | **yes** | loop | Battle won |
| `weak` | no → `idle` | loop | HP under 25% (kneeling, panting) |
| `guard` | no → `ready` | loop | `Guard` cue |
| `step` | no → `ready` | once | Stepping forward to act (the 8-16 dp step-out) |
| `revive` | no → `idle` | once | Back from `ko` |
| `item` | no → `cast` | once, `release=` | Using an item |

**Regular enemies**

| Tag | Required | Loop | Plays when |
|---|---|---|---|
| `idle` | **yes** | loop | Default |
| `attack` | **yes** | once, `impact=` | `EnemyMove` (physical) |
| `hurt` | **yes** | once | Hit |
| `windup` | no → first frames of `attack` | loop | `Telegraph` at 75% gauge; stretched to `windupMs` |
| `cast` | no → `attack` | once, `release=` | `EnemyMove` (magic) |
| `death` | no → engine shatter / fade / dissolve of the last `hurt` frame | once | `Death` |
| `taunt` | no → `idle` | once | `Taunt` |

**Bosses**: everything for enemies, plus `cast` and `windup` **required**, `charge_ultimate` (loop, while
`UltimateCharge` turns run) and `ultimate` (once, `release=`) **required**. An optional second set with the
prefix `p2_` (`p2_idle`, `p2_attack`…) takes over after a `PhaseShift`.

Fallbacks are an engine table, so a game only ever *adds* tags. A missing required tag is a contract
failure (section 3.4), never a crash: it falls back down the chain to `idle`.

### 3.3 Variants (enemy families on a budget)

`sprites.json` lets one sheet serve a family through palette swaps, recoloured once at load and cached:

```json
{
  "sprites": {
    "ember_brute": { "base": "brute", "palette": { "#3A5F2B": "#8A2B1A", "#6E9E4A": "#D9572B" } },
    "frost_brute": { "base": "brute", "palette": { "#3A5F2B": "#1F4E79" }, "scale": 1.15 }
  }
}
```

Echoes' 79 enemies could become about 12 drawn families × variants instead of 3 shared silhouettes.

### 3.4 Enforcement

- `manifest.json` gains `"spriteContract": 1`. With it, the engine's content-pack test
  (`scripts/test_content_packs.sh`) **fails** when a party fighter, an enemy or a boss has no sheet, a
  required tag is missing, `feet` is missing, a `release` or `impact` event is missing from `cast`, `attack`
  or `ultimate`, or a frame isn't the declared cell size.
- Without it, the same check runs as a **coverage report** (per game: which characters lack which tags), so
  each game can see its gap before opting in.
- **Release is blocked** (Todd, 2026-11-25): the sync installs `.engine-sync/release_gate.gradle.kts` in each game
  and applies it from `app/build.gradle.kts`; a release build (`assembleRelease`, `bundleRelease`) runs
  `testReleaseUnitTest` first, where `GameSpriteContractTest` is strict. Debug builds are unaffected.

---

## 4. Engine work (milestones)

Each milestone ships to every game by sync and keeps all four games running.

### S1: Sheet loader and renderer (no visual change)
- `ui/sprites/SpriteSheet.kt`: parse Aseprite JSON (array and hash), frames, durations, tags, slices, tag
  user data; load the PNG once (bitmap cache keyed by sprite and variant).
- `SpriteRenderer` composable: draws one frame with nearest-neighbour filtering, at a whole-number scale
  chosen from the arena height, mirrored for the enemy side. The white hit flash is a colour filter, and the
  glow and shadow stay as they are.
- `StageColumn` places the `feet` slice on the floor line (today it uses the bottom of the cell).
- Today's text grids plug into the same path as a **legacy sprite source**: their frames become `idle`
  (upright, crouch), `cast`, `attack`, `hurt` and `death` clips, with the feet on the lowest opaque row. No
  converter and no asset churn: every game keeps running unchanged, and the contract (S3) is what pushes
  each game onto real sheets.
- The death shatter and dissolve read pixels from the sheet frame instead of the grid.

### S2: Animation state machine
- `ui/sprites/SpriteAnimator.kt`, one per combatant inside `CombatantFx`: current tag, frame and time,
  queued one-shots, and a return-to state (idle, ready or weak). It steps on `FxClock` fx time, so a hit-stop
  freezes the frame and slow motion slows it.
- The director maps cues to animations (table in 3.2) and **lines up the event frames with the battle's
  timing**:
  - `cast` and `attack` start early, so their `release` frame lands on the `Release` cue.
  - An enemy `windup` is stretched to the move's `windupMs`.
  - `hurt` starts on the frame the `Impact` lands.
  - `CombatTiming` stays the single source of timing; animations fit into it, never the other way round.
- `charge` plays at 0.8x-1.6x speed with the live voice level.
- Projectiles leave from the `cast` slice and land on the target's `hit` slice. Today both are the sprite
  centre.

### S3: Contract, fallbacks and coverage
- Fallback table and contract check (3.2, 3.4), `SpriteContractTest` in the content-pack run, and
  `scripts/sprite_coverage.sh <game>` for a readable gap report.
- `sprites.json` variants (3.3), plus per-sprite `scale` (replacing `Enemy.scale` as the art-size knob;
  `Enemy.scale` stays for story-driven growth).

### S4: Developer tools
- **Sprite lineup** in the existing asset gallery: every party fighter, enemy family and boss side by side
  on one floor line at the same pixel scale, all playing the chosen animation in step (idle, cast, attack,
  hurt…), on a neutral grey or any backdrop. Spot height, outline weight, palette and proportion mismatches
  at a glance. Tap one to open it in the sprite viewer.
- **Sprite viewer** in the existing asset gallery: every sprite with each tag playing, both facings, on any
  backdrop, with the `feet`/`cast`/`hit` slices drawn and event frames marked.
- The FX bench (developer tools in battle) gains "play tag" for the selected combatant.

### S5: Art production support (graphical-studio)
graphical-studio already renders single sprites (`generate_asset.py --pixelate --remove-bg`); what's missing
is **consistency across frames**. Add a sprite-sheet mode:
- Input: a reference image of the character (its portrait or an approved base frame) and a pose sequence per
  tag (a stick-figure skeleton for each frame, kept once in the studio as reusable "animation templates":
  humanoid idle, cast, slash, hurt, KO…).
- Each frame is generated pose-guided (ControlNet OpenPose) and reference-guided (IP-Adapter), then snapped
  to the 64 px grid, reduced to the character's palette, and has its background removed.
- Output: an Aseprite-compatible sheet and JSON with tags; slices are guessed (feet = lowest opaque row) and
  checked in the sprite viewer.

AI output will still need a cleanup pass (stray pixels, the weapon hand that changes between frames).
Aseprite or LibreSprite is fine for that. See decision 2 in section 7.

### Testing
- **E2E through real battles** (as in BattleHudTest): a hero's chant plays `ready → charge → cast → idle` with
  the `release` frame on the `Release` cue; the target plays `hurt` on impact; a hit-stop freezes the frame
  index; a boss runs `windup` for exactly `windupMs` and `charge_ultimate` while charging; `PhaseShift`
  switches to `p2_`; victory plays `victory`. Also run against the legacy text-grid sprites, so both sources share
  one path.
- Content-pack contract checks for every game (coverage report until it opts in).
- No native code: the 16 KB check is unaffected, but still run on every APK. A 64 px sheet with about
  40 frames is roughly 20-60 KB of PNG, so an all-new cast adds a few MB at most.

---

## 5. Art production options

| Route | Quality | Speed | Consistency | Best for |
|---|---|---|---|---|
| **A. Hand pixel art / commission** (Aseprite) | Best | Slow (a full hero set is 1-3 days of skilled work) | Best | Heroes, bosses |
| **B. AI-assisted sheets** (S5, then a cleanup pass) | Good after cleanup | Fast for monsters | Medium: poses drift | Monster bases, bosses' first drafts |
| **C. Modular paper doll**: one shared humanoid body animated once (every tag), with layered parts per character (outfit, hair, weapon, cape), assembled into sheets by a script | Good, uniform | Fast once the base exists | Best (all share one skeleton) | Human heroes and human enemies across all four games |

**Recommendation: a mix.**
- **Heroes** use C as the base, with each hero's layers drawn or AI-drafted and then hand-finished. One base
  body animated once covers every human fighter in all four games (Commander's 11 caricatures need
  distinctive heads and outfits; the base body takes care of the animation).
- **Monsters** use B for each family's base sheet, then palette variants. Families with only a few frames
  (blobs, wisps, constructs) are cheap.
- **Bosses** use B for a draft and A for the finish: they are on screen longest and carry the cut-ins.

---

## 6. Game rollout

Same order as the combat overhaul: **echoes-of-logos** (pilot) → superstore-saga → wallys-world →
commander-america-rpg.

### Phase 0: engine S1-S4
All four games run their current text grids through the new sprite path. They look the same, but gain
feet placement and the animation state machine.

### Phase 1: Echoes pilot art
1. The 4 heroes (Aethel, Cedric, Lyra, Zephyr): full party set (8 required tags + `weak`, `guard`).
2. Malakor (the first boss the player meets) as the boss reference, including `p2_`.
3. Enemy families: group the 79 enemies into about 12 families from their names and roles (e.g. wolves,
   wraiths, knights, cultists, constructs, crystals). One base sheet each, variants for the rest.
4. The other 10 bosses.
5. Turn on `"spriteContract": 1`, play every boss once on the phone (pocket mode on and off, Reduce motion
   on and off).

### Phase 2: the other games
Each game: coverage report → art (reusing the paper-doll base for humans) → contract on → sign-off. Commander
starts furthest behind (82 enemies with no sprite at all); Wally's World is furthest ahead (32 enemy sprites
to convert or redraw).

---

## 7. Decisions (Todd, 2026-11-25)

1. **Style and size**: 64 px side-view pixel art, facing the foe (bosses 96 or 128 px). Decided.
2. **Art route**: try graphical-studio first (S5). If its sheets come out a mess, look at other routes (paper
   doll, commission). Todd needs **a view of just the sprites** to judge consistency across characters: the
   **sprite lineup** (S4 in the engine's gallery on the phone, and in graphical-studio's sprite-sheet mode on
   the desktop) shows every character side by side at the same scale, on a plain or backdrop background,
   all playing the same animation in step.
3. **Contract strictness**: **blocks release**. A game whose sprites don't meet the contract can't be
   released (section 3.4).
4. **Media license**: new sprite art goes under each game's `LICENSE-MEDIA` (CC BY-NC 4.0); the README's
   provenance section names the AI assistance.

## 8. Rough size

| Work | Size |
|---|---|
| S1 Loader, renderer, legacy adapter | Medium-Large |
| S2 Animation state machine + cue sync | Medium |
| S3 Contract, variants, coverage | Small-Medium |
| S4 Sprite viewer, bench | Small |
| S5 graphical-studio sprite-sheet mode | Medium-Large (model setup, pose templates, palette lock) |
| Echoes art (4 heroes, ~12 families, 11 bosses) | The largest part; depends on route |
