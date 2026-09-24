package com.voicerpg.engine.content

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.voicerpg.engine.combat.StatGrowth
import com.voicerpg.engine.model.BattleEnvironment
import com.voicerpg.engine.model.DialogueChoice
import com.voicerpg.engine.model.DialogueNode
import com.voicerpg.engine.model.DialogueSpeaker
import com.voicerpg.engine.model.Enemy
import com.voicerpg.engine.model.EncounterDefinition
import com.voicerpg.engine.model.BossPhaseRule
import com.voicerpg.engine.model.MidBattleJoin
import com.voicerpg.engine.model.HubCompletion
import com.voicerpg.engine.model.PartyMember
import com.voicerpg.engine.model.SavedCharacterStats
import com.voicerpg.engine.model.SpeakerSide
import com.voicerpg.engine.model.Spell
import com.voicerpg.engine.model.SpellSchool
import com.voicerpg.engine.model.StoryScene
import java.io.File

// =============================================================================
// JSON SCHEMA DTOs (assets/game/**)
// Every field has a default so Gson tolerates missing keys in content files.
// =============================================================================

data class ManifestDto(
    val gameTitle: String = "VoiceRPG Engine",
    val gameSubtitle: String = "",
    val initialSceneId: String = "",
    val initialNodeId: String = "",
    val defaultEncounterId: String = "",
    val startingAchievementId: String = "QUEST_BEGUN",
    val victoryLine: String = "Victory! The battle is won.",
    val defeatLine: String = "Defeat. Your voice fades.",
    val titleBackgroundAsset: String? = null
)

data class GrowthDto(
    val maxHp: Int = 18,
    val maxMp: Int = 8,
    val speed: Int = 1,
    val defense: Int = 1,
    val potencyPct: Float = 0.03f
) {
    fun toStatGrowth(): StatGrowth = StatGrowth(maxHp, maxMp, speed, defense, potencyPct)
}

data class CharacterCombatDto(
    val maxHp: Int = 100,
    val maxMp: Int = 50,
    val speed: Int = 50,
    val defense: Int = 0,
    val avatarTint: String = "#4FC3F7",
    val startAtb: Float = 0f,
    val spellIds: List<String>? = null,
    val growth: GrowthDto? = null
)

data class SpellGrantDto(
    val flag: String = "",
    val spellIds: List<String>? = null
)

data class CharacterDto(
    val id: String = "",
    val role: String = "npc",
    val name: String = "",
    val title: String = "",
    val portraitAsset: String? = null,
    val spriteId: String = "template_hero",
    val themeColor: String = "#FFFFFF",
    val ttsPitch: Float = 1.0f,
    val samplePhrase: String? = null,
    val preferredVoiceId: String? = null,
    val voiceAliases: List<String>? = null,
    val loreClass: String = "Adventurer",
    val combat: CharacterCombatDto? = null,
    val joinWhenFlag: String? = null,
    val spellGrants: List<SpellGrantDto>? = null
)

data class ClassDto(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val startingHp: Int = 200,
    val startingMp: Int = 100,
    val startingSpeed: Int = 60,
    val startingBaseAttack: Int = 20,
    val preferredSchool: String = "PHYSICAL",
    val starterSpellIds: List<String>? = null,
    val voiceKeywords: List<String>? = null,
    val growth: GrowthDto? = null
)

data class SpellDto(
    val id: String = "",
    val name: String = "",
    val school: String = "PHYSICAL",
    val basePower: Int = 0,
    val mpCost: Int = 0,
    val isHeal: Boolean = false,
    val hitsAll: Boolean = false,
    val description: String = "",
    val exampleChant: String = "",
    val status: String? = null,
    val lifesteal: Boolean = false,
    val isGuard: Boolean = false,
    val manaRestorePct: Float = 0f,
    val aliases: List<String>? = null
)

data class SpellFileDto(val spells: List<SpellDto>? = null)

data class EnemyDto(
    val id: String = "",
    val name: String = "",
    val subtitle: String = "",
    val family: String = "FLESH",
    val selfElement: String = "",
    val hp: Int = 100,
    val attack: Int = 10,
    val defense: Int = 0,
    val speed: Int = 50,
    val xpReward: Int = 60,
    val movesetId: String = "",
    val isBoss: Boolean = false,
    val spriteId: String = "",
    val spriteTint: String = "#E57373"
)

