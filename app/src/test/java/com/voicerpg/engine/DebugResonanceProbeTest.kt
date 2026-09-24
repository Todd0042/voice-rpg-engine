package com.voicerpg.engine

import com.voicerpg.engine.content.GameContent
import com.voicerpg.engine.engine.NoveltyCache
import com.voicerpg.engine.engine.ResonanceEngine
import com.voicerpg.engine.engine.SpellChantPresets
import com.voicerpg.engine.engine.SpellThesaurus
import com.voicerpg.engine.model.AcousticProfile
import com.voicerpg.engine.model.ResonanceTier
import org.junit.Test

class DebugResonanceProbeTest {
    @Test
    fun probeRealValues() {
        val engine = ResonanceEngine(NoveltyCache())
        val sb = StringBuilder()
        val spell = GameContent.spells.getValue("power_strike")
        val preset = SpellChantPresets.getPreset(spell, ResonanceTier.TRANSCENDENTAL)
        val chant = preset.chantText
        val words = chant.lowercase().replace(Regex("[^a-z0-9\\s]"), "").split("\\s+".toRegex())
        val matched = SpellThesaurus.findMatches(chant, spell.school)
        val zeroAcoustic = AcousticProfile()
        val rZero = engine.evaluate(chant, spell.school, zeroAcoustic, ignoreNoveltyDecay = true)
        val rT = engine.evaluate(chant, spell.school, preset.acousticProfile, ignoreNoveltyDecay = true)
        sb.appendLine("chant=$chant")
        sb.appendLine("wordCount=${words.size}")
        sb.appendLine("matched=$matched  matchCount=${matched.size}")
        sb.appendLine("withZeroAcoustic: bonus=${rZero.bonusPercent} tier=${rZero.tier}")
        sb.appendLine("withTranscAcoustic: bonus=${rT.bonusPercent} tier=${rT.tier}")
        sb.appendLine("fullAcoustic: peak=${preset.acousticProfile.peakVolumeDb} vol=${preset.acousticProfile.volumeScore} inf=${preset.acousticProfile.inflectionScore}")
        val out = java.io.File("/tmp/opencode/probe-output2.txt")
        out.writeText(sb.toString())
        println(sb.toString())
    }
}