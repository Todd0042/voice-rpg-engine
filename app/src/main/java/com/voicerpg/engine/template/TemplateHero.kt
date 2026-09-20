package com.voicerpg.engine.template

import androidx.compose.ui.graphics.Color
import com.voicerpg.engine.model.SpellSchool
import com.voicerpg.engine.model.PartyMember
import com.voicerpg.engine.model.Spell

/**
 * ========================================================================
 * CREATOR TEMPLATE: Playable Hero Character & Spells
 * ========================================================================
 * Use this file as a blueprint to create playable heroes for your custom RPG.
 *
 * Each hero has:
 * - Base stats (HP, MP, Speed for ATB initiative, Defense)
 * - Visual tint color for party UI and sprite canvas
 * - List of voice-enabled spells (Physical, Elemental, Healing, Buff)
 */
object TemplateHero {

    /**
     * Physical Single-Target Attack Spell
     * Voice Chants: "Strike!", "Slash!", "Attack!", "Blade!"
     */
    val STRIKE_SPELL = Spell(
        id = "template_strike",
        name = "Power Strike",
        school = SpellSchool.PHYSICAL,
        basePower = 40,
        mpCost = 0,
        description = "A swift, heavy blade strike.",
        exampleChant = "Strike!",
        aliases = listOf("strike", "slash", "attack", "blade", "cleave")
    )

    /**
     * Elemental Fire Spell (Hits weakness on fire-vulnerable foes)
     * Voice Chants: "Flame spark!", "Burn!", "Ignite!", "Fire!"
     */
    val SPARK_SPELL = Spell(
        id = "template_spark",
        name = "Flame Spark",
        school = SpellSchool.PYROMANCY,
        basePower = 55,
        mpCost = 15,
        description = "Hurls a searing burst of fire.",
        exampleChant = "Flame spark!",
        aliases = listOf("spark", "flame", "burn", "fire", "ignite")
    )

    /**
     * Restoration Healing Spell
     * Voice Chants: "Mend!", "Heal!", "Restore!", "Cure!"
     */
    val MEND_SPELL = Spell(
        id = "template_mend",
        name = "Mending Touch",
        school = SpellSchool.HOLY,
        basePower = 50,
        mpCost = 20,
        isHeal = true,
        description = "Restores hit points to the acting hero or an ally.",
        exampleChant = "Mending touch!",
        aliases = listOf("heal", "mend", "restore", "cure", "soothe")
    )

    /**
     * Defensive Shield Spell
     * Voice Chants: "Defend!", "Guard!", "Aegis!"
     */
    val GUARD_SPELL = Spell(
        id = "template_guard",
        name = "Aegis Guard",
        school = SpellSchool.PHYSICAL,
        basePower = 0,
        mpCost = 10,
        isGuard = true,
        description = "Raises a defensive stance, reducing incoming damage.",
        exampleChant = "Aegis guard!",
        aliases = listOf("guard", "defend", "shield", "protect")
    )

    /**
     * Factory function to instantiate the template hero.
     */
    fun createTemplateAdventurer(
        id: String = "template_hero",
        name: String = "Adventurer",
        currentHp: Int = 200,
        maxHp: Int = 200,
        currentMp: Int = 80,
        maxMp: Int = 80
    ): PartyMember {
        return PartyMember(
            id = id,
            name = name,
            loreClass = "Spellsword",
            currentHp = currentHp,
            maxHp = maxHp,
            currentMp = currentMp,
            maxMp = maxMp,
            speed = 60,
            defense = 12,
            avatarTint = Color(0xFF64B5F6),
            spells = listOf(STRIKE_SPELL, SPARK_SPELL, MEND_SPELL, GUARD_SPELL)
        )
    }
}
