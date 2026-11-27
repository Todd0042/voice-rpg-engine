# Combat Progression & Balance: Plan

Goal: fights that get harder as the story goes on, numbers that grow because characters learn new spells
at story moments (not because of levels), spell choice driven by what each enemy is weak to (not by mana,
cooldowns or limits), and tools that prove every game's fights are challenging, winnable and never stuck in a
loop. One engine, four games: echoes-of-logos, commander-america-rpg, wallys-world, superstore-saga.

Status: planned 2026-11-27; every decision in section 2 is Todd's. Nothing built yet. Build order: section 5.

---

## 1. Where things stand

| | Today |
|---|---|
| Levels | XP from every won fight (`xpReward`, +300 for a boss), split across the party; each level adds HP/MP/speed and +3% spell power (`combat/Progression.kt`, `DamageResolver.levelPower`). Never shown, gates nothing, enemies don't scale. A playthrough ends around level 8-19: up to **+54% damage** by the finale, so games get *easier* toward the end. The defense it adds is unused and unsaved; the level-up line says the id ("hero ascends"). |
| Spells | Every fighter has their whole kit from the start. The hero always gets the class's full kit (`spellsForClass`), so nothing can be taught to the hero; `spellGrants` exist only for flag-joined companions, and no game uses them except Echoes' three late "trials". |
| Mana | In every game: MP, costs, attune (self restore), breath spells; 16 engine files. |
| Difficulty | No game gets harder toward the end. Commander Red is unloseable (Commander 5,000 HP / 1,776 MP, companions ~4,000 HP vs enemies hitting 16-34); Echoes fights last ~3 rounds with a ~4.6x safety margin, the finale like the first fight. |
| Weaknesses | Engine has school x family affinity (x0.8 resisted to x2.0 weak), self-element impairment, a stagger for Master chants on a weakness. Commander: 82 of 83 enemies are FLESH with no element (everything neutral); Superstore 2 families; Wally 4; Echoes 7. The "Enemies" voice report gives only names and HP. |
| Enemy healing | DRAIN moves heal the enemy (every game); Wally's radiant healer has "Mend Ally". |
| Party layout | 1-3: one diagonal column at full size. 4+: two columns, sprites shrink (1.8 at 4, 1.7 at 5+), everyone but the hero stacked in one back column; six heavily overlap. Enemy speed scales by party size for 3 and 4 but drops back to 1.0 for 5 and 6. |
| Warp | Built from scenes, one per `chapterIndex`. Commander's 49 scenes are all index 0 (one entry, possibly the other track); Superstore's departments share scenes (a Cashier save can warp into the Unloader story); Echoes has 6 indexes shared by several scenes. A warp only moves to a node: flags, party, spells and won battles are not set. |
| Commander Blue | Each act's lead fights alone (an error: the leads are meant to stay and fight alongside the new lead). |
| Superstore | 7 fights shared by 4 department storylines of ~56 shifts each. |

---

## 2. Decisions (Todd, 2026-11-27)

1. **No levels or XP** in any game.
2. **Mana only in echoes-of-logos.** The engine supports mana or none per game; the other three games' "spells" are attacks and taunts.
3. **No cooldowns.** Spell choice comes from enemy weaknesses, resistances and defenses.
4. **Spells are learned at story moments and only ever added, never replaced.** Characters start small; later joiners arrive with more of their kit (milestones already passed) and still earn their own spells after joining.
5. **Healing and mana by join order** (section 3.5): main character = a targetable heal (an enemy selected heals themselves); 2nd = no heal; 3rd = the healer (party heal by % of max HP, a very strong single-target heal, one attack; earns more attacks); the rest: no heals. Echoes: Cedric (2nd) a large targetable mana restore, Zephyr (4th) a party mana restore by %; everyone keeps attune.
   - Commander Red: **Kash** has the large targetable heal, **Kennedy** the party heal.
   - Wally's World differs: **Adriana is the support character** (read as: the healer kit is Adriana's, as her Inspiration and in person; to confirm in the Wally pass).
