# Combat Feel Overhaul: Implementation Plan

Goal: make battles exciting to watch and to play. The rules of combat are already solid (resonance,
affinity, statuses, movesets). What's missing is presentation and moment-to-moment feel. Everything
here lives in the engine (`com.voicerpg.engine.*`), is content-neutral, and is configured per game
through JSON. Each game then adds its own flavor (portraits, title-card style, taunts, sounds).

Games in scope: **echoes-of-logos**, **commander-america-rpg**, **wallys-world**, **superstore-saga**.

---

## 0. Prerequisite: engine convergence

All four games must run the identical engine before this work starts. That work is planned in
[`ENGINE_CONVERGENCE_PLAN.md`](ENGINE_CONVERGENCE_PLAN.md): guardrails against drift, removing the
game content baked into the engine, Wally's catch-up, the Echoes merge and the Commander extraction.
The combat milestones below then reach every game through a normal sync.

---

## 1. Architecture (engine)

### 1.1 Battle cues: logic emits events, the presentation layer plays them
Today `CombatViewModel` both decides outcomes and sequences visuals through about 60 hard-coded
`delay(...)` calls. Add a **cue stream**:

```kotlin
sealed interface BattleCue {
    data class ChargeStart(val casterId: String) : BattleCue
    data class ChargeLevel(val casterId: String, val level: Float) : BattleCue   // from rmsLevel, 0..1
    data class Release(val casterId: String, val spell: Spell, val resonance: ResonanceResult) : BattleCue
    data class Impact(val targetId: String, val amount: Int, val affinity: Float, val tier: ResonanceTier,
                      val fromLeft: Boolean, val isHeal: Boolean) : BattleCue
    data class Telegraph(val enemyId: String, val move: EnemyMove, val chargeTurns: Int) : BattleCue
    data class TitleCard(val text: String, val style: String, val side: Side) : BattleCue
    data class Stagger(val enemyId: String, val interruptedMove: String?) : BattleCue
    data class Streak(val count: Int) : BattleCue
    data class CutIn(val characterId: String, val tier: ResonanceTier) : BattleCue
    data class Death(val combatantId: String, val isEnemy: Boolean) : BattleCue
    data class Taunt(val enemyId: String, val line: String) : BattleCue
}
val cues: SharedFlow<BattleCue>
```

- New package `ui/fx/` holds `BattleFxDirector`, which collects cues and drives every visual system.
- The ViewModel waits on **one** call, `fx.awaitBeat(cue)`. It returns a duration from a central timing
  table (§1.3), so pocket mode and tests can make it instant.
- Unit tests assert the *order of cues* without rendering anything. This keeps the existing
  "settle pattern" rule in AGENTS.md §5.

### 1.2 FX clock (time-based, with hit-stop)
- `FxClock` advances on frame time (`withFrameNanos` delta), not "per frame". This fixes projectiles
  flying twice as fast on 120 Hz phones (`SpellVfxEngine.update(0.045f)` per frame today).
- `FxClock.timeScale`: hit-stop sets it to 0 for N ms, and slow-motion sets it to 0.3. Projectiles,
  particles, sprite animation and number pops all read this clock, so one freeze stops the whole arena.

### 1.3 Timing table
All waits move into `CombatTiming` (one place), scaled by resonance tier: Basic turns are snappy,
Transcendental turns linger. Pocket mode keeps its own audio-paced timings, which are unchanged.

### 1.4 Presentation config (opt-in, all fields defaulted)
A new optional file, `app/src/main/assets/game/combat/presentation.json`:

```jsonc
{
  "intensity": 1.0,                    // global multiplier for shake/flash/hit-stop
  "titleCardStyle": "banner",          // banner | headline | pa_announcement | scroll  (engine-drawn styles)
  "damageNumberStyle": "pop",          // pop | receipt | comic
  "labels": { "weak": "WEAK!", "resist": "RESIST", "crit": "{tier}!" },   // localizable
  "cutIn": { "minTier": "MYTHIC", "style": "slash" },
  "streak": { "label": "RESONANCE x{n}", "minTier": "ADEPT" },
  "telegraph": { "showIntentIcons": true, "bossUltimateChargeTurns": 1 },
  "deathEffect": "shatter",            // shatter | fade | dissolve
  "music": { "intensityTrack": null, "lowHpThreshold": 0.3 }
}
```

