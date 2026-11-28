package com.voicerpg.engine.desktop

import android.content.Context
import android.content.res.Configuration
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.voicerpg.engine.audio.CombatNarrator
import com.voicerpg.engine.audio.MusicManager
import com.voicerpg.engine.audio.SfxManager
import com.voicerpg.engine.audio.SpeechManager
import com.voicerpg.engine.content.GameContent
import com.voicerpg.engine.engine.MotionSettings
import com.voicerpg.engine.engine.SaveManager
import com.voicerpg.engine.localization.ModelDownloadStatus
import com.voicerpg.engine.localization.TranslationManager
import com.voicerpg.engine.model.GameScreen
import com.voicerpg.engine.model.MiniGameConfig
import com.voicerpg.engine.ui.combat.DebugWarpDialog
import com.voicerpg.engine.ui.combat.OptionsDialog
import com.voicerpg.engine.ui.combat.PlaystylePreset
import com.voicerpg.engine.ui.combat.RetroBattleScreen
import com.voicerpg.engine.ui.creation.CharacterCreationScreen
import com.voicerpg.engine.ui.environment.EnvironmentAssetLoader
import com.voicerpg.engine.ui.gallery.AssetViewerScreen
import com.voicerpg.engine.ui.minigame.MiniGameContainerScreen
import com.voicerpg.engine.ui.pocket.PocketHeroVitals
import com.voicerpg.engine.ui.pocket.PocketModeTouchGuard
import com.voicerpg.engine.ui.pocket.PocketModeUnlockedBanner
import com.voicerpg.engine.ui.setup.AudioSetupScreen
import com.voicerpg.engine.ui.setup.DialectSelectionScreen
import com.voicerpg.engine.ui.setup.LanguageDownloadStandbyScreen
import com.voicerpg.engine.ui.setup.LanguageModelDownloadBanner
import com.voicerpg.engine.ui.setup.LanguageSelectionScreen
import com.voicerpg.engine.ui.sprites.SpriteBitmaps
import com.voicerpg.engine.ui.story.StoryAssetLoader
import com.voicerpg.engine.ui.story.StoryScreen
import com.voicerpg.engine.ui.theme.LogosGold
import com.voicerpg.engine.ui.theme.RetroBlack
import com.voicerpg.engine.ui.theme.RetroBorder
import com.voicerpg.engine.ui.theme.RetroPanel
import com.voicerpg.engine.ui.theme.VoiceRPGTheme
import com.voicerpg.engine.ui.title.TitleScreen
import com.voicerpg.engine.ui.tutorial.TutorialBattleScreen
import com.voicerpg.engine.viewmodel.CombatViewModel
import com.voicerpg.engine.viewmodel.StoryViewModel
import com.voicerpg.engine.viewmodel.TutorialBattleViewModel
import java.io.File

private const val TOP_BAR_HEIGHT_DP = 44

enum class DesktopLayout(
    val label: String,
    val shortLabel: String,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val orientation: Int
) {
    PORTRAIT("Portrait (9:16)", "Portrait", 460, 818, Configuration.ORIENTATION_PORTRAIT),
    LANDSCAPE("Landscape (16:9)", "Landscape", 1280, 720, Configuration.ORIENTATION_LANDSCAPE),
    FOLDABLE("Open Foldable (1:1)", "Foldable", 800, 800, Configuration.ORIENTATION_LANDSCAPE)
}

enum class DesktopScale(
    val label: String,
    val factor: Float
) {
    SCALE_50("50%", 0.50f),
    SCALE_65("65%", 0.65f),
    SCALE_75("75%", 0.75f),
    SCALE_85("85%", 0.85f),
    SCALE_100("100%", 1.00f),
    SCALE_125("125%", 1.25f),
    SCALE_150("150%", 1.50f)
}

data class GameEntry(
    val slug: String,
    val label: String,
    val repoDir: File
) {
    val assetsDir: File get() = File(repoDir, "app/src/main/assets")
    val saveDir: File get() = File(System.getProperty("user.home"), ".local/share/voicerpg-desktop/$slug")
}

private fun discoverGames(): List<GameEntry> {
    val cwd = File(System.getProperty("user.dir")).canonicalFile
    val engineDir = if (File(cwd, "app/src/main/assets").isDirectory) cwd else cwd.parentFile
    val parent = engineDir.parentFile ?: engineDir
    val candidates = listOf(
        "superstore-saga" to "Superstore Saga",
        "wallys-world" to "Wally's World",
        "commander-america-rpg" to "Commander America",
        "echoes-of-logos" to "Echoes of the Logos",
        "voice-rpg-engine" to "VoiceRPG Engine (Template)"
    )
    return candidates.mapNotNull { (slug, label) ->
        val dir = if (slug == engineDir.name) engineDir else File(parent, slug)
        val assets = File(dir, "app/src/main/assets")
        if (assets.isDirectory) GameEntry(slug, label, dir) else null
    }
}

