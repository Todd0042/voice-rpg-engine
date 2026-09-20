package com.voicerpg.engine

import com.voicerpg.engine.audio.CombatNarrator
import com.voicerpg.engine.audio.SpeechManager
import com.voicerpg.engine.engine.SaveManager
import com.voicerpg.engine.engine.StoryScript
import com.voicerpg.engine.model.GameScreen
import com.voicerpg.engine.model.SpeakerSide
import com.voicerpg.engine.template.TemplateEncounters
import com.voicerpg.engine.viewmodel.StoryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class StoryDialogueTest {

    private lateinit var storyViewModel: StoryViewModel
    private lateinit var testSaveDir: File

    @Before
    fun setUp() {
        testSaveDir = File(System.getProperty("java.io.tmpdir"), "voicerpg_engine_dialogue_test_${System.currentTimeMillis()}")
        testSaveDir.mkdirs()

        val saveManager = SaveManager(context = null)
        val speechManager = SpeechManager()
        val combatNarrator = CombatNarrator()

        storyViewModel = StoryViewModel(
            speechManager = speechManager,
            combatNarrator = combatNarrator,
            saveManager = saveManager,
            scopeOverride = CoroutineScope(Dispatchers.Default)
        )
        storyViewModel.startNewGame(com.voicerpg.engine.model.PlayerCustomization())
    }

    @After
    fun tearDown() {
        testSaveDir.deleteRecursively()
    }

    @Test
    fun testInitialStoryState() {
        val state = storyViewModel.state.value
        assertEquals("scene_proving_grounds", state.currentScene.id)
        assertEquals("template_linear_narration", state.currentNode.id)
        assertEquals(SpeakerSide.CENTER_NARRATOR, state.currentNode.side)
        assertTrue(state.currentNode.text.contains("Deep beneath the citadel"))
    }

    @Test
    fun testLinearNarrationAdvancesToMentor() {
        assertEquals("template_linear_narration", storyViewModel.state.value.currentNode.id)
        storyViewModel.advanceDialogue()
        assertEquals("template_mentor_greeting", storyViewModel.state.value.currentNode.id)
        assertEquals("mentor", storyViewModel.state.value.currentNode.speaker.id)
    }

    @Test
    fun testMentorGreetingAdvancesToChoiceHub() {
        storyViewModel.advanceDialogue() // to mentor
        storyViewModel.advanceDialogue() // to hub
        assertEquals("template_choice_hub", storyViewModel.state.value.currentNode.id)

        val choices = storyViewModel.state.value.currentNode.choices
        assertTrue("Choice hub must have choices", choices.size >= 4)
    }

    @Test
    fun testVoiceKeywordChoiceResolution() {
        storyViewModel.advanceDialogue()
        storyViewModel.advanceDialogue()
        assertEquals("template_choice_hub", storyViewModel.state.value.currentNode.id)

        // Spoken input "Tell me the history"
        storyViewModel.handleStoryVoiceInput("Tell me the ancient history of this place")
        assertEquals("template_branch_history", storyViewModel.state.value.currentNode.id)
        assertTrue(storyViewModel.state.value.narrativeFlags.containsKey("flag_template_history_read"))
    }

    @Test
    fun testChoiceEliminationOnVisit() {
        storyViewModel.advanceDialogue()
        storyViewModel.advanceDialogue()
        assertEquals("template_choice_hub", storyViewModel.state.value.currentNode.id)

        val initialCount = storyViewModel.state.value.currentNode.choices.size

        // Select the history choice
        val historyChoice = storyViewModel.state.value.currentNode.choices.first { it.id == "choice_history" }
        storyViewModel.selectChoice(historyChoice)
        assertEquals("template_branch_history", storyViewModel.state.value.currentNode.id)

        // Return to hub
        storyViewModel.advanceDialogue()
        assertEquals("template_choice_hub", storyViewModel.state.value.currentNode.id)

        // Choice must be eliminated from available choices
        val available = storyViewModel.state.value.currentNode.choices.filter { choice ->
            choice.completionFlag == null || storyViewModel.state.value.narrativeFlags[choice.completionFlag] != true
        }
        assertEquals(initialCount - 1, available.size)
        assertFalse(available.any { it.id == "choice_history" })
    }

    @Test
    fun testSoloBattleTriggerAndVictoryReturn() {
        storyViewModel.advanceDialogue()
        storyViewModel.advanceDialogue()
        assertEquals("template_choice_hub", storyViewModel.state.value.currentNode.id)

        val soloChoice = storyViewModel.state.value.currentNode.choices.first { it.id == "choice_solo_battle" }
        storyViewModel.selectChoice(soloChoice)

        assertEquals("template_trigger_solo_combat", storyViewModel.state.value.currentNode.id)
        assertEquals(TemplateEncounters.ENCOUNTER_SOLO_ID, storyViewModel.state.value.currentNode.triggerBattleEncounterId)

        // Advance into battle
        storyViewModel.advanceDialogue()
        assertEquals(GameScreen.COMBAT_ARENA, storyViewModel.state.value.gameScreen)

        // Win encounter
        storyViewModel.onCombatVictory()
        assertEquals(GameScreen.STORY_EXPLORATION, storyViewModel.state.value.gameScreen)
        assertEquals("template_post_victory", storyViewModel.state.value.currentNode.id)
        assertTrue(storyViewModel.state.value.defeatedEncounters.contains(TemplateEncounters.ENCOUNTER_SOLO_ID))
    }

    @Test
    fun testSquadBattleTriggerAndVictoryReturn() {
        storyViewModel.advanceDialogue()
        storyViewModel.advanceDialogue()
        assertEquals("template_choice_hub", storyViewModel.state.value.currentNode.id)

        val trioChoice = storyViewModel.state.value.currentNode.choices.first { it.id == "choice_trio_battle" }
        storyViewModel.selectChoice(trioChoice)

        assertEquals("template_trigger_trio_combat", storyViewModel.state.value.currentNode.id)
        assertEquals(TemplateEncounters.ENCOUNTER_TRIO_ID, storyViewModel.state.value.currentNode.triggerBattleEncounterId)

        // Advance into battle
        storyViewModel.advanceDialogue()
        assertEquals(GameScreen.COMBAT_ARENA, storyViewModel.state.value.gameScreen)

        // Win encounter
        storyViewModel.onCombatVictory()
        assertEquals(GameScreen.STORY_EXPLORATION, storyViewModel.state.value.gameScreen)
        assertEquals("template_post_victory", storyViewModel.state.value.currentNode.id)
    }

    @Test
    fun testDialogueBacklogAppendsEntries() {
        val initialHistory = storyViewModel.state.value.dialogueHistory
        assertTrue("Initial history should have the intro node", initialHistory.isNotEmpty())
        assertEquals("Narrator", initialHistory.first().speakerName)

        storyViewModel.advanceDialogue()
        val secondHistory = storyViewModel.state.value.dialogueHistory
        assertEquals(2, secondHistory.size)
        assertEquals("Grandmaster Vael", secondHistory.last().speakerName)
    }

    @Test
    fun testNoDialogueNodeIsDeadEnd() {
        StoryScript.ALL_NODES.values.forEach { node ->
            if (node.choices.isEmpty() && node.triggerBattleEncounterId == null) {
                assertNotNull(
                    "Node '${node.id}' is a dead end: no choices, no battle trigger, and no nextNodeId",
                    node.nextNodeId
                )
            }
        }
    }
}