Per-content additions (all optional, with defaults):
- `characters/*.json` → `combat.cutIn`: `{ "portrait": "...", "color": "#...", "quote": "..." }`.
  The default is the character's `portraitAsset` and `themeColor`.
- `enemies.json` → `scale` (default 1.0; bosses default 2.0), `taunts: []`, `hitReaction`
  (`flinch | stagger | comic_spin`).
- Enemy moves (`enemy_spells.json` / movesets) → `titleCard: true|false` (default true for named
  moves), `windupMs`, `intentIcon`.
- Sprite JSON → optional frames `attack`, `hurt`, `death`. When missing, the engine builds them
  from existing frames with transforms.

Rule: a game with **no** new JSON still gets the full upgrade with neutral defaults
(AGENTS.md "Opt-In Features & Compatibility").

### 1.5 Accessibility and safety
- Options gets **Reduce motion** (no shake or zoom; hit-stop kept short) and **Reduce flashing**
  (no full-screen white; follows WCAG 2.3.1, at most 3 flashes per second). Both saved in settings.
- Pocket (eyes-free) mode skips the visual beats but keeps the audio stingers. Cut-ins and title
  cards are spoken ("Malakor readies Cords of Severance!").
- Performance budget: 60 fps on a mid-range device. Hard cap of 900 live particles; shatter uses
  instanced `drawPoints`.

---

## 2. Engine milestones

Each milestone is one PR-sized change: engine tests green, `compileDebugKotlin` clean, then synced to
the games that are ready (see §3). Letters map to the suggestions list (A–J).

### M1: Foundation (needed by everything else)
- `BattleCue` stream, `BattleFxDirector`, `FxClock` with time scale, `CombatTiming` table.
- Port `SpellVfxEngine` and `ParticleCanvas` to `FxClock` (**J**: frame-rate-independent projectiles).
- Shake only the arena box, in the hit direction (today the whole `Scaffold`, console included, moves).
- `presentation.json` loader in `GameContent` with defaults. Reduce-motion / reduce-flashing options.
- Tests: cue order for a cast, enemy turn and victory; defaults load when the file is missing;
  projectile travel time is the same at 60 and 120 fps.

**Status: done (2026-11-24).** Built as planned, with these differences:
- **Cue set.** Beside the plan's cues, logic also emits `EnemyMove`, `PhaseShift`, `Guard`, `Reinforcements`,
  `TurnEnd`, `BattleStart` and `BattleEnd`, so every wait in a turn is a cue's hold. `Impact` carries all
  hits of one release (an area spell lands together; M2 staggers its numbers from that list).
  `ChargeStart`/`ChargeLevel`, `Telegraph`, `TitleCard`, `Stagger`, `Streak`, `CutIn` and `Taunt` arrive
  with the milestones that emit them.
- **`awaitBeat`.** The view model waits on the timing table itself (`beat(cue)` = emit + `CombatTiming`
  hold), never on the screen, so a battle runs the same with no screen collecting (pocket mode, tests).
  The view model no longer owns the projectile and particle engines; the director does.
- **Timing.** Behaviour-preserving: the table holds today's durations (the 1000-1200 ms pre-hit banner
  stays until M3). Projectiles now fly for exactly the release's travel time (420 ms hero, 350/550 ms
  enemy), so impact bursts land with the damage.
- **`presentation.json`** holds only `intensity` (shake strength, 0-2) for now; each field arrives with
  the effect that uses it, so no game can configure an effect that doesn't exist yet.
- **Options.** Reduce motion is in (no shake; saved with the slot). **Reduce flashing moves to M2**, with
  the white flashes it turns off: nothing in M1 flashes.
- Not touched: the tutorial battle (`TutorialBattleViewModel`/`TutorialBattleScreen`) keeps its own
  small shake; it gets the cue layer when it is next reworked.

