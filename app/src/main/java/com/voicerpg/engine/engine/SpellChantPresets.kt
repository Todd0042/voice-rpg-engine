package com.voicerpg.engine.engine

import com.voicerpg.engine.model.AcousticProfile
import com.voicerpg.engine.model.ResonanceTier
import com.voicerpg.engine.model.Spell

data class ChantPreset(
    val chantText: String,
    val acousticProfile: AcousticProfile,
    val tier: ResonanceTier,
    val label: String
)

/**
 * Generates finely-tuned incantations paired with acoustic profiles designed to hit
 * the requested resonance tier for hands-free and simulator testing. Presets are built
 * generically from any spell's name — games can extend this with bespoke chants.
 *
 * Tier targets:
 * - BASIC: 0% .. 15% (1.0x .. 1.15x) -> Target ~10%
 * - ADEPT: 20% .. 50% (1.20x .. 1.50x) -> Target ~48%
 * - MASTER: 55% .. 95% (1.55x .. 1.95x) -> Target ~87%
 * - MYTHIC: 100% .. 150% (2.0x .. 2.50x) -> Target ~142%
 * - TRANSCENDENTAL: 155% .. 200% (2.55x .. 3.0x MAX) -> Target 200% MAX
 */
object SpellChantPresets {

    fun getPreset(spell: Spell, tier: ResonanceTier): ChantPreset {
        val (chant, acoustic) = presetFor(spell, tier)

        val label = when (tier) {
            ResonanceTier.BASIC -> "10% Basic"
            ResonanceTier.ADEPT -> "40% Adept"
            ResonanceTier.MASTER -> "80% Master"
            ResonanceTier.MYTHIC -> "125% Mythic"
            ResonanceTier.TRANSCENDENTAL -> "200% MAX"
        }

        return ChantPreset(
            chantText = chant,
            acousticProfile = acoustic,
            tier = tier,
            label = label
        )
    }

    private fun presetFor(spell: Spell, tier: ResonanceTier): Pair<String, AcousticProfile> {
        // Generic, deterministic chants: word length + poetic phrasing + the fixed
        // acoustic profile for each tier are chosen so the resonance engine lands inside
        // the target bracket for ANY spell whose name matches a single thematic root.
        return when (tier) {
            ResonanceTier.BASIC -> spell.name to AcousticProfile.BASIC
            ResonanceTier.ADEPT -> spell.name to AcousticProfile.ADEPT
            ResonanceTier.MASTER -> "Unleash ${spell.name} now" to AcousticProfile.MASTER
            ResonanceTier.MYTHIC -> "By the power of ancient song, now cast ${spell.name}" to AcousticProfile.MYTHIC
            ResonanceTier.TRANSCENDENTAL ->
                "O primordial celestial forces descend from the heavens, unleash ${spell.name} to obliterate all in eternal victory once and for all!" to AcousticProfile.TRANSCENDENTAL
        }
    }
}
