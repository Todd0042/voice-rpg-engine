# Image Prompts — The Squeeze: A Wally's World Memoir

Copy-paste-ready Gemini 2.5 Flash Image / Imagen 3 prompts for every graphical asset the game
actually loads. Each block is self-contained: paste the whole block into Gemini and say
"generate". Deliver at **1280x720 landscape** (title + story backgrounds) or **640x800
portrait** (character portraits) to match the engine's asset loaders.

**Do not hand-edit these files.** They are the in-repo placeholders the engine loads from
`app/src/main/assets/`. A generated image replaces the matching file name.

---

## Global rule 1 — the Melting Smiley rule (applies to EVERY prompt)

Every smiley-face graphic in the entire game — price badges, hanging store signs, wall posters,
uniform pins, background details, the title card — **must be a melting smiley**: the iconic
round face drooping and running like warm wax, eyes sagging, the smile smearing downward. No
normal, happy, intact smiley faces anywhere. This is the visual identity of the whole campaign.
If a prompt below doesn't call it out explicitly, add "with a melting smiley" to whatever smiley
the scene contains.

## Global rule 2 — signage text (applies to EVERY prompt)

In-store wall signage may read "Always Low Morale" (the campaign's own inverse slogan) as plain
wall graphics. Do **not** use the real "Everyday Low Price", "Always Save Money", "rollback",
"Always" slogans of any real chain, the yellow spark, or any Walmart wordmark/logo. This is a
fictional megastore named **Wally's World**; keep every real-brand mark out by construction.
No barcodes, no real logos, no founder likenesses, no real store names.

## Style header (paste before any prompt for consistency)

> Gritty but warm editorial photography, muted early-2000s American retail toning, slightly
> desaturated mustard-and-steel palette, soft fluorescent overhead light, honest working-class
> portraiture, subtle film grain, photorealistic, 35mm. No real logos, no barcodes, no text
> except what the prompt itself names and the mandatory melting-smiley graphics.

---

## 1. Title screen background
**File:** `app/src/main/assets/story/squeeze_title.jpg` (1280x720)
**Also burned as the adaptive-launcher artwork source (`app-icon.jpg` logo family).**

> Wide establishing shot of a giant fictional superstore front from across a huge rain-slicked
> parking lot at dusk. The building stretches for blocks under mustard-yellow signage reading
> "WALLY'S WORLD" in bold rounded letters (no other text). Warm light spills from the glass
> entrance; a lone orange-vested employee pushes a long train of carts toward the door, and the
> giant sign above the entrance carries a single **melting smiley** — the face drooping with wax
> running down it. Overhead low clouds, a faint rainbow, a distant pickup truck. Nostalgic,
> slightly melancholy early-2000s retail grandeur. No real logos, no real brand marks.

## 2. Front-end / registers scene
**File:** `app/src/main/assets/story/squeeze_utah_front_end.jpg` (1280x720)

> Interior of a cavernous fictional superstore checkout lobby in a Utah-frontier-styled morning:
> dozens of register lanes under buzzing fluorescent tubes, worn teal-and-orange employee vests,
> shoppers queued with full carts. A weary older cashier waves kindly while the young freight
> associate (Todd) struggles to hoist a heavy appliance onto the belt. Behind the registers a
> hanging sign shows a big **melting smiley**, the wax face dripping. Overhead speaker grilles,
> chipped tiles, warm but tired light. No text, no real logos.

## 3. Trench / floor-stocker scene
**File:** `app/src/main/assets/story/squeeze_utah_trench.jpg` (1280x720)

> Low-angle shot deep inside a giant megamart's back-stock "the trench" — a long canyon of
> tall steel racking stacked with pallets of boxes. A young freight associate in an orange vest
> walks a thin aisle holding a heavy cardboard box, head bowed with exhaustion. Dust motes in a
> shaft of fluorescent light, forklift silhouette far behind, torn shrink-wrap hanging. On one
> pallet a lone **melting smiley** sticker droops. Moody, cinematic, claustrophobic scale.
> No text, no logos.

## 4. Office / manager's desk
**File:** `app/src/main/assets/story/squeeze_utah_office.jpg` (1280x720)

> A cramped back-office inside a big-box store: cluttered desk with an old CRT monitor showing a
> green inventory spreadsheet, office chair, wall of laminate shelving piled with file boxes, a
> single frosted window looking onto the sales floor. A weary store manager in a teal shirt with
> an orange tie leans back with a clipboard while the young associate stands in the doorway. On
> the wall hangs a **melting smiley** poster, face running. Fluorescent cast. No readable text.

## 5. Sales floor / center aisle
**File:** `app/src/main/assets/story/squeeze_utah_floor.jpg` (1280x720)

> Overhead-ish wide shot of the vast main aisle of a fictional megastore: towering shelves of
> merchandise in neat rows, huge wall graphics in muted mustard (no real text) reading only
> "ALWAYS LOW MORALE" as plain wallpaper on otherwise bare walls. Shoppers with carts,
> orange-vested employees stocking shelves with a pallet jack, a PA speaker hanging from the
> ceiling. A giant **melting smiley** drinks from the ceiling on a hanging banner, wax dripping.
> Bright but flat fluorescent light, busy but orderly. No real logos, no barcodes.

## 6. Todd — the hero
**File:** `app/src/main/assets/portraits/todd.jpg` (640x800, portrait)

> Middle-distance portrait of a young man, early 20s, wearing a cheap teal-and-orange employee
> vest over a wrinkled white polo. Nametag-less; instead a **melting smiley** button on the vest,
> face running. He looks tired but quietly determined, slight stubble, messy brown hair, one
> eyebrow raised, holding a folded cardboard box under his arm. Studio-style editorial portrait
> with soft fluorescent key light, plain slightly-blurred superstore aisle behind him, muted
> mustard palette. 35mm, photorealistic. No real logos.

## 7. Jodie — the floor's senior hand (a man, late 60s)
**File:** `app/src/main/assets/portraits/jodie.jpg` (640x800, portrait)

> Editorial portrait of a man in his late 60s, the floor's senior hand — very short, bristly
> white hair, weathered kind face, strong forearms, wearing the worn teal-and-orange employee
> vest over a flannel shirt. He holds a clipboard with a yellow highlighter tucked behind his
> ear勝利. On the clipboard, a custom three-tier name badge — top tier: a small **melting smiley**
> icon on blue with the text "blah blah sometimes"; middle tier: a plain white strip reading
> "401000500"; bottom tier: a blue strip reading "aka jodie". The badge text is small and only
> half-readable in the photo (editorial do-not-fully-render instruction) but unmistakably in
> that three-tier shape. Soft back-office light, honest working-class warmth. 35mm
> photorealistic. Melting smiley only; no real logos.

## 8. Mattie-T — store manager (a man)
**File:** `app/src/main/assets/portraits/mattie_t.jpg` (640x800, portrait)

> Editorial portrait of a man in his late 30s, balding, wearing a tidy blue dress shirt with a
> tie, sitting in a manager's swivel chair at a computer desk. Behind him a frosted window and a
> wall poster of a **melting smiley** (face running) done in the muted brand palette. Blue-hour
> palette lighting — cool dusk light through the window, warm face light from the CRT. He looks
> weary but sharp, glasses low on his nose, one hand resting on a well-worn clipboard. 35mm
> photorealistic editorial. The melting smiley is the only graphic; no real logos, no readable
> text.

## 9. Narrator / storyteller
**File:** `app/src/main/assets/portraits/narrator.jpg` (640x800, portrait)

> Mysterious but warm portrait of an unseen storyteller represented as a softly glowing
> silhouette — a faintly humanoid shape of warm hearth-light with two bright eyes, no distinct
> face, against a deep plum-black background with subtle book-page texture. Impressionistic,
> calm, inviting, slightly magical memoir-nostalgia. Loose painterly glow, NOT photorealistic
> (this one is intentionally abstract). No text.

---

## Regeneration notes

- If Gemini refuses a prompt for "Walmart trademark", the image already uses the fictional
  "Wally's World" name — just re-run; the fallback is fine.
- The **melting smiley** and **Always Low Morale** rules are baked into every block above; an
  intact, happy smiley or the real "Everyday Low Price" wording is the one thing that's an
  automatic reject-and-regenerate.
- Portraits are hot-swappable by file name alone (`todd.jpg`, `jodie.jpg`, `mattie_t.jpg`,
  `narrator.jpg`) — the engine never references specific art beyond these names, so you may
  upgrade any of them without a rebuild.