### M2: Impact and numbers (B, D)
- Hit-stop 60–180 ms by tier; white silhouette flash (sprite drawn with a solid color filter);
  knockback 8–14 px with a spring back; Transcendental adds slow motion, a full flash and a
  camera punch-zoom (scale the arena 1.0 → 1.06 → 1.0).
- Damage numbers: bigger, a scale pop (1.6 → 1.0, overshoot), an arc and a bounce. AoE numbers spread
  and staggered by 60 ms. Labels: WEAK! (gold), RESIST (grey), tier word on MYTHIC+. Heals rise in green.
- Turn, mana and "charging" messages move out of the arena into a status line above the console
  (they currently reuse `FloatingCombatText`).

**Status: done (2026-11-24).** Hit-stop 60-180 ms by tier (enemy blows 70, boss ultimates 140; at most
60 with Reduce motion), white sprite flash, knockback 8-14 dp with spring-back, squash on hit; Transcendental
adds 450 ms slow motion, a zoom punch and a full-screen flash (max 3/s; none with **Reduce flashing**, now in
Options and saved). Numbers come from the director (pop/receipt/comic per `damageNumberStyle`; `labels`
configurable), area hits stagger 60 ms; damage-over-time ticks are `Impact` cues with `isTick` (number only).
All other messages moved to a status line above the console. `presentation.json` now carries the full §1.4
schema.

### M3: Voice charge-up (A), the signature feature
- While listening: `rmsLevel` → `ChargeLevel` cues → caster aura (radius, brightness, particle
  rate), the arena background dims to 70%, and a synthesized charge hum (new `SfxManager.chargeHum(level)`,
  PCM like the existing effects) follows the voice.
- Reorder `processIncantation`: the resonance reveal (banner and fanfare) plays **together with
  the impact**, not before the projectile. Remove the 1000–1200 ms pre-hit banner wait.
- The resonance meter fills live while speaking, then snaps to the scored value on release.
- Typed chants (`submitTypedChant`) play a short simulated charge, so the beat still exists.

**Status: done (2026-11-24).** The view model watches the microphone: while the active hero's turn is
listening it emits `ChargeStart`/`ChargeLevel`/`ChargeEnd` (small changes at most every 50 ms, jumps at once)
and drives `SfxManager`'s streaming charge hum. The director dims the backdrop to 70% and grows a gold aura
and rising motes on the caster. The pre-hit banner wait is gone: Release → (flight) → ResonanceReveal + banner
+ fanfare + Impact as one moment. Typed and automatic chants hold a 350 ms simulated charge. The console's
meter follows the voice while listening ("CHARGING…") and snaps to the scored value.

### M4: Telegraphs and title cards (E)
- An enemy picks its move **when its gauge reaches 75%** (`EnemyBrain.pickMove` moves earlier
  and is stored as `plannedMove`). An intent icon shows above it (attack, AoE, buff, summon, drain,
  ultimate).
- Enemy wind-up: a new `CASTING` stance for enemies (lean forward, glow in the school color,
  `windupMs`), then the projectile.
- Move title card on every named enemy move, in the game's `titleCardStyle`.
- Boss ultimates (`powerMult >= 2.0`) take a charge of `bossUltimateChargeTurns`. A red bar shows over
  the boss and the narrator warns in pocket mode.
- Tests: `plannedMove` is deterministic with a seeded RNG; ultimates always telegraph at least one turn.