6. **Wally's World:** Todd gains skills through the story, from 2-3 spells to the whole set; the class choice at creation is removed.
7. **Commander Blue:** a growing party (each new lead joins the ones before); earlier leads keep earning spells after handing over. **Red's invincibility is removed.**
8. **Weaknesses are learned by playing** (hitting reveals them), then remembered.
9. **Enemies never heal, restore or use mana** in any game.
10. **Never stuck in a loop** of deaths and revives, or of heals and mana restores.
11. **Balance at resonance x1.5** (the lower end of the mid range); one fixed difficulty, with engine support for difficulty presets later.
12. **Pure Story Mode wins fights the fastest way**, always.
13. **Superstore gets more fights now** (longer stories later). Department warps from before the interview ask which department.
14. **Battle formation:** no shrinking; fighters slightly overlap as if standing in front of each other, never hidden.
15. **Cheater controls** show only spells the fighter knows; **warps are locked to the save's route** (Commander track, Superstore department).
16. **Tutorials** cover only the basics of combat; Echoes also covers mana.
17. **Saves made before this change are wiped** (test saves only).
18. **Type names** for the non-magic games: proposed by Claude, approved by Todd per game.
19. **No resonance tracking.**
20. **Type names approved as proposed** (each game's `docs/ENEMY_TYPES_PROPOSAL.md`); Superstore: Henderson is
    Management, the Auditor is Corporate.
21. **Solemn fights keep their weight:** Wally's 3 AM fight ("The Alternative") keeps its weakness mechanics but
    none of the gamey layer: no "Weak!"/effectiveness callouts, no taunts, no victory fanfare; it ends on its own
    story line. An encounter-level option any game can use (also available to the Pay Cut and Shingles fights).
    The story itself is not softened.
22. **Recurring people keep the same weakness every time they're fought**, except Commander America (weak to
    Scandal in 2016, resistant from the mugshot on).
23. **Wally's Todd starts with a second attack type** from the first fight: Volt Snap (Know-How).

---

## 3. Engine work

