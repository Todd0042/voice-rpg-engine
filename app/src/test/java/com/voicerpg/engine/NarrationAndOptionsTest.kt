package com.voicerpg.engine

import com.voicerpg.engine.audio.CombatNarrator
import com.voicerpg.engine.audio.SpeechManager
import com.voicerpg.engine.audio.SpeechState
import com.voicerpg.engine.engine.IntentParser
import com.voicerpg.engine.engine.SaveManager
import com.voicerpg.engine.engine.StoryEncounters
import com.voicerpg.engine.engine.StoryScript
import com.voicerpg.engine.model.DialogueChoice
import com.voicerpg.engine.model.DialogueSpeaker
import com.voicerpg.engine.model.GameSaveData
import com.voicerpg.engine.model.GameScreen
import com.voicerpg.engine.model.MetaCommand
import com.voicerpg.engine.model.PlayerCustomization
import com.voicerpg.engine.viewmodel.StoryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NarrationAndOptionsTest {

    @org.junit.Test
    fun speakerAttributionToggleMetaCommandRecognized() {
        val sayHi = com.voicerpg.engine.engine.IntentParser.parse("toggle speaker names", emptyList())
        org.junit.Assert.assertEquals(com.voicerpg.engine.model.MetaCommand.TOGGLE_SPEAKER_ATTRIBUTION, sayHi.metaCommand)
        val who = com.voicerpg.engine.engine.IntentParser.parse("who is speaking", emptyList())
        org.junit.Assert.assertEquals(com.voicerpg.engine.model.MetaCommand.TOGGLE_SPEAKER_ATTRIBUTION, who.metaCommand)
        val plain = com.voicerpg.engine.engine.IntentParser.parse("fireball the orc", emptyList())
        org.junit.Assert.assertEquals(com.voicerpg.engine.model.MetaCommand.NONE, plain.metaCommand)
    }


    private lateinit var narrator: CombatNarrator
    private lateinit var speechManager: SpeechManager
    private lateinit var saveManager: SaveManager
    private lateinit var storyViewModel: StoryViewModel

    @Before
    fun setUp() {
        narrator = CombatNarrator(context = null)
        speechManager = SpeechManager(context = null)
        saveManager = SaveManager(context = null)
        val testScope = CoroutineScope(Dispatchers.Default)
        storyViewModel = StoryViewModel(
            speechManager = speechManager,
            combatNarrator = narrator,
            saveManager = saveManager,
            scopeOverride = testScope
        )
        storyViewModel.startNewGame(PlayerCustomization(name = "Kaelen"))
    }

    @Test
    fun testIntentParserRecognizesNarrationMetaCommands() {
        val intentNarration = IntentParser.parse("Please toggle narration now")
        assertEquals(MetaCommand.TOGGLE_NARRATION, intentNarration.metaCommand)

        val intentChoices = IntentParser.parse("read choices aloud")
        assertEquals(MetaCommand.TOGGLE_READ_CHOICES, intentChoices.metaCommand)

        val intentOptions = IntentParser.parse("open options")
        assertEquals(MetaCommand.OPEN_OPTIONS, intentOptions.metaCommand)

        val intentClose = IntentParser.parse("close options")
        assertEquals(MetaCommand.CLOSE_OPTIONS, intentClose.metaCommand)
    }

    @Test
    fun testCombatNarratorToggles() {
        // Narration toggle
        assertTrue(narrator.isNarrationEnabled.value)
        narrator.toggleNarration()
        assertFalse(narrator.isNarrationEnabled.value)
        narrator.setNarrationEnabled(true)
        assertTrue(narrator.isNarrationEnabled.value)

        // Read choices toggle
        assertTrue(narrator.isReadChoicesEnabled.value)
        narrator.toggleReadChoices()
        assertFalse(narrator.isReadChoicesEnabled.value)
        narrator.setReadChoicesEnabled(true)
        assertTrue(narrator.isReadChoicesEnabled.value)

        // Speech rate
        narrator.setSpeechRate(1.25f)
        assertEquals(1.25f, narrator.speechRate.value, 0.01f)

        // Character pitch
        assertTrue(narrator.isCharacterPitchEnabled.value)
        narrator.setCharacterPitchEnabled(false)
        assertFalse(narrator.isCharacterPitchEnabled.value)
    }

    @Test
    fun testCombatNarratorMultiVoiceConfiguration() {
        // When running in headless context, installed voices default safely to empty
        assertTrue(narrator.getInstalledVoices().isEmpty())
        assertEquals(0, narrator.availableVoiceCount.value)

        // Verifying speaker voice mapping getters return gracefully without throwing
        val cedricVoice = narrator.getVoiceForSpeaker(DialogueSpeaker.CEDRIC)
        val aethelVoice = narrator.getVoiceForSpeaker(DialogueSpeaker.AETHEL)
        val narratorVoice = narrator.getVoiceForSpeaker(DialogueSpeaker.NARRATOR)

        // Without hardware TTS engine, speaker voices are null
        assertEquals(null, cedricVoice)
        assertEquals(null, aethelVoice)
        assertEquals(null, narratorVoice)
    }

    @Test
    fun testCombatNarratorPreferredVoiceIdsConfiguredForAllSpeakers() {
        val preferred = CombatNarrator.PREFERRED_VOICE_IDS
        assertEquals(6, preferred.size)
        assertEquals("en-gb-x-gbd-local", preferred[DialogueSpeaker.CEDRIC.id]) // Sir Cedric
        assertEquals("en-gb-x-gba-local", preferred[DialogueSpeaker.AETHEL.id]) // Aethel the Invocator
        assertEquals("en-us-x-tpf-local", preferred[DialogueSpeaker.LYRA.id]) // Lyra the Grove Warden
        assertEquals("en-us-x-tpc-local", preferred[DialogueSpeaker.ZEPHYR.id]) // Zephyr the Shadowblade
        assertEquals("en-us-x-tpd-local", preferred[DialogueSpeaker.MALAKOR.id]) // Grand Inquisitor Malakor
        assertEquals("en-gb-x-rjs-local", preferred[DialogueSpeaker.NARRATOR.id]) // The Chronicle
    }

    @Test
    fun testResolvePreferredVoiceNameRespectsPersistedThenPreferredThenHeuristic() {
        val installed = setOf(
            "en-gb-x-gbd-local", "en-us-x-tpf-local", "en-gb-x-rjs-local",
            "en-us-x-tpc-local", "en-us-x-tpd-local", "en-gb-x-gba-local", "en-us-x-iob-local"
        )
        val preferredDefault = CombatNarrator.PREFERRED_VOICE_IDS

        // Heuristic alone is used when neither a persisted override nor the preferred pack is installed
        assertEquals(
            "en-us-x-iob-local",
            CombatNarrator.resolvePreferredVoiceName(
                DialogueSpeaker.CEDRIC.id, "en-us-x-iob-local",
                setOf("en-us-x-iob-local", "en-gb-x-rjs-local"), emptyMap(), preferredDefault
            )
        )

        // First-try preferred pack overrides the heuristic when its voice model is installed
        assertEquals(
            "en-gb-x-gbd-local",
            CombatNarrator.resolvePreferredVoiceName(
                DialogueSpeaker.CEDRIC.id, "en-us-x-iob-local", installed, emptyMap(), preferredDefault
            )
        )

        // Persisted player override wins over both when its voice is still installed
        assertEquals(
            "en-us-x-iob-local",
            CombatNarrator.resolvePreferredVoiceName(
                DialogueSpeaker.CEDRIC.id, "en-us-x-iob-local", installed,
                mapOf(DialogueSpeaker.CEDRIC.id to "en-us-x-iob-local"), preferredDefault
            )
        )

        // Persisted override to a no-longer-installed voice falls back to the preferred pack
        assertEquals(
            "en-gb-x-gbd-local",
            CombatNarrator.resolvePreferredVoiceName(
                DialogueSpeaker.CEDRIC.id, "en-us-x-iob-local", installed,
                mapOf(DialogueSpeaker.CEDRIC.id to "en-us-x-gone-local"), preferredDefault
            )
        )
    }

    @Test
    fun testVoiceAssignmentsStayPendingHeadlessAndEmptySnapshot() {
        // Headless JVM has no TTS engine: assignments are held in reserve for onInit/refresh,
        // and the snapped assignments map is empty because no real voice models exist.
        val assignments = mapOf(
            DialogueSpeaker.CEDRIC.id to "en-gb-x-gbd-local",
            DialogueSpeaker.AETHEL.id to "en-gb-x-gba-local",
            DialogueSpeaker.LYRA.id to "en-us-x-tpf-local",
            DialogueSpeaker.ZEPHYR.id to "en-us-x-tpc-local",
            DialogueSpeaker.MALAKOR.id to "en-us-x-tpd-local",
            DialogueSpeaker.NARRATOR.id to "en-gb-x-rjs-local"
        )
        narrator.setVoiceAssignments(assignments)
        narrator.refreshInstalledVoices()

        assertTrue(narrator.getVoiceAssignments().isEmpty())

        // Persisting over a fresh save must not crash and the load path stays stable
        storyViewModel.persistCurrentState()
        val save = saveManager.load()
        assertNotNull(save)
        assertTrue(save!!.voiceAssignments.isEmpty())
    }

    @Test
    fun testNarrateDialogueInvokesCompletionCallbackEvenWhenMuted() {
        narrator.setNarrationEnabled(false)
        narrator.setEyesFreeMode(false)

        var completed = false
        val testChoices = listOf(
            DialogueChoice("1", "Look outside", emptyList(), "next_node")
        )
        narrator.narrateDialogue(DialogueSpeaker.CEDRIC, "Halt!", testChoices) {
            completed = true
        }

        assertTrue(completed)
    }

    @Test
    fun testVoiceCommandsInStoryViewModelToggleSettingsAndPersist() {
        // Initial state
        assertTrue(narrator.isNarrationEnabled.value)
        assertTrue(narrator.isReadChoicesEnabled.value)

        // Turn off narration via voice command
        storyViewModel.handleStoryVoiceInput("toggle narration")
        assertFalse(narrator.isNarrationEnabled.value)

        // Verify save data updated
        var save = saveManager.load()
        assertNotNull(save)
        assertFalse(save!!.isNarrationEnabled)

        // Turn off reading choices via voice command
        storyViewModel.handleStoryVoiceInput("toggle choices")
        assertFalse(narrator.isReadChoicesEnabled.value)

        save = saveManager.load()
        assertNotNull(save)
        assertFalse(save!!.isReadChoicesEnabled)
    }

    @Test
    fun testOpenAndCloseOptionsCallbacks() {
        var openCalled = false
        var closeCalled = false
        storyViewModel.onOpenOptions = { openCalled = true }
        storyViewModel.onCloseOptions = { closeCalled = true }

        storyViewModel.handleStoryVoiceInput("open options")
        assertTrue(openCalled)

        storyViewModel.handleStoryVoiceInput("close options")
        assertTrue(closeCalled)
    }

    @Test
    fun testNarrationSettingsPersistenceRoundTrip() {
        val custom = PlayerCustomization(name = "Lyra")
        val initialSave = saveManager.createInitialSave(custom)

        val updatedSave = initialSave.copy(
            isNarrationEnabled = false,
            isReadChoicesEnabled = true,
            speechRate = 1.30f,
            isCharacterPitchEnabled = false
        )
        saveManager.save(updatedSave)

        val loaded = saveManager.load()
        assertNotNull(loaded)
        assertFalse(loaded!!.isNarrationEnabled)
        assertTrue(loaded.isReadChoicesEnabled)
        assertEquals(1.30f, loaded.speechRate, 0.01f)
        assertFalse(loaded.isCharacterPitchEnabled)
    }

    @Test
    fun testHandsFreeAutoListenSessionStateAndCancellation() {
        assertFalse(speechManager.isAutoListen.value)
        assertFalse(speechManager.isSessionActive)

        speechManager.setAutoListen(true)
        assertTrue(speechManager.isAutoListen.value)

        var recognizedText = ""
        speechManager.startListening { result ->
            recognizedText = result
        }

        // In headless unit test with context = null, isAvailable is false and SpeechState.Error is set
        assertFalse(speechManager.isAvailable)
        assertTrue(speechManager.speechState.value is SpeechState.Error)

        // Cancel cleanly resets to Idle
        speechManager.cancel()
        assertFalse(speechManager.isSessionActive)
        assertEquals(SpeechState.Idle, speechManager.speechState.value)
        assertEquals("", recognizedText)
    }

    @Test
    fun testCombatNarratorDialoguePreservesVoiceAndUtteranceTracking() {
        var callbackFired = false
        narrator.narrateDialogue(
            speaker = DialogueSpeaker.CEDRIC,
            text = "By the holy light, we stand firm!",
            choices = emptyList()
        ) {
            callbackFired = true
        }

        // Without initialized engine in unit tests, callback executes cleanly via fallback
        assertTrue(callbackFired)

        // Stop cleanly resets active utterance
        narrator.stop()
        assertFalse(narrator.isSpeaking.value)
    }

    @Test
    fun testAudioSetupScreenAndCompanionVoiceState() {
        // Verify installedVoiceCount flow exists and starts at 0 in headless test
        assertEquals(0, narrator.installedVoiceCount.value)
        assertEquals(0, narrator.availableVoiceCount.value)

        // Rescan / refresh voices runs gracefully
        narrator.refreshInstalledVoices()
        assertEquals(0, narrator.installedVoiceCount.value)

        // Verify speaker mapping returns null safely without throwing
        assertEquals(null, narrator.getVoiceForSpeaker(DialogueSpeaker.ZEPHYR))
        assertEquals(null, narrator.getVoiceForSpeaker(DialogueSpeaker.MALAKOR))
        assertEquals(null, narrator.getVoiceForSpeaker(DialogueSpeaker.LYRA))

        // Verify fresh story state begins at TITLE
        val freshVm = StoryViewModel(
            speechManager = speechManager,
            combatNarrator = narrator,
            saveManager = SaveManager(context = null),
            scopeOverride = CoroutineScope(Dispatchers.Default)
        )
        assertEquals(GameScreen.TITLE, freshVm.state.value.gameScreen)

        // Starting new game flow routes to AUDIO_SETUP then CHARACTER_CREATION
        freshVm.startNewGameFlow()
        assertEquals(GameScreen.AUDIO_SETUP, freshVm.state.value.gameScreen)
        freshVm.proceedToCharacterCreation()
        assertEquals(GameScreen.CHARACTER_CREATION, freshVm.state.value.gameScreen)

        // Reset returns to TITLE
        freshVm.resetGame()
        assertEquals(GameScreen.TITLE, freshVm.state.value.gameScreen)
    }

    @Test
    fun testCombatNarrationAndPocketGuardDecoupling() {
        // Default states
        assertTrue(narrator.isCombatNarrationEnabled.value)
        assertTrue(narrator.isPocketGuardEnabled.value)

        // Turn off combat narration
        narrator.setCombatNarrationEnabled(false)
        assertFalse(narrator.isCombatNarrationEnabled.value)

        // Turn off pocket touch guard
        narrator.setPocketGuardEnabled(false)
        assertFalse(narrator.isPocketGuardEnabled.value)
        assertFalse(narrator.isPocketGuardLocked.value)

        // Enabling eyes-free mode when pocket guard is disabled does not lock the screen guard
        narrator.setEyesFreeMode(true)
        assertTrue(narrator.isEyesFreeMode.value)
        assertFalse(narrator.isPocketGuardLocked.value)

        // Enabling pocket guard and locking locks the guard
        narrator.setPocketGuardEnabled(true)
        narrator.lockPocketGuard()
        assertTrue(narrator.isPocketGuardLocked.value)

        // Verify persistence via StoryViewModel
        storyViewModel.persistCurrentState()
        val save = saveManager.load()
        assertNotNull(save)
        assertTrue(save!!.isPocketGuardEnabled)
        assertFalse(save.isCombatNarrationEnabled)

        // Test restoring from save
        val freshNarrator = CombatNarrator(context = null)
        val freshVm = StoryViewModel(
            speechManager = speechManager,
            combatNarrator = freshNarrator,
            saveManager = saveManager,
            scopeOverride = CoroutineScope(Dispatchers.Default)
        )
        assertFalse(freshNarrator.isCombatNarrationEnabled.value)
        assertTrue(freshNarrator.isPocketGuardEnabled.value)
    }
}