private class DesktopGameSession(val game: GameEntry) {
    val context = Context(assetsDir = game.assetsDir, filesDir = game.saveDir).apply {
        filesDir.mkdirs()
    }
    val speechManager: SpeechManager
    val combatNarrator: CombatNarrator
    val musicManager: MusicManager
    val saveManager: SaveManager
    val motionSettings: MotionSettings
    val combatViewModel: CombatViewModel
    val storyViewModel: StoryViewModel
    val tutorialBattleViewModel: TutorialBattleViewModel

    init {
        EnvironmentAssetLoader.clear()
        StoryAssetLoader.clear()
        SpriteBitmaps.clear()
        GameContent.reset()
        GameContent.initialize(context)
        TranslationManager.initialize(context)

        speechManager = SpeechManager(context)
        combatNarrator = CombatNarrator(context)
        combatNarrator.registerSpeakers(GameContent.allSpeakers)
        musicManager = MusicManager(context)
        saveManager = SaveManager(context)
        motionSettings = MotionSettings()

        combatViewModel = CombatViewModel(
            speechManager = speechManager,
            combatNarrator = combatNarrator,
            musicManager = musicManager,
            sfxManager = SfxManager(context)
        )
        storyViewModel = StoryViewModel(
            speechManager = speechManager,
            combatNarrator = combatNarrator,
            saveManager = saveManager,
            musicManager = musicManager,
            motionSettings = motionSettings
        )
        tutorialBattleViewModel = TutorialBattleViewModel(
            speechManager = speechManager,
            combatNarrator = combatNarrator,
            musicManager = musicManager
        )

        combatViewModel.applyPlayerCustomization(
            customization = storyViewModel.state.value.player,
            savedStats = storyViewModel.state.value.partyStats
        )
        storyViewModel.onOpenOptions = {
            storyViewModel.cancelPendingAutoAdvance()
            combatViewModel.openOptions()
        }
        storyViewModel.onCloseOptions = { combatViewModel.closeOptions() }
        combatViewModel.onContinueStory = {
            storyViewModel.updatePartyStatsFromCombat(combatViewModel.state.value.party)
            storyViewModel.onCombatVictory()
        }
    }

    fun dispose() {
        runCatching { storyViewModel.persistCurrentState() }
        runCatching { storyViewModel.cancelPendingAutoAdvance() }
        runCatching { storyViewModel.cancelPendingStoryMode() }
        runCatching { combatViewModel.cleanup() }
        runCatching { speechManager.destroy() }
        runCatching { combatNarrator.destroy() }
        runCatching { musicManager.destroy() }
        MediaPlayer.stopAll()
        TextToSpeech.stopAll()
    }
}

