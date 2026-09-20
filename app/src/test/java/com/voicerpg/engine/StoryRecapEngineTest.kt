package com.voicerpg.engine

import com.voicerpg.engine.engine.StoryRecapEngine
import com.voicerpg.engine.engine.StoryScript
import com.voicerpg.engine.model.SavedCharacterStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryRecapEngineTest {

    @Test
    fun testPrologueRecap() {
        val scene = StoryScript.SCENE_PROVING_GROUNDS
        val node = StoryScript.NODE_INTRO
        val flags = emptyMap<String, Boolean>()
        val party = listOf(
            SavedCharacterStats(id = "hero", name = "Adventurer", loreClass = "Spellsword", currentHp = 200, maxHp = 200, currentMp = 80, maxMp = 80, speed = 60)
        )

        val recap = StoryRecapEngine.buildRecap(scene, node, flags, party)

        assertNotNull(recap.currentActTitle)
        assertEquals("Prologue: The Echoing Chamber", recap.currentChapterTitle)
        assertEquals("The Proving Grounds", recap.currentSceneName)
        assertTrue(recap.milestones.isNotEmpty())
        assertTrue(recap.milestones.first().isCurrent)
        assertTrue(recap.spokenRecap.contains("The Proving Grounds"))
    }
}