Every new field is content-neutral with a default that leaves a game unchanged until it opts in. Each item lists
its tests (end to end on every game's content via `scripts/test_content_packs.sh`, per the engine rules).

### 3.1 Saves (E1)
- `GameSaveData` gets a format version; a save older than this change is deleted on load with a short spoken and
  on-screen notice ("Old saves from an earlier version were cleared").
- Saves stop storing level, XP and spell lists (both are now worked out from story flags); they gain a route
  (3.8) and a bestiary (3.6).
- Test: a pre-change save from each game is cleared with the notice; a new save round-trips.

### 3.2 Levels removed (E2)
- Remove `Progression`, `xpReward`, the boss XP bonus, the level-up text and popup, `levelPower` and level fields
  (model, saves, tests). Character `growth` blocks are removed from content in each game's pass.
- Test: no level or XP field in any game's content (contract).

### 3.3 Combat resource (E3)
- `manifest.json` `"combatResource": "mana" | "none"` (default `"mana"`).
- `"none"`: no MP checks, costs, regen; no attune action or voice commands ("attune", "breathe", "steady"...);
  no MP bars or costs in the HUD and console; no "not enough mana" lines; "Status" reports HP only; tutorial,
  Pocket Mode and Pure Story Mode ignore mana; MP fields ignored.
- Test: in a `"none"` game no line, chip or command mentions mana; a `"none"` game's content has no
  `mpCost`, `manaRestorePct` or breath spells (contract); Echoes unchanged.

### 3.4 Spells from the story (E4)
- **One rule:** at battle start a fighter's spells = their starting kit + every grant whose flag is set. Never
  stored. Applies to the hero (class kits gain `spellGrants`), flag-joined companions and encounter-listed
  fighters alike; replaces the saved `spellIds` and the hero's full-kit shortcut (`spellsForClass`).
- **Catch-up:** a fighter joining late arrives knowing every grant whose milestone flag has passed; they still
  have personal grants (their own story beats) after joining.
- **Announcement:** when a grant's flag is set for a fighter already in the party: a short spoken and on-screen
  line ("Lyra learns Thornwall"), as a story beat. Joining with catch-up spells is not announced spell by spell.
- **Unlearned spells:** saying a spell the fighter hasn't learned yet answers "Lyra hasn't learned Thornwall
  yet" without costing the turn (today: a generic "didn't catch that").
- **Only additions:** no spell is replaced. The spell console handles 10-12 spells.
- Tests: a contract check, run at every battle on the auto-play path of every route: every fighter has a
  single-target attack (and attune where `"mana"`); every character has at least one grant after joining;
  later joiners start with at least as many spells as earlier joiners did at their own start; the cheater HUD
  and quick-cast chips list exactly the known spells.

### 3.5 Heals, restores and roles (E5)
- **Targetable heals and restores:** a spell with `"target": "ally"` heals (or restores mana to) the chosen
  party member; if an enemy is selected, it affects the caster. Voice: "heal Lyra", "restore Cedric"; no name =
  the caster.
- **% of max:** `healPctOfMax` (like the existing `manaRestorePct`), single target or party, multiplied by
  resonance (20% at x1.5 = 30% of each fighter's max).
- **Roles by join order** (contract-checked per game, with a per-game override for Wally's World):

| Join order | Heal | Mana (Echoes) |
|---|---|---|
| 1st: main character | targetable heal | attune |
| 2nd | none | Echoes: Cedric, large targetable mana restore |
| 3rd: healer | party heal (% of max HP), very strong single-target heal, one attack; earns more attacks | attune |
| 4th | none | Echoes: Zephyr, party mana restore (% of max) |
| 5th, 6th | none | attune |

- Test (contract): each game's fighters match their role at every battle on the auto-play path; solo fighters
  always have a heal (the main character's targetable heal covers it).

### 3.6 Enemies (E6)
- **No enemy healing:** enemy moves can't heal, restore or use mana; DRAIN keeps its damage and loses the
  self-heal; "Mend Ally"-type moves are removed. Contract test.
- **Defenses:** per enemy `weak` and `resist` lists on top of the family table; `armor` (flat reduction per hit;
  piercing spells ignore it); boss phases may change weaknesses ("the aura turns to frost").
- **Type names per game:** `affinity_matrix.json` may rename schools and families (display and speech only;
  engine ids unchanged), so non-magic games get their own types.
- **`weak` / `resist` override the family table:** weak = x2.0, resist = x0.8 for that school.
- **Solemn encounters:** `"solemn": true` on an encounter silences tag callouts, effectiveness lines, taunts and
  the victory fanfare; `"endLine"` gives the line it closes on.
- **Learned weaknesses:** a school's effect on an enemy type is revealed by the first hit (the existing "Weak!" /
  "Resisted." callouts) and remembered in a bestiary in the save. The "Enemies" voice report adds what's known
  ("Orc, 300 HP, weak to fire"); a weakness icon on the enemy bar once known.
- Tests: a never-seen type reports no weakness; after one weak hit the report and icon show it, and survive a
  save and reload; no enemy move heals (contract).

### 3.7 Battle stage (E7)
- **Formation instead of shrinking** (`PartyFlank`, `EnemyFlank`): sprite size fixed at the 1-3 fighter size
  (slight perspective: back rows ~8% smaller), up to three staggered depth rows (4 = 2+2, 5 = 2+2+1,
  6 = 2+2+2), each row shifted half a sprite so heads show between the fighters in front, spread across the
  flank's width, drawn back to front. Shrinking only on very small screens, with a floor.
- **Never hidden:** placement keeps every sheet's `head` slice fully visible and most of each body.
- **Front row from content:** a per-character position (from the role) replaces the id check for "hero" /
  "vanguard".
- **Active fighter steps forward** and is drawn on top during their turn.
- **HUD:** compact party chips for 5-6 fighters (short name, HP, MP where `"mana"`, turn gauge); six fit in
  360 dp without scrolling.
- **Enemy speed by party size:** a smooth scale for 1-6 fighters (today 5 and 6 fall back to 1.0).
- Tests: placement is a pure function, tested on every game's real parties and enemy groups with their real
  sheets at phone portrait (console open and shrunk), landscape and tablet sizes: no fighter below the size
  floor, every head fully visible, feet on the floor band, nobody off screen, the active fighter on top. Then
  phone screenshots of Red's 5, Blue Act 5's 6 and Echoes' 4 for Todd.

### 3.8 Routes, checkpoints and the warp (E8)
- **Route per save:** chosen at creation (Commander's `campaignTrackId`) or by a story flag (Superstore's
  `role_*`), declared in the manifest. A save before its route flag is on the shared opening.
- **Checkpoints:** worked out by walking the route in play order (auto-play's choices); a game may mark nodes
  `"checkpoint": "Shift 11"`; otherwise scene changes in play order. Replaces `DEBUG_CHAPTER_TARGETS`.
- **Warp plays the story:** in memory, from the route's start to the checkpoint, choosing as Pure Story Mode
  does, winning battles, skipping mini-games, fixed random seed: flags, party, known spells and won battles
  are what a player would have. Only the save's route is listed.
- **Superstore before the interview:** warping past it asks which department.
- Tests: for every route of every game, warp from a fresh save to every checkpoint: right node, same route, no
  node of another route visited, party/flags/spells as auto-play had them, and playing on reaches the route's
  end. A Red save never lists Blue; a Cashier save never reaches an Unloader node (including a warp across the
  interview).

### 3.9 Pure Story Mode (E9)
- Decides for the fastest victory: highest damage per turn, known weaknesses, area spells on groups, a heal only
  to stop a fighter falling; at a fixed strong resonance (x1.8).
- Test (contract): Pure Story Mode wins every story fight of every route within a short round cap.

### 3.10 Balance settings, difficulty and the simulator (E10)
- **`combat/balance.json` per game:** `balanceResonance` (1.5 for all four), target ranges, and `difficulties`:
  presets of enemy HP/attack multipliers and balance point. The manifest picks the default; there is no
  difficulty menu yet (one fixed difficulty), but the engine supports adding one later.
- **Simulator** (JVM, the real `CombatViewModel` at instant timing, many seeded runs): every story fight of every
  route, with the party, stats and known spells at that point, played by a **sensible player** (matchup-aware,
  heals when needed) at the balance point, plus x1.1 and x1.8 and a **spam player** (always the highest-power
  spell). Reports win rate, rounds, HP lost, heal/restore share and suggested enemy HP/attack for fights outside
  their range.
- **Targets at x1.5** (sensible player):

| Fight | Rounds to win | Safety margin (rounds to wipe / rounds to win) |
|---|---|---|
| Early regular | 3-4 | ~3.0 |
| Mid regular | 4-5 | ~1.8 |
| Late regular | 4-6 | ~1.4 |
| Boss | 6-10 | 1.3 mid, 1.15 late |
| Finale | 10-14 | ~1.1 |

- **Effort matters:** at x1.1 late bosses are losable; at x1.8 early fights are easy wins; the spam player is
  clearly worse (fights ~30-40% longer, late bosses losable).
- **No loops:** a fight fails if more than about a third of party turns are heals or mana restores, if a
  fighter falls and is revived more than once, or if it passes its round cap.
- **Every spell earns its place:** each spell in a kit is the best choice in at least one fight of its game;
  every fight has, for each enemy, a known spell that is neutral or better.
- **Growth:** spell base power follows a per-game curve from x1.0 at the start to ~x2.5-3.0 at the finale
  (new spells placed on it); heals grow slower than enemy damage.

### 3.11 Tutorials (E11)
- Each game's tutorial covers the basics: speaking a spell, targeting, healing, weaknesses ("Weak!"); Echoes
  also mana and attune. Built from that game's starting kit; `"none"` games skip mana steps.

### 3.12 Speech and languages (E12)
- New engine lines in all 5 engine languages: spell learned, not learned yet, weakness in the Enemies report,
  the department question, old saves cleared. New story lines in each game's `translations/`.

### 3.13 Docs (E13)
- `GAME_CREATION_GUIDE.md`: `combatResource`, routes and checkpoints, spell grants and `target`/`healPctOfMax`,
  roles and positions, enemy `weak`/`resist`/`armor`, type names, `balance.json` and difficulties.
- Each game's README: its combat section and test counts, in the same commit as its pass.

---

## 4. Per-game passes

Each pass: roles and kits on the growth curve, grants placed on story beats (with announcement lines),
enemy types/weaknesses/defenses, no enemy healing, tutorial, then rebalance until the simulator and contract
tests pass. Story lines are drafted in the game's voice for Todd's review; nothing added unseen.

### echoes-of-logos (first)
- Keeps mana. Aethel (any of the 4 classes): one attack + targetable heal to start, about 7 spells by the end.
- Cedric (2nd): no heal; large targetable mana restore; guardian. `lay_on_hands` and the `aegis_dawn` grant lose
  their healing (aegis_dawn becomes a party guard).
- Lyra (3rd, chapter 6): the healer (party % heal, strong single heal, one attack; earns more attacks).
- Zephyr (4th, chapter 8): striker; party mana restore (% of max); arrives with about 4 of his 6 spells.
- Enemy weaknesses per enemy on the existing 7 families.

### commander-america-rpg
- **Blue:** a growing party: Hill-Rod (Act 1), + Nancy (2), + Joe (3), + Kamala (4), + Zohran and Swagger (5);
  join flags at each handover; every Act 2-5 encounter lists the whole party; handover scenes acknowledge the
  leads staying. Roles: Hill-Rod main (targetable heal), Nancy none, Joe healer, Kamala/Zohran/Swagger none;
  earlier leads keep earning.
- **Red:** Commander (main, targetable heal), Marco (no heal), **Kash** (large targetable heal), **Kennedy**
  (party % heal), JD Pants (none); stats brought to Blue's scale (the "1,776" joke can live in dialogue).
- Scene `chapterIndex` / acts set for both tracks; route checkpoints per track.
- New enemy types for all 83 enemies (today all FLESH); type names proposed for approval.

### wallys-world
- Remove the class choice at creation; Todd is one character (today `hero` for 3 fights, then `todd_shop`)
  whose kit grows from 2-3 spells to the whole set; main character's targetable heal.
- Adriana is the support: the healer kit as her Inspiration (no attack: it can't attack) and in person.
- Remove "Mend Ally"; widen enemy types beyond 4 families; type names proposed for approval.

### superstore-saga
- Routes: the four departments (`role_*` flags); the interview asks; warps before it ask.
- Each department class: a growing kit with the main character's heal (a solo hero heals themselves).
- **More fights now:** about 12-14 per department across its ~56 shifts (today 7 shared); new fights reuse the 8
  existing enemies and recoloured variants where they fit; new enemies get sprite briefs for Anti-Gravity
  (the sprite contract blocks release without them). Longer stories later.
- Widen enemy types beyond 2 families; type names proposed for approval.

---

## 5. Build order

1. **Engine:** E1 saves, E2 levels, E3 combat resource, E4 spells from the story, E5 heals and roles, E6 enemies,
   E8 routes and warp, E9 Pure Story Mode, E10 balance and simulator, E11-E13; then E7 formation. Each with
   its tests; content packs run against all four games after each step.
2. **Commander:** Blue's party and joins, scene chapters, Red's stats (so its routes and six-fighter Act 5 are
   right before balancing).
3. **Game passes:** Echoes, then Commander (both tracks), Wally's World, Superstore.
4. **Todd play-tests Echoes** on the phone before the other three are finished.

---

## 6. To confirm during the passes

- Wally's World: the healer kit belongs to Adriana in both forms (decision 5's reading).
- Each game's story lines for spell beats, Blue's handovers and Red's rebalance (drafts for Todd's review).
