# VoiceRPG Engine: Game Creator & Starter Guide

This guide walks you through creating your own voice-commanded RPG using the **VoiceRPG Engine**. Follow these steps to replace the reference narrative and encounters with your own custom adventure.

---

## Step 1: Change Your Game's Identity

1. **App Name:** Open `app/src/main/res/values/strings.xml` and update:
   ```xml
   <string name="app_name">Your Game Title</string>
   ```
2. **Unique App ID:** Open `app/build.gradle.kts` and update `applicationId`:
   ```kotlin
   defaultConfig {
       applicationId = "com.yourname.yourgame"
   }
   ```
   *Changing `applicationId` allows your game to be installed on Android devices alongside other VoiceRPG titles.*

---

## Step 2: Write Your Story & Dialogue Trees

All narrative scenes and branching dialogues live in `StoryScript.kt`.

### 1. Define a Scene
A scene bundles the background visual and atmospheric background music:
```kotlin
val SCENE_TAVERN = StoryScene(
    id = "tavern",
    name = "The Rusty Anchor Tavern",
    backgroundAsset = "story/tavern.jpg",
    musicAsset = "audio/music/bgm_tavern.ogg",
    initialNodeId = "tavern_intro"
)
```

### 2. Define Dialogue Nodes
Nodes represent narrative beats, dialogues, or inquiries:
```kotlin
DialogueNode(
    id = "tavern_intro",
    speakerName = "Barkeep",
    dialogueText = "Welcome, traveler! What brings you to these foggy shores?",
    speakerPortraitAsset = "portraits/barkeep.jpg",
    choices = listOf(
        DialogueChoice(
            text = "Ask about the rumors in the crypt.",
            speechTriggers = listOf("rumors", "crypt", "ask about crypt"),
            nextNodeId = "inquire_crypt",
            completionFlag = "heard_crypt_rumors" // Auto-eliminates choice after visited!
        ),
        DialogueChoice(
            text = "Order a flagon of mead.",
            speechTriggers = listOf("order mead", "drink", "mead"),
            nextNodeId = "order_drink"
        )
    )
)
```

### 3. Transition into Combat
To transition seamlessly from story dialogue into an active battle, set `combatEncounterId` and `postBattleNodeId`:
```kotlin
DialogueNode(
    id = "bandit_ambush",
    speakerName = "Narrator",
    dialogueText = "The tavern door splinters open—shadow thieves draw their daggers!",
    combatEncounterId = "tavern_brawl",
    postBattleNodeId = "tavern_victory"
)
```

---

## Step 3: Create Custom Combat Encounters

All battle encounters live in `StoryEncounters.kt`.

### 1. Define an Enemy
```kotlin
fun createShadowThief(id: String, name: String) = Enemy(
    id = id,
    name = name,
    maxHp = 120,
    currentHp = 120,
    attackPower = 18,
    defense = 6,
    speed = 12,
    affinities = mapOf(
        AffinityType.LIGHT to AffinityValue.WEAK,
        AffinityType.DARK to AffinityValue.ABSORB
    )
)
```

### 2. Register Encounter Group
```kotlin
val TAVERN_BRAWL = CombatEncounter(
    id = "tavern_brawl",
    name = "Tavern Brawl",
    environment = BattleEnvironment.TAVERN,
    enemies = listOf(
        createShadowThief("thief_1", "Shadow Thief A"),
        createShadowThief("thief_2", "Shadow Thief B")
    )
)
```

---

## Step 4: Define Heroes & Spells

Custom player characters and spell libraries live in `ClassSpellLibrary.kt` and `CombatModels.kt`.

### 1. Define a Spell
```kotlin
val FIREBALL = Spell(
    id = "fireball",
    name = "Fireball",
    cost = 15,
    affinity = AffinityType.FIRE,
    targetType = SpellTargetType.SINGLE_ENEMY,
    basePower = 55,
    description = "Hurls a roaring sphere of flame.",
    chantSuggestions = listOf("Incinerate!", "Flames arise!", "Burn to ash!")
)
```

### 2. Configure Voice Keywords for the Spell
Add phonetic and fuzzy phrases in `SpellThesaurus.kt`:
```kotlin
"fireball" to listOf("fireball", "fire ball", "flame strike", "incinerate", "burn")
```

---

## Step 5: Configure Character Voices

In `CombatNarrator.kt`, assign custom pitch and voice settings to each character:
```kotlin
when (speakerName.lowercase()) {
    "barkeep" -> tts.setPitch(0.75f)   // Deep, gravelly voice
    "rogue"   -> tts.setPitch(1.20f)   // Swift, sharp pitch
    else      -> tts.setPitch(1.00f)   // Standard storyteller narrator
}
```

---

## Step 6: Build, Verify & Play

Run verification tests:
```bash
./gradlew testDebugUnitTest
```

Compile and install directly to your device via ADB:
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