data class EnemyFileDto(val enemies: List<EnemyDto>? = null)

data class EncounterEnemyDto(
    val templateId: String = "",
    val id: String? = null,
    val name: String? = null,
    val subtitle: String? = null,
    val hp: Int? = null,
    val attack: Int? = null,
    val defense: Int? = null,
    val speed: Int? = null,
    val isTargeted: Boolean = false,
    val atbGauge: Float = 0f
)

data class BossPhaseDto(
    val enemyId: String = "",
    val hpBelow: Float = 0.5f,
    val bannerText: String = "",
    val narrationText: String = "",
    val reducePartyToHpPct: Float? = null,
    val executeBelowHpPct: Float? = null,
    val summonEnemyId: String? = null,
    val summonCount: Int = 0
)

data class MidBattleJoinDto(
    val characterId: String = "",
    val bannerText: String = "",
    val announcementText: String = "",
    val afterEnemyActions: Int = 1
)

data class EncounterDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val environment: String = "FOREST",
    val party: List<String>? = null,
    val enemies: List<EncounterEnemyDto>? = null,
    val bossPhases: List<BossPhaseDto>? = null,
    val midBattleJoin: MidBattleJoinDto? = null
)

data class EncounterFileDto(val encounters: List<EncounterDto>? = null)

data class SceneDto(
    val id: String = "",
    val name: String = "",
    val chapterTitle: String = "",
    val backgroundAsset: String = "",
    val initialNodeId: String = "",
    val ambientDescription: String = "",
    val musicAsset: String = "audio/music/bgm_exploration.ogg",
    val chapterIndex: Int = 0,
    val actNumber: Int = 1,
    val actTitle: String = "Act I",
    val milestoneTitle: String = "",
    val milestoneLocation: String = "",
    val milestoneSummary: String = "",
    val milestoneIcon: String = "✨",
    val defaultObjective: String = ""
)

data class SceneFileDto(val scenes: List<SceneDto>? = null)

data class ChoiceDto(
    val id: String = "",
    val text: String = "",
    val voiceKeywords: List<String>? = null,
    val nextNodeId: String = "",
    val completionFlag: String? = null
)

data class HubCompletionDto(
    val requiredFlags: List<String>? = null,
    val redirectToNodeId: String = ""
)

data class NodeDto(
    val id: String = "",
    val speakerId: String = "narrator",
    val side: String = "CENTER_NARRATOR",
    val text: String = "",
    val choices: List<ChoiceDto>? = null,
    val nextNodeId: String? = null,
    val triggerBattleEncounterId: String? = null,
    val changeSceneId: String? = null,
    val setFlagOnEnter: String? = null,
    val hubCompletion: HubCompletionDto? = null,
    val healPartyOnEnter: String? = null
)

data class NodeFileDto(val nodes: List<NodeDto>? = null)

// =============================================================================
// RESOLVED ENGINE MODELS
// =============================================================================

data class GameManifest(
    val gameTitle: String,
    val gameSubtitle: String,
    val initialSceneId: String,
    val initialNodeId: String,
    val defaultEncounterId: String,
    val startingAchievementId: String,
    val victoryLine: String,
    val defeatLine: String,
    val titleBackgroundAsset: String? = null
)

/** A playable/voice hero class archetype loaded from assets/game/classes/<name>.json. */
data class HeroClassDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val startingHp: Int,
    val startingMp: Int,
    val startingSpeed: Int,
    val startingBaseAttack: Int,
    val preferredSchool: SpellSchool,
    val starterSpellIds: List<String>,
    val voiceKeywords: List<String>,
    val growth: StatGrowth
)

enum class CharacterRole { HERO, NPC, NARRATOR }

data class SpellGrant(val flag: String, val spellIds: List<String>)

data class CharacterCombat(
    val maxHp: Int,
    val maxMp: Int,
    val speed: Int,
    val defense: Int,
    val avatarTint: Color,
    val startAtb: Float,
    val spells: List<Spell>,
    val growth: StatGrowth
)