fun main(args: Array<String>) = application {
    val games = remember { discoverGames() }
    val initialSlug = remember {
        val idx = args.indexOf("--game")
        if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
    }
    val initialScale = remember {
        val idx = args.indexOf("--scale")
        val raw = if (idx >= 0 && idx + 1 < args.size) args[idx + 1].trim().removeSuffix("%") else null
        val parsed = raw?.toFloatOrNull()?.let { if (it > 3f) it / 100f else it }
        if (parsed != null) {
            DesktopScale.entries.minByOrNull { kotlin.math.abs(it.factor - parsed) } ?: DesktopScale.SCALE_100
        } else {
            DesktopScale.SCALE_100
        }
    }
    val autoStart = remember { "--autostart" in args }

    var selectedGame by remember {
        mutableStateOf(
            games.firstOrNull { it.slug.equals(initialSlug, ignoreCase = true) }
                ?: games.firstOrNull()
        )
    }
    var selectedLayout by remember { mutableStateOf(DesktopLayout.PORTRAIT) }
    var selectedScale by remember { mutableStateOf(initialScale) }
    var activeSession by remember { mutableStateOf<DesktopGameSession?>(null) }

    fun computeWindowSize(layout: DesktopLayout, scale: DesktopScale): DpSize =
        DpSize(
            (layout.viewportWidth * scale.factor).dp,
            (layout.viewportHeight * scale.factor + TOP_BAR_HEIGHT_DP).dp
        )

    val windowState = rememberWindowState(
        size = computeWindowSize(selectedLayout, selectedScale)
    )

    LaunchedEffect(selectedLayout, selectedScale) {
        windowState.size = computeWindowSize(selectedLayout, selectedScale)
    }

    LaunchedEffect(autoStart) {
        if (autoStart && activeSession == null) {
            selectedGame?.let { activeSession = DesktopGameSession(it) }
        }
    }

    fun stopSession() {
        activeSession?.dispose()
        activeSession = null
    }

    fun startSession(game: GameEntry) {
        stopSession()
        activeSession = DesktopGameSession(game)
    }

    Window(
        onCloseRequest = {
            stopSession()
            exitApplication()
        },
        state = windowState,
        title = selectedGame?.let { "VoiceRPG Desktop — ${it.label}" } ?: "VoiceRPG Desktop"
    ) {
        DisposableEffect(Unit) {
            onDispose { stopSession() }
        }

        Column(modifier = Modifier.fillMaxSize().background(RetroBlack)) {
            DesktopTopBar(
                games = games,
                selectedGame = selectedGame,
                selectedLayout = selectedLayout,
                selectedScale = selectedScale,
                isRunning = activeSession != null,
                onSelectGame = { game ->
                    selectedGame = game
                    if (activeSession != null) {
                        startSession(game)
                    }
                },
                onSelectLayout = { layout ->
                    selectedLayout = layout
                },
                onSelectScale = { scale ->
                    selectedScale = scale
                },
                onToggleStartStop = {
                    if (activeSession != null) {
                        stopSession()
                    } else {
                        selectedGame?.let { startSession(it) }
                    }
                }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val baseDensity = LocalDensity.current
                val viewportDensity = remember(baseDensity, selectedScale) {
                    Density(
                        density = baseDensity.density * selectedScale.factor,
                        fontScale = baseDensity.fontScale
                    )
                }
                val session = activeSession
                if (session == null) {
                    CompositionLocalProvider(LocalDensity provides viewportDensity) {
                        StandbyViewport(
                            selectedGame = selectedGame,
                            selectedLayout = selectedLayout,
                            onStart = { selectedGame?.let { startSession(it) } }
                        )
                    }
                } else {
                    val config = remember(selectedLayout) {
                        Configuration(
                            screenWidthDp = selectedLayout.viewportWidth,
                            screenHeightDp = selectedLayout.viewportHeight,
                            orientation = selectedLayout.orientation
                        )
                    }
                    CompositionLocalProvider(
                        LocalDensity provides viewportDensity,
                        LocalContext provides session.context,
                        LocalConfiguration provides config
                    ) {
                        DesktopGameViewport(session = session)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopTopBar(
    games: List<GameEntry>,
    selectedGame: GameEntry?,
    selectedLayout: DesktopLayout,
    selectedScale: DesktopScale,
    isRunning: Boolean,
    onSelectGame: (GameEntry) -> Unit,
    onSelectLayout: (DesktopLayout) -> Unit,
    onSelectScale: (DesktopScale) -> Unit,
    onToggleStartStop: () -> Unit
) {
    var gameMenuOpen by remember { mutableStateOf(false) }
    var layoutMenuOpen by remember { mutableStateOf(false) }
    var scaleMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOP_BAR_HEIGHT_DP.dp)
            .background(Color(0xFF0E1017))
            .border(1.dp, RetroBorder)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(RetroPanel)
                        .border(1.dp, RetroBorder, RoundedCornerShape(6.dp))
                        .clickable { gameMenuOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Game: ${selectedGame?.label ?: "None"} ▾",
                        color = LogosGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                DropdownMenu(
                    expanded = gameMenuOpen,
                    onDismissRequest = { gameMenuOpen = false }
                ) {
                    games.forEach { game ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = game.label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                gameMenuOpen = false
                                onSelectGame(game)
                            }
                        )
                    }
                }
            }

            // Layout Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(RetroPanel)
                        .border(1.dp, RetroBorder, RoundedCornerShape(6.dp))
                        .clickable { layoutMenuOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Layout: ${selectedLayout.shortLabel} ▾",
                        color = Color(0xFF81D4FA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                DropdownMenu(
                    expanded = layoutMenuOpen,
                    onDismissRequest = { layoutMenuOpen = false }
                ) {
                    DesktopLayout.entries.forEach { layout ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = layout.label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                layoutMenuOpen = false
                                onSelectLayout(layout)
                            }
                        )
                    }
                }
            }

            // Scale Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(RetroPanel)
                        .border(1.dp, RetroBorder, RoundedCornerShape(6.dp))
                        .clickable { scaleMenuOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scale: ${selectedScale.label} ▾",
                        color = Color(0xFFA5D6A7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                DropdownMenu(
                    expanded = scaleMenuOpen,
                    onDismissRequest = { scaleMenuOpen = false }
                ) {
                    DesktopScale.entries.forEach { scale ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = scale.label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                scaleMenuOpen = false
                                onSelectScale(scale)
                            }
                        )
                    }
                }
            }
        }

        // Start / Stop Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isRunning) Color(0xFFC62828) else Color(0xFF2E7D32))
                .border(
                    1.dp,
                    if (isRunning) Color(0xFFFF8A80) else Color(0xFF69F0AE),
                    RoundedCornerShape(6.dp)
                )
                .clickable(enabled = selectedGame != null, onClick = onToggleStartStop)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRunning) "⏹ Stop" else "▶ Start",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun StandbyViewport(
    selectedGame: GameEntry?,
    selectedLayout: DesktopLayout,
    onStart: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0C12)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RetroPanel)
                .border(1.dp, RetroBorder, RoundedCornerShape(12.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "VOICERPG DESKTOP RUNNER",
                color = LogosGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Game: ${selectedGame?.label ?: "No game found"}",
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Layout: ${selectedLayout.label} (${selectedLayout.viewportWidth}x${selectedLayout.viewportHeight})",
                color = Color(0xFF90A4AE),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onStart,
                enabled = selectedGame != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "▶ Start Game",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DesktopGameViewport(session: DesktopGameSession) {
    val storyViewModel = session.storyViewModel
    val combatViewModel = session.combatViewModel
    val tutorialBattleViewModel = session.tutorialBattleViewModel
    val speechManager = session.speechManager
    val combatNarrator = session.combatNarrator
    val musicManager = session.musicManager
    val motionSettings = session.motionSettings

    val storyState by storyViewModel.state.collectAsState()
    val combatState by combatViewModel.state.collectAsState()
    val isEyesFreeMode by combatNarrator.isEyesFreeMode.collectAsState()
    val isPocketGuardLocked by combatNarrator.isPocketGuardLocked.collectAsState()
    val isPocketGuardEnabled by combatNarrator.isPocketGuardEnabled.collectAsState()
    val isSpellCalloutsEnabled by combatNarrator.isSpellCalloutsEnabled.collectAsState()
    val isBattleReportsEnabled by combatNarrator.isBattleReportsEnabled.collectAsState()
    val speechState by speechManager.speechState.collectAsState()
    val rmsLevel by speechManager.rmsLevel.collectAsState()
    val isTtsSpeaking by combatNarrator.isSpeaking.collectAsState()
    val isNarrationEnabled by combatNarrator.isNarrationEnabled.collectAsState()
    val isReadChoicesEnabled by combatNarrator.isReadChoicesEnabled.collectAsState()
    val isAutoReadHubChoices by combatNarrator.isAutoReadHubChoices.collectAsState()
    val isCharacterPitchEnabled by combatNarrator.isCharacterPitchEnabled.collectAsState()
    val isSpeakerAttributionEnabled by combatNarrator.isSpeakerAttributionEnabled.collectAsState()
    val speechRate by combatNarrator.speechRate.collectAsState()
    val isAutoListen by speechManager.isAutoListen.collectAsState()
    val isChimeMuted by speechManager.isChimeMuted.collectAsState()
    val isMusicEnabled by musicManager.isMusicEnabled.collectAsState()
    val musicVolume by musicManager.musicVolume.collectAsState()
    val isReduceMotion by motionSettings.reduceMotion.collectAsState()
    val isReduceFlashing by motionSettings.reduceFlashing.collectAsState()
    var showDebugWarp by remember { mutableStateOf(false) }
    val isDeveloperToolsEnabled by combatViewModel.isDeveloperToolsEnabled.collectAsState()

    LaunchedEffect(combatNarrator) {
        combatNarrator.isSpeaking.collect { speaking ->
            if (speaking) musicManager.duckForSpeech() else musicManager.restoreFromSpeech()
        }
    }

    LaunchedEffect(storyState.player) {
        combatViewModel.applyStoryFlags(storyState.narrativeFlags)
        combatViewModel.applyPlayerCustomization(
            customization = storyState.player,
            savedStats = storyState.partyStats
        )
    }

    LaunchedEffect(storyState.activeEncounter) {
        storyState.activeEncounter?.let { encounter ->
            combatViewModel.applyStoryFlags(storyState.narrativeFlags)
            combatViewModel.applyBestiary(storyState.bestiaryWeaknesses)
            combatViewModel.onBestiaryUpdated = { storyViewModel.recordBestiaryWeaknesses(it) }
            combatViewModel.applySavedStats(storyState.partyStats)
            combatViewModel.startEncounter(encounter)
            combatViewModel.setPureStoryMode(storyState.isPureStoryMode)
        }
    }

    LaunchedEffect(storyState.isPureStoryMode) {
        combatViewModel.setPureStoryMode(storyState.isPureStoryMode)
    }

    LaunchedEffect(storyState.gameScreen) {
        if (storyState.gameScreen != GameScreen.COMBAT_ARENA) {
            combatViewModel.pauseAtb()
        }
        if (storyState.gameScreen == GameScreen.TUTORIAL) {
            tutorialBattleViewModel.startTutorial()
        }
    }

    LaunchedEffect(storyState.currentScene.id, storyState.gameScreen) {
        when (storyState.gameScreen) {
            GameScreen.COMBAT_ARENA -> musicManager.playCombatMusic()
            GameScreen.STORY_EXPLORATION -> musicManager.playTrack(storyState.currentScene.musicAsset)
            GameScreen.TITLE, GameScreen.TUTORIAL -> musicManager.playTrack(MusicManager.TRACK_TITLE)
            else -> musicManager.playTrack(MusicManager.TRACK_EXPLORATION)
        }
    }

    VoiceRPGTheme {
        val downloadStatus by TranslationManager.downloadStatus.collectAsState()
        val downloadProgress by TranslationManager.downloadProgress.collectAsState()
        val downloadTargetName by TranslationManager.downloadTargetName.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = downloadStatus != ModelDownloadStatus.IDLE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LanguageModelDownloadBanner(
                    status = downloadStatus,
                    progress = downloadProgress,
                    languageName = downloadTargetName,
                    onDismiss = { TranslationManager.dismissDownloadBanner() }
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (storyState.gameScreen) {
                    GameScreen.TITLE -> {
                        TitleScreen(
                            manifest = GameContent.manifest,
                            hasSave = storyState.hasExistingSave,
                            saveSummary = storyState.saveSummary,
                            saveSlots = storyState.saveSlots,
                            currentSlot = storyState.currentSlot,
                            speechState = speechState,
                            isAutoListen = isAutoListen,
                            onContinue = { storyViewModel.continueGame() },
                            onNewGame = { storyViewModel.startNewGameFlow() },
                            startupNotice = storyState.startupNotice,
                            onDismissStartupNotice = { storyViewModel.dismissStartupNotice() },
                            onSelectSlot = { storyViewModel.selectSlot(it) },
                            onDeleteSlot = { storyViewModel.deleteSlot(it) },
                            onContinueSlot = { storyViewModel.continueGame(it) },
                            onContinueSlotAuto = { storyViewModel.continueGame(it, startInAutoPlay = true) },
                            onNewGameInSlot = { storyViewModel.startNewGameFlow(it) },
                            isMiniGameTestMenuOpen = storyState.isMiniGameTestMenuOpen,
                            onOpenMiniGameTestMenu = { storyViewModel.openMiniGameTestMenu() },
                            onCloseMiniGameTestMenu = { storyViewModel.closeMiniGameTestMenu() },
                            onLaunchMiniGame = { storyViewModel.triggerMiniGame(it) },
                            onAudioSetup = { storyViewModel.openAudioSetup() },
                            onOptions = { combatViewModel.openOptions() },
                            storyModeSummary = storyViewModel.getStoryModeSaveSummary(),
                            onPureStoryMode = { fresh, trackId -> storyViewModel.startPureStoryMode(fresh, trackId) },
                            onTutorial = {
                                tutorialBattleViewModel.resetTutorial()
                                storyViewModel.openTutorial()
                            },
                            onAssetViewer = { storyViewModel.openAssetViewer() },
                            onStartListening = {
                                speechManager.startListening(onResult = { storyViewModel.handleStoryVoiceInput(it) })
                            },
                            onStopListening = { speechManager.stopListening() }
                        )
                    }
                    GameScreen.TUTORIAL -> {
                        TutorialBattleScreen(
                            viewModel = tutorialBattleViewModel,
                            onBack = { storyViewModel.returnFromTutorial() },
                            onStartNewGame = { storyViewModel.startNewGameFlow() }
                        )
                    }
                    GameScreen.AUDIO_SETUP -> {
                        AudioSetupScreen(
                            combatNarrator = combatNarrator,
                            speechManager = speechManager,
                            isFromGame = storyState.previousScreen != null && !storyState.isNewGameFlow && storyState.previousScreen != GameScreen.TITLE,
                            isNewGameFlow = storyState.isNewGameFlow,
                            onBack = { storyViewModel.returnFromAudioSetup() },
                            onVoiceChanged = { storyViewModel.persistCurrentState() },
                            onProceed = {
                                if (storyState.isNewGameFlow) storyViewModel.proceedToCharacterCreation()
                                else storyViewModel.returnFromAudioSetup()
                            },
                            onOpenLanguageSelection = { storyViewModel.openLanguageSelection(fromOptions = false) }
                        )
                    }
                    GameScreen.LANGUAGE_SELECTION -> {
                        LanguageSelectionScreen(
                            currentLanguageCode = storyState.pendingSelectedLanguage,
                            canCancel = storyState.previousScreen != null,
                            onLanguageSelected = { storyViewModel.onLanguageChosen(it) },
                            onCancel = { storyViewModel.cancelLanguageSelection() }
                        )
                    }
                    GameScreen.DIALECT_SELECTION -> {
                        DialectSelectionScreen(
                            languageCode = storyState.pendingSelectedLanguage,
                            initialRegionCode = storyState.pendingSelectedRegion,
                            combatNarrator = combatNarrator,
                            onBack = { storyViewModel.returnToLanguageSelection() },
                            onConfirm = { langCode, regionCode -> storyViewModel.onDialectConfirmed(langCode, regionCode) }
                        )
                    }
                    GameScreen.LANGUAGE_DOWNLOAD_STANDBY -> {
                        LanguageDownloadStandbyScreen(
                            languageCode = storyState.pendingSelectedLanguage,
                            regionCode = storyState.pendingSelectedRegion,
                            onDownloadComplete = { langCode, regionCode -> storyViewModel.applyLanguageAndFinish(langCode, regionCode) },
                            onChangeLanguage = { storyViewModel.returnToLanguageSelection() }
                        )
                    }
                    GameScreen.CHARACTER_CREATION -> {
                        CharacterCreationScreen(
                            initialCustomization = storyState.player,
                            slot = storyState.currentSlot,
                            speechManager = speechManager,
                            combatNarrator = combatNarrator,
                            onBack = {
                                if (storyState.isNewGameFlow) storyViewModel.startNewGameFlow(storyState.currentSlot)
                                else storyViewModel.returnToTitle()
                            },
                            onConfirmCharacter = { customization ->
                                storyViewModel.startNewGame(customization, storyState.currentSlot)
                                combatViewModel.applyPlayerCustomization(customization)
                            }
                        )
                    }
                    GameScreen.STORY_EXPLORATION -> {
                        StoryScreen(
                            storyViewModel = storyViewModel,
                            combatViewModel = combatViewModel,
                            onOpenOptions = { combatViewModel.openOptions() }
                        )
                    }
                    GameScreen.COMBAT_ARENA -> {
                        RetroBattleScreen(
                            viewModel = combatViewModel,
                            motionSettings = motionSettings,
                            onReturnToStory = { storyViewModel.switchToStory() },
                            onContinueStory = {
                                combatNarrator.stop()
                                combatViewModel.onContinueStory?.invoke()
                            }
                        )
                    }
                    GameScreen.MINI_GAME -> {
                        val activeConfig = storyState.activeMiniGameConfig ?: MiniGameConfig(id = "minigame", title = "Mini-Game")
                        MiniGameContainerScreen(
                            config = activeConfig,
                            onFinish = { storyViewModel.onMiniGameFinished(it) },
                            onSkip = { storyViewModel.onSkipMiniGame() }
                        )
                    }
                    GameScreen.ASSET_VIEWER -> {
                        AssetViewerScreen(
                            musicManager = musicManager,
                            speechManager = speechManager,
                            onBack = { storyViewModel.returnFromAssetViewer() }
                        )
                    }
                }

                OptionsDialog(
                    isOpen = combatState.isOptionsOpen,
                    isEyesFreeMode = isEyesFreeMode,
                    isNarrationEnabled = isNarrationEnabled,
                    isAutoPlayEnabled = storyState.isPureStoryMode,
                    isSpellCalloutsEnabled = isSpellCalloutsEnabled,
                    isBattleReportsEnabled = isBattleReportsEnabled,
                    isPocketGuardEnabled = isPocketGuardEnabled,
                    isReadChoicesEnabled = isReadChoicesEnabled,
                    isAutoReadHubChoices = isAutoReadHubChoices,
                    isCharacterPitchEnabled = isCharacterPitchEnabled,
                    isSpeakerAttributionEnabled = isSpeakerAttributionEnabled,
                    speechRate = speechRate,
                    isAutoListen = isAutoListen,
                    isChimeMuted = isChimeMuted,
                    onToggleEyesFreeMode = {
                        val enabled = combatNarrator.toggleEyesFreeMode()
                        if (enabled) speechManager.setAutoListen(true)
                        storyViewModel.persistCurrentState()
                    },
                    onToggleNarration = {
                        combatNarrator.toggleNarration()
                        storyViewModel.persistCurrentState()
                    },
                    onToggleAutoPlay = { storyViewModel.toggleAutoPlay() },
                    onToggleSpellCallouts = {
                        combatNarrator.toggleSpellCalloutsEnabled()
                        storyViewModel.persistCurrentState()
                    },
                    onToggleBattleReports = {
                        combatNarrator.toggleBattleReportsEnabled()
                        storyViewModel.persistCurrentState()
                    },
                    onTogglePocketGuard = {
                        combatNarrator.togglePocketGuardEnabled()
                        storyViewModel.persistCurrentState()
                    },
                    onApplyPreset = { preset ->
                        when (preset) {
                            PlaystylePreset.POCKET_WALK -> {
                                combatNarrator.setPocketGuardEnabled(true)
                                combatNarrator.setSpellCalloutsEnabled(true)
                                combatNarrator.setBattleReportsEnabled(true)
                                combatNarrator.setNarrationEnabled(true)
                                combatNarrator.setReadChoicesEnabled(true)
                                combatNarrator.setAutoReadHubChoices(true)
                                combatNarrator.setEyesFreeMode(true, lockGuard = true)
                                speechManager.setAutoListen(true)
                                speechManager.setChimeMuted(true)
                            }
                            PlaystylePreset.STORYBOOK -> {
                                combatNarrator.setPocketGuardEnabled(false)
                                combatNarrator.setSpellCalloutsEnabled(true)
                                combatNarrator.setBattleReportsEnabled(true)
                                combatNarrator.setNarrationEnabled(true)
                                combatNarrator.setReadChoicesEnabled(true)
                                combatNarrator.setEyesFreeMode(true, lockGuard = false)
                                speechManager.setAutoListen(true)
                                speechManager.setChimeMuted(false)
                            }
                            PlaystylePreset.CLASSIC_TACTICAL -> {
                                combatNarrator.setPocketGuardEnabled(false)
                                combatNarrator.setSpellCalloutsEnabled(true)
                                combatNarrator.setBattleReportsEnabled(false)
                                combatNarrator.setNarrationEnabled(true)
                                combatNarrator.setReadChoicesEnabled(false)
                                combatNarrator.setEyesFreeMode(false, lockGuard = false)
                                speechManager.setAutoListen(false)
                                speechManager.setChimeMuted(false)
                            }
                            PlaystylePreset.CUSTOM -> {}
                        }
                        storyViewModel.persistCurrentState()
                    },
                    onToggleReadChoices = {
                        combatNarrator.toggleReadChoices()
                        storyViewModel.persistCurrentState()
                    },
                    onToggleAutoReadHubChoices = {
                        combatNarrator.toggleAutoReadHubChoices()
                        storyViewModel.persistCurrentState()
                    },
                    onToggleCharacterPitch = {
                        combatNarrator.setCharacterPitchEnabled(!isCharacterPitchEnabled)
                        storyViewModel.persistCurrentState()
                    },
                    onToggleSpeakerAttribution = {
                        combatNarrator.toggleSpeakerAttribution()
                        storyViewModel.persistCurrentState()
                    },
                    onSpeechRateChange = { rate ->
                        combatNarrator.setSpeechRate(rate)
                        storyViewModel.persistCurrentState()
                    },
                    onToggleAutoListen = {
                        speechManager.toggleAutoListen()
                        storyViewModel.persistCurrentState()
                    },
                    onToggleChimeMute = {
                        speechManager.toggleChimeMute()
                        storyViewModel.persistCurrentState()
                    },
                    isMusicEnabled = isMusicEnabled,
                    musicVolume = musicVolume,
                    onToggleMusic = {
                        musicManager.toggleMusic()
                        storyViewModel.persistCurrentState()
                    },
                    onMusicVolumeChange = { vol ->
                        musicManager.setVolume(vol)
                        storyViewModel.persistCurrentState()
                    },
                    isReduceMotion = isReduceMotion,
                    onToggleReduceMotion = {
                        motionSettings.toggleReduceMotion()
                        storyViewModel.persistCurrentState()
                    },
                    isReduceFlashing = isReduceFlashing,
                    onToggleReduceFlashing = {
                        motionSettings.toggleReduceFlashing()
                        storyViewModel.persistCurrentState()
                    },
                    onOpenVoiceSettings = { combatNarrator.openVoiceSettings(session.context) },
                    onOpenVoiceAssignment = {
                        combatViewModel.closeOptions()
                        storyViewModel.openAudioSetup()
                    },
                    onOpenLanguageSelection = {
                        combatViewModel.closeOptions()
                        storyViewModel.openLanguageSelection(fromOptions = true)
                    },
                    onTriggerCheaterDialogue = {
                        combatNarrator.speak(
                            "Cheater detected! You have invoked the ancient Konami incantation to bend time and space. Do you boldly embrace your dishonor?",
                            force = true
                        )
                    },
                    onOpenDebugWarp = { showDebugWarp = true },
                    onReturnToTitle = {
                        combatViewModel.closeOptions()
                        storyViewModel.returnToTitle()
                    },
                    onClose = { combatViewModel.closeOptions() }
                )

                DebugWarpDialog(
                    isOpen = showDebugWarp,
                    targets = storyViewModel.warpTargets(),
                    developerToolsEnabled = isDeveloperToolsEnabled,
                    onToggleDeveloperTools = { combatViewModel.toggleDeveloperTools() },
                    onWarpTo = { nodeId ->
                        showDebugWarp = false
                        combatViewModel.closeOptions()
                        storyViewModel.debugWarpToChapter(nodeId)
                    },
                    onClose = { showDebugWarp = false }
                )

                val heroVitals = remember(storyState.gameScreen, combatState.party, storyState.partyStats, storyState.player) {
                    if (storyState.gameScreen == GameScreen.COMBAT_ARENA && combatState.party.isNotEmpty()) {
                        combatState.party.map { PocketHeroVitals(it.name, it.loreClass, it.currentHp, it.maxHp, it.currentMp, it.maxMp) }
                    } else if (storyState.partyStats.isNotEmpty()) {
                        storyState.partyStats.map { PocketHeroVitals(it.name, it.loreClass, it.currentHp, it.maxHp, it.currentMp, it.maxMp) }
                    } else {
                        listOf(
                            PocketHeroVitals(
                                storyState.player.name,
                                storyState.player.heroClass.title,
                                storyState.player.heroClass.startingHp,
                                storyState.player.heroClass.startingHp,
                                storyState.player.heroClass.startingMp,
                                storyState.player.heroClass.startingMp
                            )
                        )
                    }
                }

                val locationTitle = remember(storyState.gameScreen, storyState.currentScene, storyState.activeEncounter, combatState.enemies) {
                    when (storyState.gameScreen) {
                        GameScreen.COMBAT_ARENA -> storyState.activeEncounter?.name?.let { "Battle: $it" } ?: "Combat Arena"
                        GameScreen.STORY_EXPLORATION -> "${storyState.currentScene.chapterTitle} • ${storyState.currentScene.name}"
                        GameScreen.TITLE -> "Title Screen"
                        GameScreen.TUTORIAL -> "Chronicle Guide"
                        else -> GameContent.manifest.gameTitle
                    }
                }

                if (isEyesFreeMode && !isPocketGuardLocked && isPocketGuardEnabled) {
                    PocketModeUnlockedBanner(
                        onLock = { combatNarrator.lockPocketGuard() },
                        onExitPocketMode = {
                            combatNarrator.setEyesFreeMode(false)
                            storyViewModel.persistCurrentState()
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                if (isEyesFreeMode && isPocketGuardLocked && isPocketGuardEnabled) {
                    PocketModeTouchGuard(
                        party = heroVitals,
                        locationTitle = locationTitle,
                        speechState = speechState,
                        rmsLevel = rmsLevel,
                        isTtsSpeaking = isTtsSpeaking,
                        onUnlock = { combatNarrator.unlockPocketGuard() },
                        onExitPocketMode = {
                            combatNarrator.setEyesFreeMode(false)
                            storyViewModel.persistCurrentState()
                        }
                    )
                }
            }
        }
    }
}
