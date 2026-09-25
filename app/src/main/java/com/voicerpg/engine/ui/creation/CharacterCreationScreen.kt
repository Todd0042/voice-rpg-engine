package com.voicerpg.engine.ui.creation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.voicerpg.engine.audio.CombatNarrator
import com.voicerpg.engine.audio.SpeechManager
import com.voicerpg.engine.audio.SpeechState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicerpg.engine.content.GameContent
import com.voicerpg.engine.content.HeroClassDef
import com.voicerpg.engine.model.AuraColor
import com.voicerpg.engine.model.PlayerCustomization
import com.voicerpg.engine.model.SpellSchool
import com.voicerpg.engine.ui.story.StoryAssetLoader
import com.voicerpg.engine.ui.theme.LogosGold
import com.voicerpg.engine.ui.theme.LogosGlow
import com.voicerpg.engine.ui.theme.RetroBlack
import com.voicerpg.engine.ui.theme.RetroBorder
import com.voicerpg.engine.ui.theme.RetroBorderGold
import com.voicerpg.engine.ui.theme.RetroPanel

@Composable
fun CharacterCreationScreen(
    initialCustomization: PlayerCustomization = PlayerCustomization(),
    speechManager: SpeechManager? = null,
    combatNarrator: CombatNarrator? = null,
    onBack: (() -> Unit)? = null,
    onConfirmCharacter: (PlayerCustomization) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialCustomization.name) }
    var selectedClass by remember { mutableStateOf(initialCustomization.heroClass) }
    var selectedTitle by remember { mutableStateOf(initialCustomization.title) }
    var selectedAura by remember { mutableStateOf(initialCustomization.auraColor) }
    var selectedAffinity by remember { mutableStateOf(initialCustomization.voiceAffinityBonusSchool) }

    val isAutoListen = speechManager?.isAutoListen?.collectAsState()?.value ?: false
    val speechState = speechManager?.speechState?.collectAsState()?.value ?: SpeechState.Idle
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val manifest = GameContent.manifest
    val heroSpeaker = GameContent.heroCharacter?.speaker
    val defaultHeroName = manifest.defaultHeroName.ifBlank { heroSpeaker?.name ?: "Hero" }
    val defaultHeroTitle = manifest.defaultHeroTitle.ifBlank { heroSpeaker?.title ?: "Adventurer" }
    val defaultHeroPortrait = manifest.defaultHeroPortrait ?: heroSpeaker?.portraitAsset ?: "portraits/todd.jpg"

    val heroBitmap = remember {
        StoryAssetLoader.loadBitmap(context, defaultHeroPortrait)
    }

    val titles = remember {
        manifest.characterTitles.ifEmpty {
            listOf(defaultHeroTitle)
        }
    }

    val quickNames = remember {
        manifest.quickNames.ifEmpty {
            listOf(defaultHeroName)
        }
    }

    fun confirmAndAwaken() {
        speechManager?.cancel()
        val finalCustomization = PlayerCustomization(
            name = if (name.isBlank()) defaultHeroName else name.trim(),
            heroClassId = selectedClass.id,
            title = selectedTitle,
            auraColor = selectedAura,
            voiceAffinityBonusSchool = selectedClass.preferredSchool
        )
        onConfirmCharacter(finalCustomization)
    }

    fun processCreationVoiceInput(utterance: String) {
        val lower = utterance.lowercase().trim()
        if (lower.isBlank()) return

        // 1. Awaken / Embark command
        val embarkKeywords = (manifest.creationVoiceKeywords.ifEmpty {
            listOf("awaken", "embark", "begin", "start", "enter", "ready")
        } + listOf("confirm", "forward", "onward", "play", "lets go", "let's go")).distinct()
        if (embarkKeywords.any { lower.contains(it) }) {
            confirmAndAwaken()
            return
        }

        // 1b. Back command
        if (lower == "back" || lower.contains("go back") || lower.contains("audio setup") || lower == "return") {
            speechManager?.cancel()
            onBack?.invoke()
            return
        }

        // 2. Class selection commands - data-driven via the loaded content classes
        GameContent.classes.firstOrNull { hc ->
            lower.contains(hc.title.lowercase()) ||
                lower.contains(hc.id) ||
                hc.voiceKeywords.any { kw -> kw.isNotBlank() && lower.contains(kw) }
        }?.let { matched ->
            selectedClass = matched
            selectedAffinity = matched.preferredSchool
            combatNarrator?.speak("${matched.title} selected. ${matched.subtitle}.", force = true)
        }

        // 3. Title selection
        titles.firstOrNull { lower.contains(it.lowercase()) }?.let {
            selectedTitle = it
        }

        // 4. Quick name selection
        quickNames.firstOrNull { lower.contains(it.lowercase()) }?.let {
            name = it
        }

        // Re-listen if auto-listen is enabled
        if (speechManager != null && speechManager.isAutoListen.value) {
            coroutineScope.launch {
                delay(200)
                speechManager.startListening { next -> processCreationVoiceInput(next) }
            }
        }
    }

    LaunchedEffect(isAutoListen) {
        if (isAutoListen) {
            delay(300)
            speechManager?.startListening { utterance -> processCreationVoiceInput(utterance) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RetroBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optional Back navigation
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RetroPanel)
                            .border(1.dp, LogosGold, RoundedCornerShape(6.dp))
                            .clickable {
                                speechManager?.cancel()
                                onBack()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "◀ BACK TO AUDIO SETUP",
                            color = LogosGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Header
            Text(
                text = "⚔️ TEMPLATE CHARACTER CREATION ⚔️",
                color = LogosGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Template Character Setup & Initial Loadout",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )


            Spacer(modifier = Modifier.height(14.dp))

            // Character Preview Card with chosen Aura & Name
            val auraColorVal = Color(android.graphics.Color.parseColor(selectedAura.hexColor))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RetroPanel.copy(alpha = 0.9f))
                    .border(2.dp, auraColorVal, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Portrait Bust
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(8.dp, RoundedCornerShape(10.dp), spotColor = auraColorVal)
                            .clip(RoundedCornerShape(10.dp))
                            .border(2.dp, auraColorVal, RoundedCornerShape(10.dp))
                    ) {
                        if (heroBitmap != null) {
                            Image(
                                bitmap = heroBitmap,
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(auraColorVal.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.firstOrNull()?.toString() ?: "A",
                                    color = auraColorVal,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (name.isBlank()) "Nameless" else name,
                            color = LogosGlow,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = selectedTitle,
                            color = auraColorVal,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Class: ${selectedClass.title} • Affinity: ${selectedAffinity.name}",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Name Input
            Text(
                text = "1. INVOCATOR'S TRUE NAME",
                color = LogosGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Character Name", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LogosGold,
                    unfocusedBorderColor = RetroBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = RetroPanel,
                    unfocusedContainerColor = RetroPanel
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Name Suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickNames.forEach { qn ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (name == qn) LogosGold.copy(alpha = 0.2f) else RetroPanel)
                            .border(1.dp, if (name == qn) LogosGold else RetroBorder, RoundedCornerShape(6.dp))
                            .clickable { name = qn }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = qn,
                            color = if (name == qn) LogosGold else Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Class Selection
            Text(
                text = "2. CHOOSE YOUR DISCIPLINE (CLASS)",
                color = LogosGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GameContent.classes.forEach { hc ->
                    ClassOptionCard(
                        heroClass = hc,
                        isSelected = hc == selectedClass,
                        onSelect = { selectedClass = hc }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Aesthetics (Aura Color & Title)
            Text(
                text = "3. AURA LUMINESCENCE (AESTHETICS)",
                color = LogosGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AuraColor.entries.forEach { aura ->
                    val isSelected = aura == selectedAura
                    val col = Color(android.graphics.Color.parseColor(aura.hexColor))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedAura = aura }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(if (isSelected) 10.dp else 2.dp, CircleShape, spotColor = col)
                                .clip(CircleShape)
                                .background(col)
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else RetroBorder, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aura.displayName.split(" ").first(),
                            color = if (isSelected) col else Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Epithet Title Selector
            Text(
                text = "4. HONORIFIC TITLE",
                color = LogosGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                titles.forEach { t ->
                    val isSelected = t == selectedTitle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) LogosGold.copy(alpha = 0.2f) else RetroPanel)
                            .border(1.dp, if (isSelected) LogosGold else RetroBorder, RoundedCornerShape(6.dp))
                            .clickable { selectedTitle = t }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = t,
                            color = if (isSelected) LogosGold else Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm & Enter Adventure Button
            Button(
                onClick = { confirmAndAwaken() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LogosGold,
                    contentColor = RetroBlack
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = manifest.creationEmbarkPrompt.ifBlank { "AWAKEN ➔" },
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Voice Prompt Bar for Hands-free Interaction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎤 Speak class or say 'Awaken' to start",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (speechManager != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (speechState is SpeechState.Listening) Color(0xFFEF5350) else RetroPanel)
                            .border(1.dp, LogosGold, CircleShape)
                            .clickable {
                                if (speechState is SpeechState.Listening) {
                                    speechManager.stopListening()
                                } else {
                                    speechManager.startListening { utterance -> processCreationVoiceInput(utterance) }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (speechState is SpeechState.Listening) "🎙️ LISTENING..." else "🎤 SPEAK",
                            color = LogosGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassOptionCard(
    heroClass: HeroClassDef,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val spells = remember(heroClass) { GameContent.spellsForClass(heroClass.id) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) RetroPanel else Color(0xFF141420))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) LogosGold else RetroBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = heroClass.title.uppercase(),
                        color = if (isSelected) LogosGold else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = heroClass.subtitle,
                        color = if (isSelected) LogosGlow else Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Stats Pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatPill("HP", heroClass.startingHp.toString(), Color(0xFF4CAF50))
                    StatPill("MP", heroClass.startingMp.toString(), Color(0xFF00B0FF))
                    StatPill("SPD", heroClass.startingSpeed.toString(), Color(0xFFFFB74D))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = heroClass.description,
                color = Color.LightGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Starter Spells
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                spells.forEach { sp ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222234))
                            .border(1.dp, Color(0xFF42425E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⚡ ${sp.name}",
                            color = Color(0xFF80D8FF),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label:$value",
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