**Status: done (2026-11-24).** Enemies plan at 75% gauge (`Telegraph` cue, `Enemy.plannedMove`, icon from
`combat/Intent.kt` or the move's `intentIcon`) and use that move unless it became unusable. Attacks wind up
(`windupMs` per move; default 280 ms, 650 for ultimates): the sprite leans in and glows in the school colour.
Named moves (`titleCard`, default true unless "Attack") show a title card in `titleCardStyle` (banner,
headline, PA announcement, scroll). Boss ultimates (`powerMult >= 2.0`) charge for
`telegraph.bossUltimateChargeTurns` of the boss's turns (`UltimateCharge` cue, red bar, narrator warning)
before firing. The view model takes a seedable `random`; tests: `EnemyTelegraphTest`.

### M5: Reward bold voices (F)
- **Stagger**: a MASTER+ chant that hits a weakness (affinity ≥ 1.5) pushes the target's gauge
  back by 35%. A MYTHIC+ hit on a charging boss **interrupts** the ultimate (`Stagger` cue, title
  card "BROKEN!").
- **Resonance streak**: consecutive casts at or above `streak.minTier` build a counter. It changes
  **visuals and audio only** (bigger particles, a higher fanfare pitch, a streak badge) and **never
  damage**, which keeps invariant I-1 (resonance is the only multiplier). It resets on a lower
  tier or when a hero falls.
- Balance: extend the balance harness (commander `docs/combat-design/BALANCE-HARNESS.md`) with
  stagger. Gate: no encounter becomes trivial by stagger-locking (cap 1 stagger per enemy per round).
- Tests: the streak never changes damage numbers (property test over all tiers); stagger cap; interrupt rules.

**Status: done (2026-11-24).** Stagger (MASTER+ on affinity ≥ 1.5: gauge −35%) and interrupt (MYTHIC+ on a
boss holding its ultimate: plan cleared, "BROKEN!" title card, narrator line, stinger) share the cap of one per
enemy per round (`Enemy.staggeredRound`). The streak counts casts at or above `streak.minTier`, resets on a
weaker cast, a fallen hero or a new battle, and only changes looks and sound (badge, bigger bursts from 3,
fanfare pitch +6% per step). Damage rolls now use the view model's injectable `random`, which makes the
property test possible: `BoldVoiceTest` shows identical damage at every tier with and without a streak.
Balance harness: not extended; the per-round cap bounds stagger to −0.35 gauge per enemy per round, which
`BoldVoiceTest` enforces. Revisit if a boss fight feels stun-locked on device.

### M6: Death shatter and living sprites (C, H)
- Shatter: on death, every opaque pixel of the sprite's current frame becomes a particle (color,
  velocity away from the hit, gravity, fade). The sprite grid is already data (`PixelSpriteData`), so
  this works for every enemy in every game with no new art.
- Squash and stretch on hit and cast, anticipation before attacks, a small idle hop, and drop shadows
  under every combatant.
- Boss scale from `enemies.json` `scale` (bosses default 2.0). `EnemyFlank` layout reserves the
  space (a boss takes the back row).
- Optional sprite frames `attack` / `hurt` / `death` used when present.

### M7: Portrait cut-ins (G)
- At MYTHIC+ (`cutIn.minTier`): a diagonal slash panel slides across with the character's portrait
  (or selfie portrait), theme color, speed lines and the optional `quote`, for about 600 ms, with the
  FX clock in slow motion behind it.
- Fallback when there is no portrait: a zoomed pixel sprite on the panel.
- Enemy bosses can use the same cut-in for their ultimates (`enemies.json` `cutIn`).

### M8: Audio (I)
- Per-school synthesized impact sets (fire crackle, ice shatter, wind whoosh, thunder, holy chime,
  shadow thrum, physical thud) with ±8% random pitch. A low-passed "whomp" layer during hit-stop
  (filtered in the PCM synthesis). Shatter and stagger stingers.
- Music intensity: optional `music.intensityTrack` crossfades in (a second `MediaPlayer`) below the
  low-HP threshold and on boss phase changes. Also allowed per encounter in `encounters.json`.

### M9: Pacing pass (J)
- Tune `CombatTiming` on device: a Basic turn from release to next input in ≤ 1.6 s, a
  Transcendental turn about 3.5 s with all the trimmings.
- Remove the leftover ad-hoc `delay()` calls that `CombatTiming` replaced.

### Verification for every milestone
- `./gradlew testDebugUnitTest` and `compileDebugKotlin` in the engine.
- An **FX test bench**: extend the developer tools (`EnvironmentSwitcherBar` / `DebugWarpDialog`) with
  buttons that fire each cue (every tier, weak/resist, stagger, shatter, cut-in, boss ultimate) on the
  current encounter. This is the main way to judge feel on a device.
