package com.voicerpg.engine

import com.voicerpg.engine.model.SpellSchool
import com.voicerpg.engine.template.TemplateEncounters
import com.voicerpg.engine.template.TemplateEnemy
import com.voicerpg.engine.template.TemplateHero
import com.voicerpg.engine.template.TemplateStory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateEngineTest {

    @Test
    fun testTemplateHeroInitialization() {
        val hero = TemplateHero.createTemplateAdventurer()
        assertEquals("Adventurer", hero.name)
        assertEquals(200, hero.maxHp)
        assertEquals(80, hero.maxMp)
        assertTrue(hero.isAlive)
        assertEquals(4, hero.spells.size)

        val strike = hero.spells.first { it.id == "template_strike" }
        assertEquals(SpellSchool.PHYSICAL, strike.school)
        assertTrue(strike.aliases.contains("strike"))

        val flame = hero.spells.first { it.id == "template_spark" }
        assertEquals(SpellSchool.PYROMANCY, flame.school)
        assertTrue(flame.aliases.contains("spark"))

        val mend = hero.spells.first { it.id == "template_mend" }
        assertTrue(mend.isHeal)
        assertTrue(mend.aliases.contains("heal"))
    }

    @Test
    fun testTemplateEnemySpawningSingleAndSquad() {
        val soloEnemy = TemplateEnemy.createTemplateEnemy("drone_test", "Test Drone")
        assertEquals("drone_test", soloEnemy.id)
        assertEquals(100, soloEnemy.maxHp)
        assertTrue(soloEnemy.isAlive)

        // Test spawning a squad of 3 enemies with unique IDs
        val squad = TemplateEnemy.spawnSquad(3, "Combat Drone")
        assertEquals(3, squad.size)
        val ids = squad.map { it.id }.toSet()
        assertEquals(3, ids.size) // IDs must be unique
        assertTrue(squad.all { it.isAlive })
        assertTrue(squad.all { it.name.startsWith("Combat Drone") })
    }

    @Test
    fun testTemplateEncountersConfigured() {
        val solo = TemplateEncounters.TEMPLATE_SOLO_BATTLE
        assertEquals(1, solo.enemies.size)
        assertNotNull(solo.initialParty)
        assertEquals(1, solo.initialParty?.size)

        val trio = TemplateEncounters.TEMPLATE_TRIO_BATTLE
        assertEquals(3, trio.enemies.size)
        val enemyIds = trio.enemies.map { it.id }.toSet()
        assertEquals(3, enemyIds.size)
    }

    @Test
    fun testTemplateStoryGraphIntegrity() {
        val hub = TemplateStory.NODE_CHOICE_HUB
        assertTrue(hub.choices.size >= 4)

        // Verify choices have voice keywords and valid next nodes
        hub.choices.forEach { choice ->
            assertTrue(choice.voiceKeywords.isNotEmpty())
            assertTrue(TemplateStory.ALL_TEMPLATE_NODES.containsKey(choice.nextNodeId))
        }

        // Verify choice elimination flags exist on inquiry branches
        val loreChoice = hub.choices.first { it.id == "choice_history" }
        assertNotNull(loreChoice.completionFlag)

        val blessingChoice = hub.choices.first { it.id == "choice_blessing" }
        assertNotNull(blessingChoice.completionFlag)

        // Verify combat triggers lead to encounters
        val combatNode = TemplateStory.NODE_TRIGGER_SOLO_COMBAT
        assertEquals(TemplateEncounters.ENCOUNTER_SOLO_ID, combatNode.triggerBattleEncounterId)
        assertEquals("template_post_victory", combatNode.nextNodeId)
    }
}
