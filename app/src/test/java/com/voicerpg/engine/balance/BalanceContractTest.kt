package com.voicerpg.engine.balance

import com.voicerpg.engine.combat.AutoPolicy
import com.voicerpg.engine.content.GameContent
import com.voicerpg.engine.story.StoryRouteWalker
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Contract test (the game's own content): its story fights get harder as the story goes on, effort matters both
 * ways, nothing loops, every spell earns its place and Pure Story Mode always wins (plan E10).
 *
 * Every fight on every route (each campaign track, department or hero class) is played by [BattleSimulator] with
 * the party, stats and spells the story has given them at that point:
 *  - a SENSIBLE player at the game's balance point (`balanceResonance`, x1.5 by default), three seeds: the targets;
 *  - the same player at x1.1 (lazy) and x1.8 (bold), and a SPAM player at the balance point: effort matters;
 *  - Pure Story Mode (FASTEST at x1.8): it must win every fight.
 *
 * Writes `app/build/reports/balance.txt` every run; fails only for a game whose `combat/balance.json` has
 * `"enforce": 1`.
 */
class BalanceContractTest {

    enum class Kind(val label: String, val rounds: IntRange, val margin: ClosedFloatingPointRange<Float>) {
        EARLY("early", 2..5, 2.0f..4.5f),
        MID("mid", 3..6, 1.4f..3.0f),
        LATE("late", 3..7, 1.15f..2.2f),
        BOSS_MID("boss", 5..11, 1.1f..2.0f),
        BOSS_LATE("late boss", 5..11, 1.05f..1.6f),
        FINALE("finale", 9..15, 1.0f..1.4f)
    }

    data class Row(
        val route: String, val encounterId: String, val kind: Kind,
        val rounds: Int, val margin: Float, val winRate: Float, val support: Float,
        val lazyWon: Boolean, val lazyMargin: Float, val boldWon: Boolean, val boldMargin: Float,
        val spamRounds: Int, val spamWon: Boolean, val storyModeWon: Boolean, val stuck: Boolean,
        val problems: List<String>
    )

    private fun fmt(f: Float) = String.format(Locale.US, "%.2f", f)

    @Test
    fun `story fights meet the balance targets`() {
        val balance = GameContent.balance
        val at = balance.balanceResonance
        val rows = mutableListOf<Row>()
        val usedByRoute = mutableMapOf<String, MutableSet<String>>()
        val kitByRoute = mutableMapOf<String, MutableSet<String>>()

        for (route in StoryRouteWalker.routes()) {
            val battles = StoryRouteWalker.battlesOn(route).filter { GameContent.encounterById(it.encounterId) != null }
            val lastBoss = battles.lastOrNull { b -> GameContent.encounterById(b.encounterId)!!.enemies.any { it.isBoss } }
            for (b in battles) {
                val encounter = GameContent.encounterById(b.encounterId)!!
                val setup = BattleSimulator.FightSetup(encounter, b.player, b.flags, b.partyStats)
                val isBoss = encounter.enemies.any { it.isBoss }
                val kind = when {
                    b == lastBoss -> Kind.FINALE
                    isBoss && b.progress >= 0.66f -> Kind.BOSS_LATE
                    isBoss -> Kind.BOSS_MID
                    b.progress < 0.33f -> Kind.EARLY
                    b.progress < 0.66f -> Kind.MID
                    else -> Kind.LATE
                }
                val sensible = (1..3).map { BattleSimulator.run(setup, AutoPolicy.SENSIBLE, at, seed = it) }
                val lazy = BattleSimulator.run(setup, AutoPolicy.SENSIBLE, 1.1f, seed = 7)
                val bold = BattleSimulator.run(setup, AutoPolicy.SENSIBLE, 1.8f, seed = 7)
                val spam = BattleSimulator.run(setup, AutoPolicy.SPAM, at, seed = 7)
                val story = BattleSimulator.run(setup, AutoPolicy.FASTEST, 1.8f, seed = 7)

                val wins = sensible.filter { it.won }
                val rounds = (wins.ifEmpty { sensible }).map { it.rounds }.sorted()[(wins.ifEmpty { sensible }).size / 2]
                val margin = sensible.map { it.safetyMargin }.sorted()[1]
                val support = sensible.map { it.supportShare }.average().toFloat()
                val stuck = (sensible + lazy + bold + spam + story).any { !it.finished }
                sensible.forEach { r -> usedByRoute.getOrPut(route.label) { mutableSetOf() } += r.spellsUsed }
                b.partyStats.forEach { m ->
                    kitByRoute.getOrPut(route.label) { mutableSetOf() } +=
                        GameContent.knownSpells(m.id, b.player.heroClassId, b.flags).filter { it.basePower > 0 && !it.isHeal }.map { it.id }
                }

                val problems = mutableListOf<String>()
                if (stuck) problems += "a fight never finished (a loop)"
                if (wins.size < 2) problems += "the sensible player at x${fmt(at)} won ${wins.size}/3"
                if (rounds !in kind.rounds) problems += "${rounds} rounds (target ${kind.rounds.first}-${kind.rounds.last})"
                if (margin !in kind.margin) problems += "safety margin ${fmt(margin)} (target ${fmt(kind.margin.start)}-${fmt(kind.margin.endInclusive)})"
                if (support > 0.34f) problems += "${(support * 100).toInt()}% of turns spent healing or restoring (max 34%)"
                if (!story.won) problems += "Pure Story Mode lost"
                val late = kind == Kind.LATE || kind == Kind.BOSS_LATE || kind == Kind.FINALE
                if ((kind == Kind.BOSS_LATE || kind == Kind.FINALE) && lazy.won && lazy.safetyMargin >= 1.0f) problems += "a lazy x1.1 player wins it comfortably"
                if (kind == Kind.EARLY && !(bold.won && bold.safetyMargin >= 2.0f)) problems += "a bold x1.8 player doesn't win it easily"
                if (late && spam.won && spam.rounds < rounds * 1.2f) problems += "spamming the strongest spell is nearly as good (${spam.rounds} vs $rounds rounds)"

                rows += Row(route.label, b.encounterId, kind, rounds, margin, wins.size / 3f, support,
                    lazy.won, lazy.safetyMargin, bold.won, bold.safetyMargin, spam.rounds, spam.won, story.won, stuck, problems)
            }
        }

        val unused = kitByRoute.flatMap { (route, kit) -> (kit - usedByRoute[route].orEmpty()).map { "$route: $it" } }
        val failing = rows.filter { it.problems.isNotEmpty() }
        val text = buildString {
            appendLine("Balance contract: ${rows.size} fights on ${kitByRoute.size.coerceAtLeast(rows.map { it.route }.distinct().size)} routes, " +
                "balanced at x${fmt(at)} (difficulty ${balance.difficulty}); ${failing.size} fight(s) off target, ${unused.size} spell(s) never the best choice" +
                if (balance.enforce) " [enforced]" else " [report only]")
            appendLine()
            appendLine(String.format(Locale.US, "%-22s %-40s %-9s %6s %6s %5s %5s %-11s %-11s %-9s %s",
                "route", "fight", "kind", "rounds", "margin", "win", "supp", "lazy x1.1", "bold x1.8", "spam", "story"))
            rows.forEach { r ->
                appendLine(String.format(Locale.US, "%-22s %-40s %-9s %6d %6s %4d%% %4d%% %-11s %-11s %-9s %s",
                    r.route.take(22), r.encounterId.take(40), r.kind.label, r.rounds, fmt(r.margin), (r.winRate * 100).toInt(), (r.support * 100).toInt(),
                    (if (r.lazyWon) "won " else "lost ") + fmt(r.lazyMargin), (if (r.boldWon) "won " else "lost ") + fmt(r.boldMargin),
                    (if (r.spamWon) "won " else "lost ") + r.spamRounds, if (r.storyModeWon) "won" else "LOST"))
                r.problems.forEach { appendLine("    - $it") }
            }
            if (unused.isNotEmpty()) {
                appendLine()
                appendLine("Attack spells the sensible player never picked on their route:")
                unused.forEach { appendLine("  $it") }
            }
        }
        runCatching {
            val dir = listOf(File("build/reports"), File("app/build/reports")).firstOrNull { it.parentFile?.isDirectory == true } ?: File("build/reports")
            dir.mkdirs()
            File(dir, "balance.txt").writeText(text)
        }
        println(text)
        if (balance.enforce) {
            assertTrue("Balance contract (combat/balance.json enforce: 1):\n$text", failing.isEmpty() && unused.isEmpty())
        }
    }
}