- Sync: `./scripts/sync_engine_to_games.sh --verify <ready games>`, then each game's tests.
- Device check on at least one phone, portrait and landscape, with reduce-motion on and off and in pocket mode.

---

## 3. Game rollout

Order (Todd, 2026-11-24): **echoes-of-logos** (pilot) → **superstore-saga** → **wallys-world** → **commander-america-rpg**.
Pilot status: echoes-of-logos runs M1 (engine 44e9572) with school-distinct spell `graphics` and
`presentation.json` intensity 1.1; installed on Todd's phone 2026-11-24 for testing. Its title cards, cut-ins,
Malakor telegraph and intensity tracks follow M4, M7 and M8.
The engine milestones land everywhere through sync; the per-game work below is **JSON and assets only**.

### Phase 0: done by the convergence plan
See `ENGINE_CONVERGENCE_PLAN.md`. Every game is on the identical engine before M1 starts.

### Phase 1: per-game flavor (after each engine milestone reaches the game)

**superstore-saga** (retail comedy; 16 characters, 19 portraits; foes like Entitled Customer,
Extreme Couponer, Regional Auditor, Loss Prevention Clint, Store Manager Henderson)
- `titleCardStyle: "pa_announcement"` (store intercom card); `damageNumberStyle: "receipt"`.
- Taunts per enemy ("I want to speak to your manager"). Cut-in quotes for the staff.
- Bosses Clint and Henderson: `scale` 2.0, ultimate cut-ins, intensity track for the Henderson fight.

**wallys-world** (22 characters, 20 portraits, 60 enemies such as The Freight Wall, The Paperwork Tide,
The Friday Rush, Jury, The Man They Wanted)
- 60 enemies: rely on engine fallbacks (transform wind-ups, shatter) and add `taunts` and `hitReaction`
  where they're funniest. Choose a title-card style that suits its tone (`headline` or `banner`).
- Cut-ins for the main cast; boss scale for the act bosses.

**echoes-of-logos** (high fantasy: Aethel/hero, Cedric, Lyra, Zephyr; bosses Malakor, Vaelor,
Ouros, Nocturne; 14 portraits)
- School-distinct spell `graphics` in `spells.json` (fire trail, ice-crystal freeze then shatter,
  wind slash across the screen); `titleCardStyle: "scroll"`.
- Cut-ins with quotes for the four heroes; boss ultimate cut-ins and charge telegraphs for Malakor's
  Cords of Severance.
- Intensity tracks for the boss encounters.

**commander-america-rpg** (political parody)
- `titleCardStyle: "headline"` (newspaper headline for every move, e.g. "TRIPLE-CIRCLE VENN DIAGRAM
  TRAP"); `damageNumberStyle: "comic"`.
- `labels.crit`: "FACT-CHECKED!" as a stamp; enemy taunt bubbles; `hitReaction: "comic_spin"`.
- A podium-style victory screen through the manifest victory fields.

### Phase 2: per-game sign-off
- Each game: unit tests green, every boss encounter played once on device with pocket mode on
  and off, reduce-motion on and off. Each game's release is its own call (AGENTS.md §7).

---

## 4. Decisions needed before starting

1. **Pilot**: decided 2026-11-24: Echoes of Logos.
2. **Assets**: synthesized sounds only (no new files), or will you supply intensity music
   tracks per game?

## 5. Rough size

| Work | Size |
|---|---|
| M1 Foundation | Large (refactors the turn flow of `CombatViewModel`) |
| M2 Impact and numbers | Medium |
| M3 Voice charge-up | Medium |
| M4 Telegraphs and title cards | Medium–Large (AI planning change plus UI) |
| M5 Stagger and streak | Medium (plus balance harness) |
| M6 Shatter and sprite life | Medium |
| M7 Cut-ins | Small–Medium |
| M8 Audio | Medium |
| M9 Pacing | Small (tuning) |
| Phase 1 per game | Small–Medium each (JSON and assets) |