/** A fully resolved character: dialogue speaker + combat identity, all from one data file. */
data class GameCharacter(
    val id: String,
    val role: CharacterRole,
    val speaker: DialogueSpeaker,
    val loreClass: String,
    val spriteId: String,
    val voiceAliases: List<String>,
    val preferredVoiceId: String?,
    val combat: CharacterCombat?,
    val joinWhenFlag: String?,
    val spellGrants: List<SpellGrant>
) {
    fun toPartyMember(atbOverride: Float? = null): PartyMember? {
        val c = combat ?: return null
        return PartyMember(
            id = id,
            name = speaker.name,
            loreClass = loreClass,
            currentHp = c.maxHp,
            maxHp = c.maxHp,
            currentMp = c.maxMp,
            maxMp = c.maxMp,
            spells = c.spells,
            avatarTint = c.avatarTint,
            speed = c.speed,
            defense = c.defense,
            atbGauge = atbOverride ?: c.startAtb
        )
    }

    fun toSavedStats(): SavedCharacterStats? {
        val c = combat ?: return null
        return SavedCharacterStats(
            id = id,
            name = speaker.name,
            loreClass = loreClass,
            currentHp = c.maxHp,
            maxHp = c.maxHp,
            currentMp = c.maxMp,
            maxMp = c.maxMp,
            speed = c.speed,
            level = 1,
            xp = 0,
            spellIds = c.spells.map { it.id }
        )
    }
}

/** Reusable enemy definition loaded from assets/game/enemies/enemies.json. */
data class EnemyTemplate(
    val id: String,
    val name: String,
    val subtitle: String,
    val family: String,
    val selfElement: String,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val xpReward: Int,
    val movesetId: String,
    val isBoss: Boolean,
    val spriteId: String,
    val spriteTint: Color
) {
    fun toEnemy(
        instanceId: String? = null,
        nameOverride: String? = null,
        subtitleOverride: String? = null,
        hpOverride: Int? = null,
        attackOverride: Int? = null,
        defenseOverride: Int? = null,
        speedOverride: Int? = null,
        isTargeted: Boolean = false,
        atbGauge: Float = 0f
    ): Enemy {
        val hp = hpOverride ?: this.hp
        return Enemy(
            id = instanceId ?: id,
            name = nameOverride ?: name,
            subtitle = subtitleOverride ?: subtitle,
            currentHp = hp,
            maxHp = hp,
            baseAttack = attackOverride ?: attack,
            defense = defenseOverride ?: defense,
            speed = speedOverride ?: speed,
            isBoss = isBoss,
            isTargeted = isTargeted,
            spriteTint = spriteTint,
            spriteId = spriteId,
            family = family,
            selfElement = selfElement,
            xpReward = xpReward,
            movesetId = movesetId,
            atbGauge = atbGauge
        )
    }
}

// =============================================================================
// CONTENT PACK & LOADER
// =============================================================================

