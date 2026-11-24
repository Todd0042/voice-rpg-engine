package com.voicerpg.engine.ui.fx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.exp

/**
 * One combatant's live effects, read by its sprite (PixelCharacterView) every frame. The director
 * starts them; [step] runs them down. Knockback and squash run on arena time (they freeze in a
 * hit-stop); the white flash and glow run on real time (they stay visible through it).
 */
class CombatantFx {
    /** 0..1: the sprite is drawn toward solid white. */
    var flash by mutableFloatStateOf(0f)
        private set

    /** dp along x: positive toward the enemy side. */
    var knockback by mutableFloatStateOf(0f)
        private set

    /** -1..1: positive squashes (wider, shorter), negative stretches. */
    var squash by mutableFloatStateOf(0f)
        private set

    /** A coloured aura behind the sprite (wind-up, charge); [glowStrength] 0..1. */
    var glow by mutableStateOf<Color?>(null)
    var glowStrength by mutableFloatStateOf(0f)

    /** A telegraphed intent above the sprite ("⚔", "☄"…), or null. */
    var intent by mutableStateOf<String?>(null)

    /** 0..1 fill of a boss's ultimate charge bar, or null when not charging. */
    var ultimateCharge by mutableStateOf<Float?>(null)

    /** The sprite has shattered (or faded) away. */
    var hidden by mutableStateOf(false)

    /** Leaning forward to attack (wind-up), 0..1. */
    var lean by mutableFloatStateOf(0f)

    private var knockAmp = 0f
    private var knockT = 0f
    private var squashAmp = 0f
    private var squashT = 0f

    fun hit(direction: Float, amplitudeDp: Float) {
        flash = 1f
        knockAmp = direction * amplitudeDp
        knockT = 0f
        knockback = knockAmp
        squashAmp = 0.35f
        squashT = 0f
    }

    /** A small stretch then settle (casting, attacking). */
    fun pulse(strength: Float = 0.25f) {
        squashAmp = -strength
        squashT = 0f
    }

    fun step(realSeconds: Float, fxSeconds: Float) {
        if (flash > 0f) flash = (flash - realSeconds / FLASH_SECONDS).coerceAtLeast(0f)
        if (knockAmp != 0f) {
            knockT += fxSeconds
            val e = exp(-knockT / KNOCK_DECAY)
            knockback = knockAmp * e * cos(knockT * KNOCK_RATE)
            if (e < 0.02f) { knockAmp = 0f; knockback = 0f }
        }
        if (squashAmp != 0f) {
            squashT += fxSeconds
            val e = exp(-squashT / SQUASH_DECAY)
            squash = squashAmp * e * cos(squashT * SQUASH_RATE)
            if (e < 0.02f) { squashAmp = 0f; squash = 0f }
        }
    }

    fun reset() {
        flash = 0f; knockAmp = 0f; knockback = 0f; squashAmp = 0f; squash = 0f
        glow = null; glowStrength = 0f; intent = null; ultimateCharge = null; hidden = false; lean = 0f
    }

    private companion object {
        const val FLASH_SECONDS = 0.14f
        const val KNOCK_DECAY = 0.11f
        const val KNOCK_RATE = 16f
        const val SQUASH_DECAY = 0.09f
        const val SQUASH_RATE = 30f
    }
}
