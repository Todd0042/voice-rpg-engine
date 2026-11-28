# Battle background prompts: all four games

Image prompts for every game's **battle screen backgrounds**, in **landscape, portrait and square (foldables)**,
with the fight each background is for. Written from each game's story: the scene each fight happens in, the
narration that starts it, and the enemies in it.

The backgrounds the games ship today are the engine's five fantasy places (forest, castle, dungeon, cave, swamp),
re-skinned. They are why a retail story fights in a jungle or a crystal cave. Each game here gets its own places,
and **every fight is mapped to the place where its story happens**.

| Game | Backgrounds | Images (×3 shapes) | Fights mapped |
|---|---|---|---|
| Wally's World | 13 | 39 | 40 (39 in the story, plus the unused Claim Blizzard) |
| Superstore Saga | 12 | 36 | 20 |
| Commander America | 22 | 66 | 35 |
| Echoes of the Logos | 17 | 51 | 32 |
| **Total** | **64** | **192** | **127** |

Order of work: Wally's World, Superstore Saga, Commander America, Echoes of the Logos (Echoes' fantasy set already
roughly fits; the other three don't fit at all).

---

## 1. How to build each prompt

Every image's prompt is **four parts joined in order**:

```
[GAME STYLE]  +  [BACKGROUND SCENE]  +  [SHAPE BLOCK]  +  "Negative: " [GAME NEGATIVE]
```