/** Immutable, fully cross-referenced bundle of a game's data files. */
class GameContentPack(
    val manifest: GameManifest,
    val characters: Map<String, GameCharacter>,
    val classes: List<HeroClassDef>,
    val spells: Map<String, Spell>,
    val enemyTemplates: Map<String, EnemyTemplate>,
    val encounters: Map<String, EncounterDefinition>,
    val scenes: Map<String, StoryScene>,
    val nodes: Map<String, DialogueNode>,
    val narrator: DialogueSpeaker
) {
    val heroCharacter: GameCharacter? = characters.values.firstOrNull { it.role == CharacterRole.HERO }

    /** All speakers that can hold a TTS voice binding (narrator first). */
    val allSpeakers: List<DialogueSpeaker> =
        (listOf(narrator) + characters.values.filter { it.role != CharacterRole.NARRATOR }.map { it.speaker })
            .distinctBy { it.id }

    val initialScene: StoryScene =
        scenes[manifest.initialSceneId] ?: scenes.values.firstOrNull() ?: FALLBACK_SCENE
    val initialNode: DialogueNode =
        nodes[manifest.initialNodeId] ?: nodes[initialScene.initialNodeId] ?: nodes.values.firstOrNull() ?: FALLBACK_NODE

    val defaultEncounter: EncounterDefinition? =
        encounters[manifest.defaultEncounterId] ?: encounters.values.firstOrNull()

    /** Per-speaker first-try TTS voice pack preferences declared in the character files. */
    val preferredVoiceIds: Map<String, String> =
        characters.values.mapNotNull { c -> c.preferredVoiceId?.let { c.id to it } }.toMap()

    fun speakerById(id: String?): DialogueSpeaker? =
        if (id == null) null else if (id == narrator.id) narrator else characters[id]?.speaker

    fun characterById(id: String?): GameCharacter? = if (id == null) null else characters[id]

    fun classById(id: String?): HeroClassDef =
        classes.firstOrNull { it.id == id } ?: classes.firstOrNull() ?: FALLBACK_CLASS

    fun encounterById(id: String?): EncounterDefinition? = if (id == null) null else encounters[id]

    fun spellById(id: String?): Spell? = if (id == null) null else spells[id]

    fun spellsForClass(classId: String?): List<Spell> =
        classById(classId).starterSpellIds.mapNotNull { spells[it] }

    fun enemyTemplateById(id: String?): EnemyTemplate? = if (id == null) null else enemyTemplates[id]

    fun voiceAliasesFor(memberId: String?): List<String> = characterById(memberId)?.voiceAliases ?: emptyList()

    /** Spells of the named character, falling back to the hero's loadout. */
    fun heroSpellsFor(memberId: String = "hero"): List<Spell> =
        characters[memberId]?.combat?.spells ?: heroCharacter?.combat?.spells ?: emptyList()

    /** Spells of a named companion character. */
    fun companionSpellsFor(memberId: String): List<Spell> =
        characters[memberId]?.combat?.spells ?: emptyList()

    /** Starter loadout for a companion: its full spell list from the character data file. */
    fun companionStarterSpellsFor(memberId: String): List<Spell> = companionSpellsFor(memberId)

    /** The free breath/attune discipline a character can cast to restore mana. */
    fun breathSpellFor(memberId: String): Spell? =
        characters[memberId]?.combat?.spells?.firstOrNull { it.manaRestorePct > 0f }

    /** Resolves a character to a combat-ready party member (null when the character has no combat stats). */
    fun companionMember(memberId: String): PartyMember? = characters[memberId]?.toPartyMember()

    /** Data-driven starter party: the template hero plus the first combat-capable NPC. */
    fun createDuoParty(): List<PartyMember> = listOfNotNull(
        heroCharacter?.toPartyMember(),
        characters.values
            .firstOrNull { it.role != CharacterRole.HERO && it.combat != null }
            ?.toPartyMember()
    )

    /** Generic summoned-minion factory used by enemy SUMMON moves. */
    fun createMinion(idSuffix: String, name: String = "Template Minion", subtitle: String = "Minion", hp: Int = 150): Enemy {
        val template = enemyTemplates["template_minion"]
        return template?.toEnemy(
            instanceId = "minion_$idSuffix",
            nameOverride = name,
            subtitleOverride = subtitle,
            hpOverride = hp,
            isTargeted = false,
            atbGauge = 0f
        ) ?: Enemy(
            id = "minion_$idSuffix",
            name = name,
            subtitle = subtitle,
            currentHp = hp,
            maxHp = hp,
            baseAttack = 18,
            isTargeted = false,
            spriteTint = Color(0xFF80CBC4),
            speed = 55,
            atbGauge = 0f
        )
    }

    companion object {
        val FALLBACK_SCENE = StoryScene(
            id = "fallback_scene",
            name = "Template Chamber",
            chapterTitle = "Sandbox",
            backgroundAsset = "story/template_scene.jpg",
            initialNodeId = "fallback_node",
            ambientDescription = "A quiet chamber awaiting your story."
        )
        val FALLBACK_NODE = DialogueNode(
            id = "fallback_node",
            speaker = DialogueSpeaker.NARRATOR,
            side = SpeakerSide.CENTER_NARRATOR,
            text = "No story content is loaded. Add scenes and nodes under assets/game/story/."
        )
        val FALLBACK_CLASS = HeroClassDef(
            id = "adventurer",
            title = "Adventurer",
            subtitle = "Jack of All Trades",
            description = "A balanced template class.",
            startingHp = 240,
            startingMp = 120,
            startingSpeed = 65,
            startingBaseAttack = 22,
            preferredSchool = SpellSchool.PHYSICAL,
            starterSpellIds = emptyList(),
            voiceKeywords = listOf("adventurer", "hero"),
            growth = StatGrowth(14, 10, 1, 1, 0.035f)
        )
    }
}

