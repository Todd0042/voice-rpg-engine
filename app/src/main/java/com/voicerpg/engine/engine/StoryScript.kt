package com.voicerpg.engine.engine

import androidx.compose.ui.graphics.Color
import com.voicerpg.engine.model.DialogueChoice
import com.voicerpg.engine.model.DialogueNode
import com.voicerpg.engine.model.DialogueSpeaker
import com.voicerpg.engine.model.SpeakerSide
import com.voicerpg.engine.model.StoryScene
import com.voicerpg.engine.template.TemplateEncounters

/**
 * ========================================================================
 * VOICE RPG ENGINE: Core Story Script & Narrative State Machine
 * ========================================================================
 * This is the primary narrative script for your game.
 * Replace or extend the scenes, speakers, and dialogue nodes below to author
 * your custom voice-commanded adventure.
 */
object StoryScript {

    // -------------------------------------------------------------------------
    // 1. Dialogue Speakers
    // -------------------------------------------------------------------------
    val NARRATOR = DialogueSpeaker(
        id = "narrator",
        name = "Narrator",
        title = "The Chronicle",
        portraitAsset = "portraits/narrator.jpg",
        themeColor = Color(0xFFFFD700),
        ttsPitch = 1.0f
    )

    val MENTOR = DialogueSpeaker(
        id = "mentor",
        name = "Grandmaster Vael",
        title = "Chamber Warden",
        portraitAsset = "portraits/cedric.jpg",
        themeColor = Color(0xFFFFB74D),
        ttsPitch = 0.85f // Distinct low mentor voice pitch
    )

    // -------------------------------------------------------------------------
    // 2. Story Scenes
    // -------------------------------------------------------------------------
    val SCENE_PROVING_GROUNDS = StoryScene(
        id = "scene_proving_grounds",
        name = "The Proving Grounds",
        chapterTitle = "Prologue: The Echoing Chamber",
        backgroundAsset = "story/bell_chamber.jpg",
        initialNodeId = "template_linear_narration",
        ambientDescription = "An ancient stone vault with glyphs pulsing faintly in response to the human voice.",
        musicAsset = "audio/music/bgm_title.ogg"
    )

    val ALL_SCENES: Map<String, StoryScene> = mapOf(
        SCENE_PROVING_GROUNDS.id to SCENE_PROVING_GROUNDS
    )

    // -------------------------------------------------------------------------
    // 3. Dialogue Nodes & Choice Graph
    // -------------------------------------------------------------------------
    val NODE_INTRO = DialogueNode(
        id = "template_linear_narration",
        speaker = NARRATOR,
        side = SpeakerSide.CENTER_NARRATOR,
        text = "Deep beneath the citadel, the stone floor hums with forgotten frequency. Grandmaster Vael stands before the proving pedestal, awaiting your vocal incantation.",
        nextNodeId = "template_mentor_greeting"
    )

    val NODE_MENTOR_GREETING = DialogueNode(
        id = "template_mentor_greeting",
        speaker = MENTOR,
        side = SpeakerSide.LEFT,
        text = "Welcome, apprentice. In this sanctuary, your spoken voice is both blade and aegis. Inquire of the ancient ways, or step forward into combat.",
        nextNodeId = "template_choice_hub"
    )

    val NODE_CHOICE_HUB = DialogueNode(
        id = "template_choice_hub",
        speaker = MENTOR,
        side = SpeakerSide.LEFT,
        text = "What is your command? Speak a choice aloud or tap the screen.",
        choices = listOf(
            DialogueChoice(
                id = "choice_history",
                text = "Ask about the history of the Proving Grounds.",
                voiceKeywords = listOf("history", "grounds", "lore", "past", "ancient"),
                nextNodeId = "template_branch_history",
                completionFlag = "flag_template_history_read"
            ),
            DialogueChoice(
                id = "choice_blessing",
                text = "Receive the Grandmaster's tactical counsel.",
                voiceKeywords = listOf("blessing", "counsel", "advice", "tactical", "help"),
                nextNodeId = "template_branch_blessing",
                completionFlag = "flag_template_blessing_received"
            ),
            DialogueChoice(
                id = "choice_solo_battle",
                text = "Begin Single-Enemy Combat Trial (1 Drone).",
                voiceKeywords = listOf("single", "solo", "one enemy", "drone", "first trial"),
                nextNodeId = "template_trigger_solo_combat"
            ),
            DialogueChoice(
                id = "choice_trio_battle",
                text = "Begin Squad Combat Trial (3 Drones).",
                voiceKeywords = listOf("squad", "trio", "three enemies", "group", "second trial"),
                nextNodeId = "template_trigger_trio_combat"
            )
        )
    )

    val NODE_BRANCH_HISTORY = DialogueNode(
        id = "template_branch_history",
        speaker = MENTOR,
        side = SpeakerSide.LEFT,
        text = "Centuries ago, the first invocators discovered that acoustic clarity directly amplifies elemental force. Every crisp syllable shatters enemy armor.",
        setFlagOnEnter = "flag_template_history_read",
        nextNodeId = "template_choice_hub"
    )

    val NODE_BRANCH_BLESSING = DialogueNode(
        id = "template_branch_blessing",
        speaker = MENTOR,
        side = SpeakerSide.LEFT,
        text = "Remember: your Flame Spark hits ice-attuned automatons for double damage, while Power Strike penetrates physical shielding.",
        setFlagOnEnter = "flag_template_blessing_received",
        nextNodeId = "template_choice_hub"
    )

    val NODE_TRIGGER_SOLO_COMBAT = DialogueNode(
        id = "template_trigger_solo_combat",
        speaker = NARRATOR,
        side = SpeakerSide.CENTER_NARRATOR,
        text = "A single training drone hums to life, gears spinning as it targets the fellowship!",
        triggerBattleEncounterId = TemplateEncounters.ENCOUNTER_SOLO_ID,
        nextNodeId = "template_post_victory"
    )

    val NODE_TRIGGER_TRIO_COMBAT = DialogueNode(
        id = "template_trigger_trio_combat",
        speaker = NARRATOR,
        side = SpeakerSide.CENTER_NARRATOR,
        text = "Three automated drones deploy in synchronized combat formation!",
        triggerBattleEncounterId = TemplateEncounters.ENCOUNTER_TRIO_ID,
        nextNodeId = "template_post_victory"
    )

    val NODE_POST_VICTORY = DialogueNode(
        id = "template_post_victory",
        speaker = MENTOR,
        side = SpeakerSide.LEFT,
        text = "Outstanding performance! Your incantations were resolute and your timing flawless. The Proving Grounds yield to your voice.",
        nextNodeId = "template_conclusion"
    )

    val NODE_CONCLUSION = DialogueNode(
        id = "template_conclusion",
        speaker = NARRATOR,
        side = SpeakerSide.CENTER_NARRATOR,
        text = "The trial is complete. You now possess the blueprint to create new chapters, heroes, spells, and battles. Returning to the Proving Grounds hub.",
        nextNodeId = "template_choice_hub"
    )

    val ALL_NODES: Map<String, DialogueNode> = listOf(
        NODE_INTRO,
        NODE_MENTOR_GREETING,
        NODE_CHOICE_HUB,
        NODE_BRANCH_HISTORY,
        NODE_BRANCH_BLESSING,
        NODE_TRIGGER_SOLO_COMBAT,
        NODE_TRIGGER_TRIO_COMBAT,
        NODE_POST_VICTORY,
        NODE_CONCLUSION
    ).associateBy { it.id }
}