- **GAME STYLE**: one per game (in each game's section).
- **BACKGROUND SCENE**: one per background (the tables in each game's section). The **Portrait** line is
  appended to the scene for the portrait image only.
- **SHAPE BLOCK**: one of the three blocks below.
- **GAME NEGATIVE**: one per game.

### Shape blocks

**Landscape** (`<id>.png`, 1376×768):
> Wide 16:9 battle arena backdrop. The horizon, where the back wall or scenery meets the floor, is a straight level
> line at 55% of the image height. The floor fills the bottom 45%: flat, open and clear from the left edge to the
> right edge, one even surface. Landmarks sit above the horizon in the centre and back; props only at the far left
> and far right edges, behind the floor line.

**Portrait** (`<id>_p.png`, 768×1376):
> Tall 9:16 battle arena backdrop of the same place, recomposed vertically. The horizon is a straight level line at
> 55% of the image height. The floor fills the bottom 45%: flat, open and clear across the full width. Use the
> extra height above the horizon for the place's tall features (ceiling, racks, towers, sky). Nothing important
> at the far left or right edges, which may be cropped.

**Square** (`<id>_s.png`, 1024×1024), for foldables:
> Square 1:1 battle arena backdrop of the same place. The horizon is a straight level line at 55% of the image
> height. The floor fills the bottom 45%: flat, open and clear across the full width. The place's main landmark
> sits centred above the horizon, with balanced scenery to either side.

### Rules for every image (all games)

These follow from how the battle screen draws the background (`ui/combat/BattleStage.kt`):

1. **The horizon is at 55% of the height in all three shapes.** The engine lines the painted horizon up with the
   floor the fighters stand on. One value for every image means `artHorizon` and `artHorizonPortrait` are both
   `0.55` and nothing needs measuring.
2. **The floor is the stage.** The party stands on the left of it and the enemies on the right, from just below
   the horizon to the bottom edge. Keep it flat, evenly lit and plain: no props, puddles, shadows, carts or
   pallets on it, no strong patterns, no perspective lines racing to a vanishing point. Mid-tone and slightly
   desaturated, so 64 px sprites read clearly on top.
3. **Side-on, eye-level camera**, as in a classic 16-bit JRPG battle: we look across the floor at the back wall or
   scenery, with gentle depth.
4. **No people, no animals, no creatures, no characters** in any image, not even in the distance or as
   silhouettes. Every fighter is drawn by the engine.
5. **No text**: no readable signs, labels, logos, brand names, numbers or UI. Signs are blank or abstract shapes.
6. **Pixel art at an exact grid.** Generate, then reduce to the art grid with nearest-neighbour and scale it back
   up by exactly 4, so every art pixel is a crisp 4×4 block:

   | Shape | Art grid | Final file |
   |---|---|---|
   | Landscape | 344×192 | 1376×768 |
   | Portrait | 192×344 | 768×1376 |
   | Square | 256×256 | 1024×1024 |

   Limited palette (about 32 to 48 colours), hard pixel edges, no blur, no anti-aliasing, no JPEG artifacts.
   **Save as PNG.**
7. **The three shapes are the same place** at the same time of day and in the same light, so a phone rotating or a
   foldable opening keeps the fight in the same room.

### Where the files go

- Files: `app/src/main/assets/environments/<id>.png`, `<id>_p.png`, `<id>_s.png` in each game.
- **Not** `app/src/main/assets/game/environments/`. The engine loads `backgroundAsset` from the assets root, so
  images in `game/environments/` are never shown. Art Studio's backdrop job currently writes there (1344×768): move
  its output, or point the job at `environments/`. This is where the five Wally's World PNGs from the last run
  are sitting, unused.
- The engine picks the shape itself: `_p` for portrait, `_s` for square screens (foldables), the plain file for
  landscape, and falls back to the plain file if a shape is missing.

---

## 2. Wiring, after the images are made

In each game:

1. **`app/src/main/assets/game/environments/environments.json`**: replace the list with this game's backgrounds.
   For each one:
   ```json
   {
     "id": "<id>",
     "displayName": "<name from the table>",
     "icon": "<one emoji>",
     "loreLocation": "<where, from the table>",
     "subtitle": "<the scene's short description>",
     "ambientThemeColor": "<hex from the table>",
     "backgroundAsset": "environments/<id>.png",
     "aliases": [],
     "overlayType": "<see below>",
     "artHorizon": 0.55,
     "artHorizonPortrait": 0.55
   }
   ```
   Keep the five legacy names (`forest`, `castle`, `dungeon`, `cave`, `swamp`, upper and lower case) as
   `aliases` on the background named in each game's section, so anything without an `environmentId` still lands
   somewhere sensible.
2. **`app/src/main/assets/game/encounters/encounters.json`**: give each fight `"environmentId": "<id>"` from the
   mapping table. `environmentId` overrides `environment`.
3. Delete the old backdrop files the game no longer names.

### Decide first: the animated layer over the background (`overlayType`)

The battle screen draws an animated layer over every background, picked by `overlayType`, and there are only five,
all fantasy:

| `overlayType` | Draws |
|---|---|
| `CASTLE` | Iron brazier flame and embers, a waving crimson war banner, a lightning flash |
| `DUNGEON` | Torch flare, pillar shadows, glowing cyan runes on a pillar, ground mist |
| `FOREST` | Moonbeams, fireflies and spores, wind in the leaves |
| `CAVE` | Dripping water into a pool, crystal sparkles |
| `SWAMP` | Marsh gas bubbles, will-o'-the-wisps, green haze |

There is no "none". An unknown value falls back to the fight's legacy `environment`. **On a store, an office or a
highway every one of these is wrong**, and it would stay wrong whatever the images look like. This needs an engine
change before the three modern games can look right: an `overlayType` of `NONE` (or new modern overlays such as
flickering fluorescents, rain, or headlights). **Ask Todd before changing the engine.** Until then, `DUNGEON` is
the least wrong for indoor modern places and `FOREST` for outdoor ones.

Echoes of the Logos uses the overlays as they are; its section lists one per background.

---

## 3. Wally's World (*The Squeeze*)

A memoir of 20 years managing big-box retail: Utah, Lamar (Colorado), the South Carolina line, Pueblo, St.
Augustine. The fights are the job's pressures made literal: freight walls, the fluorescent hum, metrics, managers,
the body giving out. Grounded and honest, never fantasy, never glossy. The 3 AM is the darkest point of the story
and must not be softened.

**GAME STYLE:**
> 16-bit SNES-era pixel art battle background for a grounded, melancholy workplace memoir RPG. A real American
> big-box discount superstore and the places around it, drawn with honest working-class detail: concrete,
> linoleum, steel shelving, cardboard, shrink-wrap, scuffed paint. Muted, slightly desaturated palette; light
> comes from real sources (fluorescent tubes, sodium lamps, headlights, dawn) and stays believable. Quiet, heavy
> atmosphere. Empty of people.

**GAME NEGATIVE:**
> people, person, silhouettes, crowds, customers, workers, characters, faces, animals, creatures, monsters, text,
> letters, words, readable signs, logos, brand names, prices, numbers, barcodes, UI, watermark, fantasy, magic,
> castles, jungle, rainforest, palm trees, swamp, tropical, cave, crystals, ruins, medieval, sci-fi, neon cyberpunk,
> glamour, cartoon exaggeration, anime, 3D render, photo, blur, anti-aliasing, JPEG artifacts, props on the floor,
> clutter in the foreground

**Legacy aliases:** `dungeon`→`trench`, `castle`→`sales_floor`, `cave`→`service_bay`, `forest`→`parking_lot`,
`swamp`→`overnight_floor`.

### Backgrounds

| id | Name, where | Theme colour | BACKGROUND SCENE | Portrait line |
|---|---|---|---|---|
| `trench` | Receiving Dock, 4 AM (Backroom Trench) | `#FF9800` | The receiving dock of a superstore at 4 AM. A row of steel roll-up bay doors along the back wall, one rolled open onto black night with the lit back of a 53-foot trailer packed wall to wall with cardboard freight. Yellow-and-black striped dock levelers, rubber dock bumpers, a parked pallet jack at the far edge, shrink-wrapped pallets stacked against the walls. Cold white fluorescent tubes hanging from a high steel-truss ceiling, a faint haze. Sealed grey concrete floor with painted yellow safety lines along the walls. | Tall steel-truss ceiling with hanging tube lights, pallet racking climbing the side walls. |
| `sales_floor` | Sales Floor, daytime (Main Action Alley) | `#2196F3` | The main sales floor of a superstore in daytime. Long aisles of tall steel gondola shelving packed with colourful generic product run away along the back, under a high open ceiling of white steel trusses, ductwork and rows of fluorescent fixtures. Endcaps, blank hanging aisle markers, a stack of paper-towel cases at the far edge. Polished pale linoleum floor reflecting the lights. Bright, flat, relentless light. | Towering shelving and the high truss ceiling with rows of lights. |
| `front_end` | Registers & Service Desk (The Front End) | `#E53935` | The front end of a superstore: a long bank of checkout lanes with conveyor belts and dark register screens along the back, red lane-number lights glowing above them (blank, no numbers), impulse racks and plastic bag carousels. To one side the service desk counter with a dark monitor. Through the glass entrance doors at the far edge, the dark parking lot. Harsh fluorescent glare, polished floor. | Lane lights and ceiling fixtures stacked overhead, the glass front windows above the lanes. |
| `overnight_floor` | Sales Floor After Close | `#5C6BC0` | The same superstore sales floor after midnight, closed and empty. Half the ceiling lights are off, so the aisles fall into long blue-grey shadow with pools of white light. Shelves half emptied, wrapped freight pallets waiting at the ends of aisles, a lone cardboard baler glow in the distance. A quiet, lonely, enormous building. Dim, cold, still. | The dark truss ceiling with only a few lights on, aisles fading into shadow. |
| `service_bay` | Tire & Lube Shop (The Highway Shop) | `#78909C` | The auto service shop of a superstore: a row of service bays along the back with open roll-up doors onto a grey winter day. Two-post hydraulic car lifts (raised, empty), tire racks stacked to the ceiling, red tool chests, coiled air hoses on reels, oil drums and a parts counter at the far edge. Oil-stained, sealed concrete floor kept clean in the middle. Cold daylight from the bay doors mixed with shop fluorescents. | Tire racks and the tall bay doors, hose reels on the high ceiling. |
| `manager_office` | Store Manager's Office | `#8D6E63` | A store manager's office at the end of a back hallway. A cheap laminate desk with a computer monitor glowing, a rolling chair, a wall of laminated corporate posters with only blank shapes and arrows, a whiteboard of blank charts, a filing cabinet, a wall clock. Low ceiling with a buzzing fluorescent panel. Beige walls, institutional carpet tiles on the floor. Tense, airless. | Posters, whiteboard and clock stacked up the wall; low ceiling panel above. |
| `conference_room` | Closed-Door Office (Area & HR office) | `#455A64` | A small corporate meeting room behind a closed door: a long table across the back with an empty chair facing us, a speakerphone in the middle, a box of tissues, a thin stack of papers and a pen, a single framed abstract print, window blinds drawn shut with grey light behind them. Cold overhead panel light. Carpet tile floor. The feeling of an interview that has already been decided. | The closed blinds and the blank wall rising above the empty chair. |
| `community_hall` | Rented Community Hall (The Christmas Party) | `#C62828` | A rented community hall for a store Christmas party: folding tables with white plastic tablecloths along the back, a buffet of foil catering trays, a cheap paper "season's greetings" garland with no letters, a tinsel tree in the corner, a small low stage. Harsh fluorescent ceiling lights with one string of coloured lights that doesn't help. Waxed vinyl tile floor. Festive and hostile at once. | Paper garlands and tinsel hanging from the high ceiling over the tables. |
| `parking_lot` | Store Parking Lot, night (Store Perimeter) | `#546E7A` | A superstore parking lot at 11 PM. The lit glass entrance and blank sign of the store across the back, tall sodium lamp posts casting orange pools, a cart corral, a few parked pickups at the far edges, a cold wind, a dark sky. Dry asphalt with faded painted lines running across, not toward us. Uneasy, exposed. | Tall lamp posts and the dark night sky above the store front. |
| `moving_day` | Leaving Town (The Body Keeps the Score) | `#6D4C41` | Night at a small rented house: a box truck parked along the back with its roll-up door open, half full of moving boxes and a mattress, more taped boxes on the porch, a single porch light, the dark street and pine trees behind. Dry driveway concrete. A late, exhausted, painful kind of quiet. | Tall pine trees and night sky above the house and truck. |
| `interstate_night` | Interstate 95 at night (The Asphalt Gauntlet) | `#F57C00` | An interstate highway at night seen from the shoulder: lanes of red tail lights and white headlights streaking along the back, orange-and-white construction barrels, concrete jersey barriers, a blank green overhead gantry sign, sodium lights fading into the distance. The roadway runs left to right across the image. Black sky, wet-looking but flat asphalt. Endless and draining. | The overhead gantry sign and tall highway lights against the black sky. |
| `backroom_3am` | The Backroom at 3 AM (The Ownerless Building) | `#37474F` | A cavernous superstore backroom at 3 AM: tall steel pallet racks disappearing up into darkness, a single fluorescent tube flickering overhead, a time clock on the wall, the green glow of an exit door at the far end of a long hallway. Deep black shadows and one cold pool of light on the bare concrete. Silent, heavy, desperate, alone. The darkest image of the game; keep it serious and still, no horror imagery. | The racks rising into total darkness above the one light. |
| `bay_sunrise` | The Shop Bay at Sunrise (The Balance Sheet) | `#FFB74D` | The same auto service shop bay at dawn: the roll-up door open on a clear sunrise, warm gold light pouring in across the shop, a car lift lowered, tools put away, dust floating in the light. Clean sealed concrete floor glowing gold. Calm, earned, unbroken. | Bright dawn sky above the open bay door. |

### Fight mapping

| Fight id | Fight | Background |
|---|---|---|
| `trench_unload` | The 4 AM Unload | `trench` |
| `floor_war` | Front-End Storm | `front_end` |
| `mattie_t_showdown` | The Manager's Office | `manager_office` |
| `lamar_oil_rush` | The Friday Oil Rush | `service_bay` |
| `purge_audit` | The Purge Audit | `sales_floor` |
| `brick_wall_hearing` | The Brick Wall | `conference_room` |
| `jury_showdown` | The Christmas Party Slur | `community_hall` |
| `shingles_fire` | The Body Keeps the Score | `moving_day` |
| `carolina_neglect` | The Neglected Bays | `service_bay` |
| `carolina_gated_tour` | The Executive Walkthrough | `sales_floor` |
| `carolina_paralysis` | Store Manager Azalia | `manager_office` |
| `carolina_fall_guy` | The Scapegoat Investigation | `conference_room` |
| `carolina_regional_desk` | The Regional Desk | `conference_room` |
| `shingles_fire_two` | The Body Keeps the Score (Again) | `moving_day` |
| `pueblo_front_storm` | The Front-End Swarm | `front_end` |
| `pueblo_one_man_close` | The One-Man Close | `overnight_floor` |
| `pueblo_blotter_ring` | The Blotter Regulars | `front_end` |
| `pueblo_claim_blizzard` | The Claim Blizzard (not triggered by the story yet) | `conference_room` |
| `pueblo_parking_draw` | The Parking Lot Draw | `parking_lot` |
| `pueblo_freight_to_floor` | The Freight-to-Floor | `trench` |
| `pueblo_compliance_visit` | The Compliance Walkthrough | `sales_floor` |
| `pueblo_paradox` | Store Manager Jay-Dau | `manager_office` |
| `pueblo_amandy_storm` | Store Lead Amandy | `manager_office` |
| `pueblo_graveyard` | The Overnight Meat Grinder | `overnight_floor` |
| `pueblo_shingles_three` | The Body Keeps the Score (Third Entry) | `overnight_floor` |
| `pueblo_relocation_contract` | The Relocation Contract | `conference_room` |
| `florida_interstate` | The Asphalt Gauntlet | `interstate_night` |
| `florida_green_metrics` | The Green Meters | `overnight_floor` |
| `florida_morning_verdict` | Sriniviii's Morning Verdict | `manager_office` |
| `florida_ohmar_interrogation` | Ohmar's Formal Office | `conference_room` |
| `florida_hostage_hours` | The Ownerless Building | `backroom_3am` |
| `florida_shingles_four` | The Fourth Signal | `backroom_3am` |
| `florida_redemption_schedule` | The Redemption Schedule | `conference_room` |
| `florida_three_am` | The 3 AM | `backroom_3am` |
| `florida_parting_shot` | The Parting Shot | `backroom_3am` |
| `act6_pay_cut` | The Pay Cut | `sales_floor` |
| `act6_abdoolie_ghost` | Store Manager Abdoolie | `manager_office` |
| `act6_customer_gauntlet` | The Customer Gauntlet | `front_end` |
| `act6_phantom_threat` | The All-Hands Inquisition | `service_bay` |
| `act6_final_balance` | The Final Balance Sheet | `bay_sunrise` |

---

## 4. Superstore Saga

A comedic retail RPG at MegaMart Store #1042: the player picks a department (cashier, stocker, unloader,
electronics specialist), works shifts, and fights rushes, coupon crowds, sensor domes, Loss Prevention and
manager Henderson, ending in a store lockdown. Brighter and funnier than Wally's World, but still a real store.

**GAME STYLE:**
> 16-bit SNES-era pixel art battle background for a comedic retail workplace RPG set in a huge suburban discount
> megastore. Clean, bright, readable, slightly satirical: glossy floors, primary-colour corporate décor in royal
> blue and yellow, endless shelves, fluorescent light. Vivid but not neon; playful without being cartoonish.
> Empty of people.

**GAME NEGATIVE:**
> people, person, silhouettes, crowds, customers, employees, characters, faces, animals, creatures, monsters,
> text, letters, words, readable signs, logos, brand names, prices, numbers, barcodes, UI, watermark, fantasy,
> magic, castles, jungle, rainforest, palm trees, swamp, cave, crystals, ruins, medieval, sci-fi, horror, gore,
> anime, 3D render, photo, blur, anti-aliasing, JPEG artifacts, props on the floor, clutter in the foreground

**Legacy aliases:** `dungeon`→`checkout_lanes`, `castle`→`manager_suite`, `cave`→`loading_dock`,
`forest`→`action_alley`, `swamp`→`locker_room`.

### Backgrounds

| id | Name, where | Theme colour | BACKGROUND SCENE | Portrait line |
|---|---|---|---|---|
| `checkout_lanes` | Front-End Concourse | `#1E88E5` | The front-end concourse of a megastore: a long row of checkout lanes along the back with conveyor belts, card readers, blank glowing lane-number lights above each lane, candy and magazine racks, stacked plastic baskets. The glass entrance and sunny parking lot beyond the far edge. Bright fluorescent light, glossy white floor. Busy-looking even when empty. | Lane lights and big blank blue-and-yellow banners hanging from the high ceiling. |
| `action_alley` | Center Action Alley (Sales Floor) | `#FBC02D` | The main sales-floor aisle of a megastore: towers of paper towels and soda cases, endcap displays, a yellow milk crate, long grocery aisles of colourful generic packaging behind, a baby-formula aisle with security peg hooks and small black ceiling camera domes. High white ceiling with rows of lights. Glossy linoleum floor. | Shelving towers and the high ceiling with camera domes and lights. |
| `loading_dock` | Backroom Receiving Dock | `#FF8F00` | The receiving dock of a megastore: roll-up bay doors along the back, a dock plate, a parked forklift and a battered pallet jack at the far edge, stacks of shrink-wrapped pallets of bottled water, a conveyor rollerline, a small dock office window. Warm sodium and fluorescent light, sealed concrete floor with yellow safety lines along the walls. | Tall pallet racking and the steel ceiling above the bay doors. |
| `electronics_counter` | Electronics Department | `#7E57C2` | The electronics department of a megastore: a glass locked display case counter across the back full of blank game boxes and gadgets, a glowing wall of TVs showing soft abstract colour, a headphone wall of peg hooks, blue LED accent lighting. Dark grey carpet tile floor. Cool, shiny, slightly smug. | The TV wall stacked high, blue light glowing upward. |
| `security_office` | Loss Prevention Office | `#424242` | A windowless loss-prevention security office: a wall of small CCTV monitors showing grey static and blank aisles, video recorders blinking, a steel table with a single typed sheet and a lamp, a heavy metal door. Harsh lamp light and cold monitor glow. Grey concrete floor. Claustrophobic. | The monitor wall stacked up to the ceiling. |
| `locker_room` | Associate Locker Room | `#607D8B` | The associate locker room of a megastore: a long row of battleship-grey metal lockers along the back, a bench, a time clock, a corkboard of blank flyers. Plain fluorescent light, an ozone smell feel. Grey vinyl floor. | Lockers and a high bare wall with a single vent and light. |
| `manager_suite` | Store Manager Executive Suite | `#3949AB` | The store manager's executive office: a big desk with an empty leather chair, framed corporate awards and plaques with blank faces, a dying office plant, a wide interior window overlooking the bright sales floor below. Shredded paper scraps by the desk, a red duffel bag at the far edge. Warm desk lamp against cold office light. Carpeted floor. | Award plaques climbing the wall, the big window above the desk. |
| `cash_office` | The Cash Office | `#2E7D32` | A small cash office: a heavy steel safe, a cash-counting machine, stacks of zippered deposit bags and drop slips on a counter, a barred pass-through window, a lone overhead light. Grey tile floor. Tense and secret. | The safe and wall shelves rising to a low ceiling light. |
| `server_room` | The Server Room | `#00897B` | A cramped store server room: racks of blinking network equipment and cables along the back, a single monitor with an abstract progress bar, cooling vents, blue and green status lights. Raised grey floor tiles. Humming and cold. | Server racks to the ceiling, cable trays overhead. |
| `breakroom` | Associate Breakroom | `#A1887F` | The associate breakroom: a humming vending machine glowing, stained vinyl chairs at a plastic table, a microwave, a coffee maker, a corkboard of blank notices, a wall clock. Tired fluorescent light. Speckled tile floor. | Vending machine and cabinets reaching the low ceiling. |
| `lockdown_floor` | The Store in Lockdown (Aisle 4) | `#B71C1C` | The megastore sales floor during an after-hours lockdown: main lights off, red emergency lights and the white beam of security lights cutting through dark aisles, shelves in deep shadow, a metal security shutter half down at the far end. Glossy floor catching red reflections. Tense and dramatic. | Dark shelving towers and red emergency lights on the high ceiling. |
| `lockdown_backhall` | Back-of-House Hallway in Lockdown | `#C62828` | The back-of-house hallway of a megastore during a lockdown: a long corridor of doors along the back (dock office, cash office, manager's suite), all blank, a glowing red emergency light and an exit sign with no letters, a dropped clipboard of forms near the wall. Dim red light. Grey vinyl floor. Final, cornered. | The corridor ceiling with emergency lights and pipes above the doors. |

### Fight mapping

| Fight id | Fight | Background |
|---|---|---|
| `encounter_register_rush` | Morning Concourse Rush Rally | `checkout_lanes` |
| `encounter_auditor_standoff` | Regional Audit Standoff | `action_alley` |
| `encounter_saturday_rush_wave` | Saturday Community Festival Rush | `checkout_lanes` (see department note) |
| `encounter_regional_showcase` | District VP Grand Inspection Showcase | `action_alley` |
| `encounter_slacker_showdown` | Department Huddle Confrontation | `action_alley` |
| `encounter_loss_prevention_standoff` | Loss Prevention Interrogation Standoff | `security_office` |
| `encounter_henderson_showdown` | The Executive Lockdown Showdown | `manager_suite` |
| `encounter_screamer_jack` | The Screamer Breaks Loose | `loading_dock` |
| `encounter_clopening_coupons` | Clopening Coupon Charge | `checkout_lanes` |
| `encounter_showcase_bangers` | Locked Case Riot | `electronics_counter` |
| `encounter_go_back_golem` | The Go-Back Golem | `action_alley` (see department note) |
| `encounter_forklift_cowboy` | Forklift Cowboy | `loading_dock` |
| `encounter_code_spark` | Code Spark | `checkout_lanes` (every department is called to the front) |
| `encounter_sensor_meltdown` | Sensor Meltdown | `action_alley` (see department note) |
| `encounter_ap_shakedown` | Asset Protection Shakedown | `locker_room` |
| `encounter_lockdown_dock` | Lockdown: The Dark Office | `lockdown_backhall` |
| `encounter_lockdown_aisle4` | Lockdown: Aisle 4 | `lockdown_floor` |
| `encounter_lockdown_cash_office` | Lockdown: The Cash Office | `cash_office` |
| `encounter_lockdown_server` | Lockdown: The Server Room | `server_room` |
| `encounter_last_line` | Henderson's Last Line | `lockdown_backhall` |

`breakroom` is not used by a fight yet; it's in the set because the story spends time there and the next fights
(department friction, break-time ambushes) will want it. Skip it if images are tight.

**Department note.** Three fights are written per department and happen in the player's own department. The
engine takes a fight's background from the encounter only, not from the story line that starts it, so for now each
uses the one background above. To match the story exactly, either the engine gets a per-node background override,
or these three fights are split into one encounter per department. **Ask Todd which.** The right background per
department:

| Fight | Cashier | Stocker | Unloader | Specialist |
|---|---|---|---|---|
| Saturday Festival Rush | `checkout_lanes` | `action_alley` | `loading_dock` | `electronics_counter` |
| The Go-Back Golem | `checkout_lanes` | `action_alley` | `loading_dock` | `electronics_counter` |
| Sensor Meltdown | `checkout_lanes` | `action_alley` | `loading_dock` | `electronics_counter` |

---

## 5. Commander America

A two-track political roast JRPG, 2015 to 2026: debate stages, rallies, the Capitol, hearing vaults, donut shops,
ice-cream counters, drive-thrus and the grand finale. Satire; the places are the joke's setting, the people are
the sprites.

**GAME STYLE:**
> 16-bit SNES-era pixel art battle background for a satirical political comedy JRPG. Vivid, saturated, punchy
> colour; bold clean shapes; a stage-like, larger-than-life version of a real American place, played for comedy
> but drawn with care. Spotlights, bunting, gold and marble where it fits. Empty of people.

**GAME NEGATIVE:**
> people, person, silhouettes, crowds, audience, politicians, characters, faces, caricatures, animals, creatures,
> monsters, text, letters, words, readable signs, logos, brand names, campaign slogans, flags with text, numbers,
> UI, watermark, fantasy castle, jungle, swamp, cave, crystals, medieval, horror, gore, anime, 3D render, photo,
> blur, anti-aliasing, JPEG artifacts, props on the floor, clutter in the foreground

**Legacy aliases:** `castle`→`debate_stage`, `forest`→`national_mall`, `dungeon`→`secure_vault`,
`cave`→`gilded_storage`, `swamp`→`coconut_grove`.

### Backgrounds

| id | Name, where | Theme colour | BACKGROUND SCENE | Portrait line |
|---|---|---|---|---|
| `golden_atrium` | Gold Escalator Atrium, Manhattan | `#FFC107` | A gleaming Manhattan tower atrium: a grand brass-gold escalator descending along the back, pink-and-gold marble walls, a waterfall wall, velvet ropes, a row of TV news cameras on tripods aimed at the escalator foot. Blinding camera lights. Polished marble floor. | The escalator climbing into a towering gold atrium. |
| `debate_stage` | Primary Debate Stage | `#D32F2F` | A televised presidential debate stage: a row of empty podiums across the back under hot spotlights, a giant blank red-white-and-blue video wall, stars-and-stripes bunting, camera cranes at the edges. Glossy dark stage floor. | Lighting rigs and the giant video wall stacked overhead. |
| `talk_show_set` | Two-Fern Web Talk Show Set | `#8D6E63` | A deliberately cheap web talk-show set: two tired chairs in front of a black curtain, a potted fern on either side, one bad lamp, a tiny camera and boom mic at the edge. Awkward and underlit. Scuffed black floor. | The black curtain rising to a single bare studio light. |
| `rally_stage` | Outdoor Rally Stage, Fairgrounds | `#E53935` | An outdoor campaign rally stage at a fairground: a raised stage along the back with a podium and bulletproof glass panels, rows of flags without text, bunting, speaker towers, a parked futuristic angular steel pickup truck with cracked windows at the far edge. Bright afternoon sky, flat trampled grass and dirt ground. | Speaker towers, flags and a big sky above the stage. |
| `national_mall` | The National Mall & White House Hedges | `#1976D2` | The National Mall in January: a white domed capitol far away at the back, bare trees, a long reflecting pool, and close by at the edges neatly clipped dark green hedges big enough to hide in. Crisp cold blue sky. Flat paved plaza ground. | The capitol dome and the tall Washington obelisk against the sky. |
| `house_chamber` | House Chamber | `#5D4037` | A grand legislative chamber: the tiered marble rostrum with a gavel and a large blank seal shape across the back, tall marble columns, a huge flag draped behind (stripes only), gold trim, and on the far edge a pair of shiny stainless-steel luxury freezers. Warm chandelier light. Rich patterned carpet floor. | Marble columns and a coffered ceiling with chandeliers. |
| `supply_gym` | Hurricane Relief Gym | `#0288D1` | A school gym turned hurricane relief supply centre: pallets of paper towel rolls and water cases along the back, a basketball hoop, folding tables, and on an easel a big weather map poster with a hand-drawn black marker loop. Bright overhead gym lights. Polished wood court floor. | The basketball hoop and high gym rafters with banners (no text). |
| `secure_vault` | The Secure Vault (SCIF / Records) | `#455A64` | A sealed government secure room and records vault: a massive round vault door open along the back, walls of grey filing cabinets and bankers boxes, a big red three-ring binder and a document stamped with blank shapes on a table, redaction black bars on papers pinned to a board, a meme-dog coin emblem shape on one wall. Cold fluorescent light. Grey carpet floor. | Filing shelves and boxes stacked to the ceiling. |
| `landscaping_lot` | Total Landscaping Parking Lot, Philadelphia | `#689F38` | A small industrial strip in Philadelphia: a modest garage door of a landscaping business along the back with blank campaign signs taped on it, a small podium in front, a crematorium building on one side and a seedy little bookshop on the other (both blank signs), a parked riding mower. Grey overcast day. Cracked parking-lot asphalt. | Power lines and an overcast sky above the low buildings. |
| `airport_tarmac` | Airport Tarmac & Air Stairs | `#90A4AE` | An airport tarmac on a windy day: a large blue-and-white presidential-style jet along the back with a tall rolling air staircase, windsocks blowing, a parked bicycle with toe-cage pedals at the far edge. Blowing streaks in the bright sky. Flat concrete apron. | The tall staircase rising to the jet door and the windy sky. |
| `beach_trail` | Beach Bike Trail & Community Pool | `#4DB6AC` | A beach-town bike trail beside a community swimming pool: a chain-link pool fence with a lifeguard chair behind, dunes and sea grass, a rain barrel, a bike rack. Summer sun. Flat paved trail ground. | Tall lifeguard chair and a wide summer sky. |
| `gilded_storage` | Gilded Resort Ballroom & Box Storage | `#FFD54F` | A gilded resort's over-the-top ballroom-bathroom: crystal chandeliers, gold mirrors and marble, with towers of cardboard bankers boxes stacked everywhere along the back, and through an arch a garage with a classic car under a cover. Warm gold light. Shiny marble floor. | Chandeliers and towering box stacks under a painted ceiling. |
| `booking_room` | County Jail Booking Room | `#546E7A` | A county jail booking room: a height-chart wall with plain lines (no numbers) and a camera on a tripod across the back, a booking desk with a fingerprint pad, harsh flat light, and at the far edge a small T-shirt press. Grey tiled floor. | The height chart and blank wall rising to bright ceiling lights. |
| `ice_cream_parlor` | Ice-Cream Parlor Counter, Portland | `#EC407A` | A bright ice-cream parlor: a long glass freezer counter of colourful tubs along the back, a waffle-cone display, pink and brown décor, neon tube outlines (no letters), and at the edge a teleprompter on a stand. Cheerful light. Checkerboard tile floor. | Menu boards (blank) and neon shapes up to the ceiling. |
| `donut_shop` | Small-Town Donut Shop, Georgia | `#FF7043` | A small-town donut shop: a glass display case of sprinkle donuts along the back, a coffee pot, a register, bakery racks, buzzing fluorescent lights. A wedge of Swiss cheese on a plate on the counter for the joke. Warm and plain. Speckled tile floor. | Bakery racks and blank menu boards up to the ceiling. |
| `coconut_grove` | The Coconut Grove (Venn Mirage) | `#43A047` | A surreal tropical coconut grove: tall palms with coconuts along the back, three giant translucent overlapping Venn-diagram circles glowing in yellow, blue and green hovering in the sky, a golden throne-like chair alone among the trees. Dreamlike late-afternoon light. Flat sandy ground. | Towering palms and the glowing Venn circles high in the sky. |
| `drive_thru` | Fast-Food Drive-Thru & Garbage Truck | `#FB8C00` | A fast-food drive-thru: the service window and fry station glowing along the back, golden fry baskets, a menu board with blank panels, and parked at the far edge a bright garbage truck with a safety vest hanging on the mirror. Evening sky. Flat drive-thru asphalt with painted lines running across. | The tall menu board and a pole sign (blank) against the evening sky. |
| `convention_floor` | National Convention Floor | `#C62828` | A national party convention floor: a huge stage along the back with giant video screens, balloon nets in the rafters, state delegation signposts (blank), red-white-and-blue lights, a giant meme-dog coin banner shape and budget spreadsheet graphics on a screen. Arena lighting. Flat carpeted floor. | Balloon nets and arena rafters over the stage. |
| `airport_lounge` | Luxury Airline Lounge & Mansion Kitchen | `#00838F` | A luxury airline business lounge: lie-flat seat pods along the back, a buffet with a plate of sea bass, big windows onto a runway at night, and through an arch a stately mansion kitchen with a marble jar on the counter. Soft upscale light. Polished floor. | Tall windows onto the night runway and lounge lights. |
| `queens_street` | Queens Street, Food Truck & Bus Stop | `#F9A825` | A Queens, New York street: a colourful chapati food truck with its window open along the back, a bus shelter, a subway entrance with a turnstile, brick apartment buildings, fire escapes. Sunny day. Flat sidewalk and street ground. | Brick apartments with fire escapes and an elevated train track above. |
| `bus_depot` | Electric School Bus Depot | `#FDD835` | An electric school bus depot: a row of yellow school buses plugged into charging posts along the back, overhead cables, a big battery unit, three faint glowing Venn circles in the sky, a glitching teleprompter on a stand at the edge. Grey daylight. Flat concrete lot. | Charging gantries and the sky with the faint circles. |
| `finale_arena` | The Bipartisan Grand Finale Stage | `#FFD700` | The grand finale stage: a huge golden stage along the back with red and blue curtains swept open, golden spotlights crossing, confetti, waffle cones, chapatis and silver balls frozen mid-air in the light, fireworks behind. Triumphant and absurd. Glossy stage floor. | Confetti, fireworks and spotlights filling the tall space above. |

### Fight mapping

| Fight id | Fight | Background |
|---|---|---|
| `act1_ch1_press_battle` | Escalator Press Confrontation | `golden_atrium` |
| `act1_ch2_debate_battle` | The Primary Debate Arena | `debate_stage` |
| `act1_track_a_barry_battle` | Tan Suit & Dijon Exhibition | `debate_stage` |
| `act1_track_a_hill_rod_finale` | The Chappaqua Server Sovereign | `debate_stage` |
| `act1_track_b_ferns_battle` | Between Two Potted Ferns | `talk_show_set` |
| `act1_track_b_commander_finale` | Golden Podium Showdown | `rally_stage` |
| `act2_ch1_inauguration_battle` | Crowd-Size Spin & The 11-Day Mooch | `national_mall` |
| `act2_media_battle` | Nancy Palooza & The Sub-Zero Vault | `house_chamber` |
| `act2_track_a_sharpie_battle` | Hurricane Sharpie & 15 Flushes | `supply_gym` |
| `act2_track_b_spicer_salsa_battle` | White House Bushes & Neon Salsa | `national_mall` |
| `act2_track_b_kash_battle` | Wizard Kash & The Nunes Scroll | `secure_vault` |
| `act2_track_b_rudy_finale` | Four Seasons Total Landscaping | `landscaping_lot` |
| `act3_track_a_stairs_bike_battle` | Windy Stairs & Toe-Cage Bike | `airport_tarmac` |
| `act3_track_a_chandelier_battle` | Bathroom Chandeliers & Corvette Boxes | `gilded_storage` |
| `act3_track_a_biden_ice_cream_finale` | Baskin-Robbins Waffle Cone Showdown | `ice_cream_parlor` |
| `act3_track_b_corn_pop_battle` | Corn Pop & The Toe-Cage Bicycle | `beach_trail` |
| `act3_track_b_foghorn_battle` | Foghorn Kennedy's Oxford Drawl | `ice_cream_parlor` |
| `act3_track_b_mugshot_finale` | 215-Pound Mugshot & Bathroom Chandelier | `booking_room` |
| `act4_ch1_fisa_battle` | Archivist Phantoms | `secure_vault` |
| `act4_track_a_donut_battle` | 'Whatever Makes Sense' Donuts & Swiss Cheese | `donut_shop` |
| `act4_track_b_venn_critics_battle` | Coconut Context & Venn Circles | `coconut_grove` |
| `act4_track_b_vance_donut_battle` | JD Pants & 'Whatever Makes Sense' | `donut_shop` |
| `act4_track_b_concepts_debate_battle` | 'Concepts of a Plan' & 'People on TV' | `debate_stage` |
| `act4_track_b_mcdonalds_finale` | Drive-Thru Fry Basket & Garbage Vest | `drive_thru` |
| `act5_track_a_cybertruck_sink_battle` | Mid-Air Star-Jump & Porcelain Sink | `rally_stage` |
| `act5_track_a_doge_bureaucrats_battle` | Hulkster Shirt-Rip & DOGE Audit | `convention_floor` |
| `act5_track_a_swagger_turkish_battle` | Mayor Swagger & Turkish Lie-Flat Suites | `airport_lounge` |
| `act5_track_a_grand_finale` | Mr. Cardamom & The All-Star Relay | `finale_arena` |
| `act5_track_b_landlord_lobby_battle` | 'Nani' Chapati Truck & Rent Freeze | `queens_street` |
| `act5_track_b_turkish_auditor_battle` | 6-Digit Passcodes & Chilean Sea Bass | `airport_lounge` |
| `act5_track_b_musk_starjump_battle` | Mid-Air 'X' Star-Jump & Porcelain Sink | `rally_stage` |
| `act5_track_b_doge_squad_battle` | Donuts, Wizard Books & Oxford Drawls | `secure_vault` |
| `act5_track_b_grand_finale` | Golden Podium & Dark Gothic MAGA | `finale_arena` |
| `act6_ch3_bus_battle` | The Electric School Bus Mire (triggers in Act IV) | `bus_depot` |
| `act6_kamala_battle` | The Word-Salad Wasteland (triggers in Act IV) | `coconut_grove` |

---

## 6. Echoes of the Logos

Romantic high-fantasy JRPG: a world whose voices were stolen into silence, and an Invocator who fights by speaking.
The five current backgrounds are the right genre but too few: the celestial stair fights in a forest, the void
reservoir in a swamp, a gothic bell tower in a generic courtyard. Each place below is a story location.

**GAME STYLE:**
> 16-bit SNES-era pixel art battle background for a romantic high-fantasy JRPG, in the tradition of Chrono Trigger
> and Final Fantasy VI. Lush, painterly pixel art with rich colour and atmospheric light, ancient stonework, living
> nature, and a sense of a world whose voice has been stolen: obsidian crystal blight and eerie silence creeping
> into beautiful places. Empty of people.

**GAME NEGATIVE:**
> people, person, silhouettes, characters, faces, knights, monsters, creatures, animals, statues with faces in the
> foreground, text, letters, runes that read as words, UI, watermark, modern elements, buildings from today,
> vehicles, electric lights, photo, 3D render, anime, blur, anti-aliasing, JPEG artifacts, props on the floor,
> clutter in the foreground

**Legacy aliases:** `forest`→`sun_shrine`, `castle`→`bell_chamber`, `dungeon`→`foundation_crypt`,
`cave`→`crystal_caverns`, `swamp`→`rotting_marsh`.

### Backgrounds

| id | Name, where | Overlay | Theme colour | BACKGROUND SCENE | Portrait line |
|---|---|---|---|---|---|
| `blighted_village` | Whispering Pines (Blighted) | `FOREST` | `#78909C` | A small pine village square at dawn, wrapped in eerie grey mist: thatched cottages along the back, a dry stone well, and villagers' shapes frozen into black obsidian crystal (rough crystal forms, no faces). Pale cold light. Packed-earth square. | Tall misty pines and cottage roofs fading into fog. |
| `sun_shrine` | The Sun Shrine Crossroads | `FOREST` | `#FFB300` | A forest crossroads under towering ancient pines: a weathered stone sun shrine with a golden sun disc along the back, moss-covered waymarkers, a faint campfire glow at one edge. Warm late-day light through the trees. Flat mossy clearing. | Towering pine trunks and god-rays from far above. |
| `crystal_caverns` | The Whispering Caverns | `CAVE` | `#26C6DA` | A vast limestone cavern: bioluminescent azure crystals growing along the back walls, stalactites, a still dark pool at one edge, a carved stone buttress. Glowing blue light. Flat damp stone floor. | Stalactites and crystal clusters reaching into the high dark vault. |
| `foundation_crypt` | Crypt of the Foundation | `DUNGEON` | `#8D6E63` | Ancient catacombs beneath a bell tower: mosaic pillars of chanting figures worn to abstract patterns, granite tomb niches along the back, a desecrated shrine altar, wall torches. Warm torchlight in deep shadow. Worn flagstone floor. | Massive foundation arches vanishing up into darkness. |
| `bell_chamber` | The Solaria Bell Chamber | `CASTLE` | `#42A5F5` | A gothic bell tower chamber above the clouds: a colossal bronze bell hanging under open pointed arches along the back, a sea of cloud and blue sky beyond, iron braziers, banners. Bright high-altitude light. Flat stone platform floor. | The enormous bell and its beams filling the tall vaulted tower. |
| `rotting_marsh` | The Rotting Marsh | `SWAMP` | `#558B2F` | A black mire under thick emerald mist: gnarled roots and twisted willows along the back, sickly glowing pools, rotting reeds. Green, poisoned light. Flat firm mud bank. | Twisted willows and hanging moss climbing into the mist. |
| `drowned_fane` | The Drowned Fane & Sunken Catacombs | `SWAMP` | `#00897B` | A flooded gothic temple courtyard: sunken pointed-arch pillars rising from murky emerald water along the back, weeping willow roots through the stones, a jade-bronze chime hanging in a broken arch, an empty cage of black thorny briars. Soft green light. Flat raised stone causeway floor. | Broken temple arches and willow canopy towering above the water. |
| `shadowed_crags` | The Shadowed Crags & The Blind Gorge | `CAVE` | `#7E57C2` | A cold canyon of razor black obsidian crags shrouded in purple mountain mist: sheer cliffs along the back, a narrow sky, rope bridges and dark culverts in the rock. Dusky violet light. Flat rocky canyon floor. | Sheer crags rising to a thin strip of sky. |
| `sun_mausoleum` | The Mausoleum of the Sun | `DUNGEON` | `#FFCA28` | A great mausoleum: shattered marble knight statues (broken, faceless) along the back beneath torn golden sunburst banners, a sealed crypt gate wreathed in cold blue fire. Gold light falling through a broken dome. Cracked marble floor. | The broken dome with light shafts high above the banners. |
| `choir_grove` | The Emerald Choir Grove | `SWAMP` | `#66BB6A` | A sacred grove where tree-spirits stand petrified as dryad-shaped wood and stone (no faces), circling a dark spring choked with black silt, colossal ferns and glass-like vines. Muted green and gold light. Flat mossy ground. | Colossal ferns and a canopy of glassy vines overhead. |
| `obsidian_vaults` | The Obsidian Vaults of the Black Guild | `DUNGEON` | `#4E342E` | A subterranean guild reliquary: black stone shelves of lead-sealed jars and heavy ledgers along the back, chains, iron candelabras, a vault door. Dim candlelight and deep purple shadow. Polished black floor. | Shelves of jars and ledgers climbing into darkness. |
| `clockwork_belfry` | The Clockwork Bastion of Ouros | `CASTLE` | `#BF8F00` | A towering iron belfry full of colossal brass cogs and steam pipes along the back, a catwalk, a furnace glow, a great iron bell. Hissing steam and orange furnace light. Flat riveted iron platform floor. | Giant turning cogs and pipes stacked up the tower. |
| `citadel_gates` | The Silent Citadel Gates & Pilgrim's Plaza | `CASTLE` | `#37474F` | A monolithic citadel of black glass battlements along the back, a gatehouse, banners of a mute sovereign (plain shapes), and a pilgrim's plaza with candle stalls and lamp posts. Night with cold lamp light. Flat paved plaza. | The black glass towers rising into a starless sky. |
| `void_reservoir` | The Void Reservoir | `SWAMP` | `#283593` | A cosmic lake of liquid silence high above the clouds: a mirror-still black-violet lake along the back reflecting stars, drifting cloud banks, ruined pilgrim shrines on the shore. Starlight, eerie and vast. Flat pale stone shore. | A sky full of stars and cloud above the still lake. |
| `umbral_trench` | The Umbral Trench | `CAVE` | `#1A237E` | A buried chamber at the bottom of a deep trench: walls of silt and old stone along the back, faint glowing motes rising, coils of dark shapes in the walls, starlight barely reaching down. Very dark blue. Flat silt floor. | The trench walls narrowing up to a faint glimmer of light. |
| `celestial_stair` | The Celestial Ribbon Stair | `CASTLE` | `#B39DDB` | A ribbon staircase of crystallised harmonic light rising through the aurora along the back, three great gates floating on its landings: one of gold, one of living roots, one of shadow. Aurora colours across a night sky. Flat crystal landing floor. | The stair spiralling up through the aurora toward the stars. |
| `vestibule_echoes` | The Vestibule of Echoes | `DUNGEON` | `#E0E0E0` | The final sanctum: four tall pillars of glass in a ring along the back, each holding a trapped glow of song, rings of kneeling frozen supplicants as rough stone shapes (no faces), a dais with a dark glass monolith, shattered mirror shards hanging in the air. White and void-violet light. Flat polished stone floor. | The glass pillars and monolith towering into a dark vault. |

### Fight mapping

| Fight id | Fight | Background |
|---|---|---|
| `prologue_solo` | Prologue: Whispers in the Fog | `blighted_village` |
| `forest_ambush` | Chapter 1: The Old Way Shrine | `sun_shrine` |
| `blight_trackers` | Camp Reconnaissance: The Blighted Prowlers | `sun_shrine` |
| `cave_broodmother` | Boss Trial: The Chittering Queen | `crystal_caverns` |
| `ch3_sentinels` | Chapter 3: The Corrupted Sentinels | `crystal_caverns` |
| `swamp_behemoth` | Chapter 6: The Weeping Willow Sanctuary | `rotting_marsh` |
| `dungeon_descent` | Chapter 4: Crypt of the Restless | `foundation_crypt` |
| `castle_horde` | Chapter 4: The Solaria Bell Chamber | `bell_chamber` |
| `marsh_rescue` | Chapter 5: The Drowned Fane Rescue | `drowned_fane` |
| `ch7_mire_wyrm` | Chapter 7: Guardian of the Silt Core | `drowned_fane` |
| `ch8_executioner_ambush` | Chapter 8: The Shadowed Crags Ambush | `shadowed_crags` |
| `ch9_galahault_trial` | Chapter 9: The Broken Vow of Dawn | `sun_mausoleum` |
| `ch9_penitent_gate` | Chapter 9: Gate of the Sun's Penitence | `sun_mausoleum` |
| `ch10_broodmother_trial` | Chapter 10: The Song of the Mute Grove | `choir_grove` |
| `ch10_thicket_guardians` | Chapter 10: Guardians of the Poisoned Thicket | `choir_grove` |
| `ch11_nocturne_trial` | Chapter 11: The Silent Blade's Reckoning | `obsidian_vaults` |
| `ch11_archive_enforcers` | Chapter 11: The Obsidian Archive | `obsidian_vaults` |
| `ch11_gorge_stalkers` | Chapter 11: The Gorge Stalker Skirmish | `shadowed_crags` |
| `ch12_warmaster_ouros` | Chapter 12: The Iron Belfry of Ouros | `clockwork_belfry` |
| `ch12_automaton_patrol` | Chapter 12: The Automatic Watch | `clockwork_belfry` |
| `ch13_commander_vaelor` | Chapter 13: Breach of the Silent Citadel | `citadel_gates` |
| `ch13_plaza_legion` | Chapter 13: The Pilgrim's Plaza Legion | `citadel_gates` |
| `ch14_abyssal_leviathan` | Chapter 14: The Void Reservoir | `void_reservoir` |
| `ch14_shallows_husks` | Chapter 14: Husks of the Silent Shallows | `void_reservoir` |
| `ch14_trench_leeches` | Chapter 14: The Umbral Trench | `umbral_trench` |
| `ch15_archon_custodians` | Chapter 15: Ascent of the Celestial Spire | `celestial_stair` |
| `ch15_trial_gold` | Chapter 15: Trial of the Gate of Gold | `celestial_stair` |
| `ch15_trial_grove` | Chapter 15: Trial of the Gate of the Grove | `celestial_stair` |
| `ch15_trial_shadow` | Chapter 15: Trial of the Gate of Shadow | `celestial_stair` |
| `ch16_nullifier_gate` | Chapter 16: The Echo Nullifier Gate | `vestibule_echoes` |
| `ch16_mirror_gauntlet` | Chapter 16: The Mirror Gauntlet | `vestibule_echoes` |
| `ch16_malakor_finale` | Chapter 16: The Primordial Syllable | `vestibule_echoes` |

---

## 7. Checking the images

Before wiring, for each image:

- The horizon is level and at 55% of the height (in a 1376×768 image, y ≈ 422; 768×1376, y ≈ 757; 1024×1024,
  y ≈ 563).
- The floor below it is open and plain from edge to edge.
- No people, no text, no logos.
- The three shapes of one background show the same place in the same light.
- PNG, exact size, crisp 4×4 art pixels.

Todd reviews the look himself; agents check only the points above.