/** Abstraction over the bytes source so the same loader runs on Android and in JVM tests. */
interface ContentSource {
    fun readText(path: String): String?
    fun listFiles(dir: String): List<String>
}

class AssetContentSource(private val assets: AssetManager, private val root: String) : ContentSource {
    override fun readText(path: String): String? = try {
        assets.open("$root/$path").bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (_: Exception) {
        null
    }

    override fun listFiles(dir: String): List<String> = try {
        assets.list("$root/$dir")?.filter { it.endsWith(".json") }?.sorted() ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

class FileContentSource(private val rootDir: File) : ContentSource {
    override fun readText(path: String): String? {
        val file = File(rootDir, path)
        return if (file.isFile) runCatching { file.readText(Charsets.UTF_8) }.getOrNull() else null
    }

    override fun listFiles(dir: String): List<String> =
        File(rootDir, dir).listFiles()?.filter { it.isFile && it.name.endsWith(".json") }?.map { it.name }?.sorted() ?: emptyList()
}

/** Parses the JSON content tree into a resolved [GameContentPack]. */
class GameContentLoader(private val source: ContentSource) {

    private val gson = Gson()

    fun load(): GameContentPack {
        val manifestDto = read("manifest.json", ManifestDto::class.java) ?: ManifestDto()
        val spells = (read("spells/spells.json", SpellFileDto::class.java)?.spells ?: emptyList())
            .map { it.toSpell() }
            .associateBy { it.id }

        val classes = source.listFiles("classes")
            .mapNotNull { read("classes/$it", ClassDto::class.java) }
            .filter { it.id.isNotBlank() }
            .map { it.toDef(spells) }

        val characters = source.listFiles("characters")
            .mapNotNull { read("characters/$it", CharacterDto::class.java) }
            .filter { it.id.isNotBlank() }
            .map { it.toCharacter(spells) }
            .associateBy { it.id }

        val enemyTemplates = (read("enemies/enemies.json", EnemyFileDto::class.java)?.enemies ?: emptyList())
            .filter { it.id.isNotBlank() }
            .map { it.toTemplate() }
            .associateBy { it.id }

        val encounters = (read("encounters/encounters.json", EncounterFileDto::class.java)?.encounters ?: emptyList())
            .filter { it.id.isNotBlank() }
            .map { it.toDefinition(characters, enemyTemplates) }
            .associateBy { it.id }

        val scenes = (read("story/scenes.json", SceneFileDto::class.java)?.scenes ?: emptyList())
            .filter { it.id.isNotBlank() }
            .map { it.toScene() }
            .associateBy { it.id }

        val speakersBydId = characters.mapValues { it.value.speaker }
        val nodes = (read("story/nodes.json", NodeFileDto::class.java)?.nodes ?: emptyList())
            .filter { it.id.isNotBlank() }
            .map { it.toNode(speakersBydId) }
            .associateBy { it.id }

        val narrator = characters.values.firstOrNull { it.role == CharacterRole.NARRATOR }?.speaker
            ?: DialogueSpeaker.NARRATOR

        val pack = GameContentPack(
            manifest = GameManifest(
                gameTitle = manifestDto.gameTitle,
                gameSubtitle = manifestDto.gameSubtitle,
                initialSceneId = manifestDto.initialSceneId,
                initialNodeId = manifestDto.initialNodeId,
                defaultEncounterId = manifestDto.defaultEncounterId,
                startingAchievementId = manifestDto.startingAchievementId,
                victoryLine = manifestDto.victoryLine,
                defeatLine = manifestDto.defeatLine,
                titleBackgroundAsset = manifestDto.titleBackgroundAsset
            ),
            characters = characters,
            classes = classes,
            spells = spells,
            enemyTemplates = enemyTemplates,
            encounters = encounters,
            scenes = scenes,
            nodes = nodes,
            narrator = narrator
        )
        return if (pack.scenes.isEmpty() || pack.nodes.isEmpty()) EmergencyFallback.build() else pack
    }

    private fun <T> read(path: String, type: Class<T>): T? = try {
        source.readText(path)?.let { gson.fromJson(it, type) }
    } catch (_: Exception) {
        null
    }

    private fun SpellDto.toSpell(): Spell = Spell(
        id = id,
        name = name.ifBlank { id },
        school = runCatching { SpellSchool.valueOf(school) }.getOrDefault(SpellSchool.PHYSICAL),
        basePower = basePower,
        mpCost = mpCost,
        isHeal = isHeal,
        hitsAll = hitsAll,
        description = description,
        exampleChant = exampleChant.ifBlank { name },
        status = status,
        lifesteal = lifesteal,
        isGuard = isGuard,
        manaRestorePct = manaRestorePct,
        aliases = aliases ?: emptyList()
    )

    private fun ClassDto.toDef(spells: Map<String, Spell>): HeroClassDef = HeroClassDef(
        id = id,
        title = title.ifBlank { id },
        subtitle = subtitle,
        description = description,
        startingHp = startingHp,
        startingMp = startingMp,
        startingSpeed = startingSpeed,
        startingBaseAttack = startingBaseAttack,
        preferredSchool = runCatching { SpellSchool.valueOf(preferredSchool) }.getOrDefault(SpellSchool.PHYSICAL),
        starterSpellIds = starterSpellIds ?: emptyList(),
        voiceKeywords = voiceKeywords ?: emptyList(),
        growth = (growth ?: GrowthDto()).toStatGrowth()
    )

    private fun CharacterDto.toCharacter(spells: Map<String, Spell>): GameCharacter {
        val speaker = DialogueSpeaker(
            id = id,
            name = name.ifBlank { id },
            title = title,
            portraitAsset = portraitAsset,
            themeColor = parseHexColor(themeColor, Color.White),
            ttsPitch = ttsPitch,
            samplePhrase = samplePhrase
        )
        val combat = combat?.let { c ->
            CharacterCombat(
                maxHp = c.maxHp,
                maxMp = c.maxMp,
                speed = c.speed,
                defense = c.defense,
                avatarTint = parseHexColor(c.avatarTint, Color(0xFF4FC3F7)),
                startAtb = c.startAtb,
                spells = (c.spellIds ?: emptyList()).mapNotNull { spells[it] },
                growth = (c.growth ?: GrowthDto()).toStatGrowth()
            )
        }
        return GameCharacter(
            id = id,
            role = when (role.lowercase()) {
                "hero" -> CharacterRole.HERO
                "narrator" -> CharacterRole.NARRATOR
                else -> CharacterRole.NPC
            },
            speaker = speaker,
            loreClass = loreClass,
            spriteId = spriteId,
            voiceAliases = voiceAliases ?: emptyList(),
            preferredVoiceId = preferredVoiceId,
            combat = combat,
            joinWhenFlag = joinWhenFlag,
            spellGrants = (spellGrants ?: emptyList()).map {
                SpellGrant(it.flag, it.spellIds ?: emptyList())
            }
        )
    }

    private fun EnemyDto.toTemplate(): EnemyTemplate = EnemyTemplate(
        id = id,
        name = name.ifBlank { id },
        subtitle = subtitle,
        family = family,
        selfElement = selfElement,
        hp = hp,
        attack = attack,
        defense = defense,
        speed = speed,
        xpReward = xpReward,
        movesetId = movesetId,
        isBoss = isBoss,
        spriteId = spriteId,
        spriteTint = parseHexColor(spriteTint, Color(0xFFE57373))
    )

    private fun EncounterDto.toDefinition(
        characters: Map<String, GameCharacter>,
        enemyTemplates: Map<String, EnemyTemplate>
    ): EncounterDefinition {
        val party = (party ?: emptyList()).mapNotNull { characters[it]?.toPartyMember() }
        val enemies = (enemies ?: emptyList()).mapNotNull { ref ->
            enemyTemplates[ref.templateId]?.toEnemy(
                instanceId = ref.id,
                nameOverride = ref.name,
                subtitleOverride = ref.subtitle,
                hpOverride = ref.hp,
                attackOverride = ref.attack,
                defenseOverride = ref.defense,
                speedOverride = ref.speed,
                isTargeted = ref.isTargeted,
                atbGauge = ref.atbGauge
            )
        }
        return EncounterDefinition(
            id = id,
            name = name.ifBlank { id },
            description = description,
            environment = runCatching { BattleEnvironment.valueOf(environment) }.getOrDefault(BattleEnvironment.FOREST),
            enemies = enemies,
            initialParty = party.ifEmpty { null },
            bossPhases = (bossPhases ?: emptyList()).map {
                BossPhaseRule(
                    enemyId = it.enemyId,
                    hpBelow = it.hpBelow,
                    bannerText = it.bannerText,
                    narrationText = it.narrationText,
                    reducePartyToHpPct = it.reducePartyToHpPct,
                    executeBelowHpPct = it.executeBelowHpPct,
                    summonEnemyId = it.summonEnemyId,
                    summonCount = it.summonCount
                )
            },
            midBattleJoin = midBattleJoin?.let {
                MidBattleJoin(
                    characterId = it.characterId,
                    bannerText = it.bannerText,
                    announcementText = it.announcementText,
                    afterEnemyActions = it.afterEnemyActions
                )
            }
        )
    }

    private fun SceneDto.toScene(): StoryScene = StoryScene(
        id = id,
        name = name.ifBlank { id },
        chapterTitle = chapterTitle,
        backgroundAsset = backgroundAsset,
        initialNodeId = initialNodeId,
        ambientDescription = ambientDescription,
        musicAsset = musicAsset,
        chapterIndex = chapterIndex,
        actNumber = actNumber,
        actTitle = actTitle,
        milestoneTitle = milestoneTitle.ifBlank { chapterTitle },
        milestoneLocation = milestoneLocation.ifBlank { name },
        milestoneSummary = milestoneSummary,
        milestoneIcon = milestoneIcon,
        defaultObjective = defaultObjective
    )

    private fun NodeDto.toNode(speakers: Map<String, DialogueSpeaker>): DialogueNode = DialogueNode(
        id = id,
        speaker = speakers[speakerId] ?: DialogueSpeaker.NARRATOR,
        side = runCatching { SpeakerSide.valueOf(side) }.getOrDefault(SpeakerSide.CENTER_NARRATOR),
        text = text,
        choices = (choices ?: emptyList()).map {
            DialogueChoice(
                id = it.id,
                text = it.text,
                voiceKeywords = it.voiceKeywords ?: emptyList(),
                nextNodeId = it.nextNodeId,
                completionFlag = it.completionFlag
            )
        },
        nextNodeId = nextNodeId,
        triggerBattleEncounterId = triggerBattleEncounterId,
        changeSceneId = changeSceneId,
        setFlagOnEnter = setFlagOnEnter,
        hubCompletion = hubCompletion?.let {
            HubCompletion(it.requiredFlags ?: emptyList(), it.redirectToNodeId)
        },
        healPartyOnEnter = healPartyOnEnter
    )
}

/**
 * Absolute last-resort content used when no data files can be located (e.g. a
 * mispackaged build). Guarantees the engine boots into a playable sandbox state.
 */
object EmergencyFallback {
    fun build(): GameContentPack {
        val narrator = DialogueSpeaker.NARRATOR
        val scene = GameContentPack.FALLBACK_SCENE
        val node = GameContentPack.FALLBACK_NODE
        return GameContentPack(
            manifest = GameManifest(
                gameTitle = "VoiceRPG Engine",
                gameSubtitle = "No content files found",
                initialSceneId = scene.id,
                initialNodeId = node.id,
                defaultEncounterId = "",
                startingAchievementId = "QUEST_BEGUN",
                victoryLine = "Victory! The battle is won.",
                defeatLine = "Defeat. Your voice fades.",
                titleBackgroundAsset = null
            ),
            characters = emptyMap(),
            classes = emptyList(),
            spells = emptyMap(),
            enemyTemplates = emptyMap(),
            encounters = emptyMap(),
            scenes = mapOf(scene.id to scene),
            nodes = mapOf(node.id to node),
            narrator = narrator
        )
    }
}

/**
 * Global access point to the active game's data files.
 *
 * On Android, call [initialize] with a Context (MainActivity does this at boot).
 * In JVM unit tests the pack is loaded directly from the repository's asset
 * directory on disk. All engine systems (story, combat, narration, saves, UI)
 * resolve characters, classes, spells, enemies, encounters, scenes, and nodes
 * through this object — nothing game-specific is hardcoded in engine classes.
 */
object GameContent {

    private const val CONTENT_ROOT = "game"

    @Volatile
    private var current: GameContentPack? = null

    fun initialize(context: Context) {
        current = try {
            GameContentLoader(AssetContentSource(context.assets, CONTENT_ROOT)).load()
        } catch (t: Throwable) {
            android.util.Log.e("GameContent", "Failed to load game content from assets", t)
            loadFromFilesystem() ?: EmergencyFallback.build()
        }
    }

    fun initializeFromDirectory(dir: File) {
        current = try {
            GameContentLoader(FileContentSource(dir)).load()
        } catch (t: Throwable) {
            EmergencyFallback.build()
        }
    }

    /** Clears the cached pack (used by tests to reload modified content). */
    fun reset() {
        current = null
    }

    val pack: GameContentPack
        get() {
            current?.let { return it }
            synchronized(this) {
                current?.let { return it }
                val loaded = loadFromFilesystem() ?: EmergencyFallback.build()
                current = loaded
                return loaded
            }
        }

    private fun loadFromFilesystem(): GameContentPack? {
        val candidates = listOf(
            File("$CONTENT_ROOT"),
            File("src/main/assets/$CONTENT_ROOT"),
            File("app/src/main/assets/$CONTENT_ROOT"),
            File("../app/src/main/assets/$CONTENT_ROOT")
        )
        for (dir in candidates) {
            if (dir.isDirectory) {
                return try {
                    GameContentLoader(FileContentSource(dir)).load()
                } catch (_: Throwable) {
                    null
                }
            }
        }
        return null
    }

    // --- Convenience accessors ---
    val manifest: GameManifest get() = pack.manifest
    val scenes: Map<String, StoryScene> get() = pack.scenes
    val nodes: Map<String, DialogueNode> get() = pack.nodes
    val characters: Map<String, GameCharacter> get() = pack.characters
    val classes: List<HeroClassDef> get() = pack.classes
    val spells: Map<String, Spell> get() = pack.spells
    val encounters: Map<String, EncounterDefinition> get() = pack.encounters
    val enemyTemplates: Map<String, EnemyTemplate> get() = pack.enemyTemplates
    val narrator: DialogueSpeaker get() = pack.narrator
    val heroCharacter: GameCharacter? get() = pack.heroCharacter
    val allSpeakers: List<DialogueSpeaker> get() = pack.allSpeakers
    val initialScene: StoryScene get() = pack.initialScene
    val initialNode: DialogueNode get() = pack.initialNode
    val defaultEncounter: EncounterDefinition? get() = pack.defaultEncounter
    val preferredVoiceIds: Map<String, String> get() = pack.preferredVoiceIds

    fun speakerById(id: String?): DialogueSpeaker? = pack.speakerById(id)
    fun characterById(id: String?): GameCharacter? = pack.characterById(id)
    fun classById(id: String?): HeroClassDef = pack.classById(id)
    fun encounterById(id: String?): EncounterDefinition? = pack.encounterById(id)
    fun spellById(id: String?): Spell? = pack.spellById(id)
    fun spellsForClass(classId: String?): List<Spell> = pack.spellsForClass(classId)
    fun voiceAliasesFor(memberId: String?): List<String> = pack.voiceAliasesFor(memberId)
    fun enemyTemplateById(id: String?): EnemyTemplate? = pack.enemyTemplateById(id)
    fun heroSpellsFor(memberId: String = "hero"): List<Spell> = pack.heroSpellsFor(memberId)
    fun companionSpellsFor(memberId: String): List<Spell> = pack.companionSpellsFor(memberId)
    fun companionStarterSpellsFor(memberId: String): List<Spell> = pack.companionStarterSpellsFor(memberId)
    fun breathSpellFor(memberId: String): Spell? = pack.breathSpellFor(memberId)
    fun companionMember(memberId: String): PartyMember? = pack.companionMember(memberId)
    fun createDuoParty(): List<PartyMember> = pack.createDuoParty()
    fun createMinion(idSuffix: String, name: String = "Template Minion", subtitle: String = "Minion", hp: Int = 150): Enemy =
        pack.createMinion(idSuffix, name, subtitle, hp)
}

/** Parses "#RRGGBB" or "#AARRGGBB" into a Compose color without touching android.graphics. */
fun parseHexColor(hex: String?, fallback: Color = Color.White): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.removePrefix("#").trim()
        val value = clean.toLong(16)
        val argb = when (clean.length) {
            6 -> 0xFF000000L or value
            8 -> value
            else -> return fallback
        }
        Color(argb)
    } catch (_: Exception) {
        fallback
    }
}
