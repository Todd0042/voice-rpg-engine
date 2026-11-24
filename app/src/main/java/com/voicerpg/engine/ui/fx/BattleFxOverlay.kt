package com.voicerpg.engine.ui.fx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicerpg.engine.content.PresentationConfig.DamageNumberStyle
import kotlin.math.roundToInt

/** Damage and heal numbers over the arena, in the game's [DamageNumberStyle]. */
@Composable
fun DamageNumbersLayer(director: BattleFxDirector, modifier: Modifier = Modifier) {
    director.frameTick // recompose every frame while numbers fly
    val style = director.presentation.damageNumberStyle
    Box(modifier = modifier.fillMaxSize()) {
        for (n in director.numbers) {
            val age = director.time - n.bornAt
            val (dx, dy) = n.riseAt(age)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset { IntOffset((n.x - 36f + dx.dp.toPx()).roundToInt(), (n.y - 52f + dy.dp.toPx()).roundToInt()) }
                    .graphicsLayer {
                        val s = n.scaleAt(age)
                        scaleX = s
                        scaleY = s
                        alpha = n.alphaAt(age)
                    }
            ) {
                if (n.label != null) {
                    Text(
                        text = n.label,
                        style = numberStyle(style, n.labelColor, if (n.big) 13 else 11),
                        modifier = labelBackground(style)
                    )
                }
                Text(text = n.text, style = numberStyle(style, n.color, if (n.big) 24 else 19), modifier = numberBackground(style))
            }
        }
    }
}

private fun numberStyle(style: DamageNumberStyle, color: Color, sizeSp: Int): TextStyle = when (style) {
    DamageNumberStyle.POP -> TextStyle(
        color = color, fontSize = sizeSp.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace,
        shadow = Shadow(Color.Black, blurRadius = 4f)
    )
    DamageNumberStyle.RECEIPT -> TextStyle(
        color = Color(0xFF212121), fontSize = (sizeSp - 3).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
    )
    DamageNumberStyle.COMIC -> TextStyle(
        color = color, fontSize = (sizeSp + 2).sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic,
        shadow = Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(3f, 3f), blurRadius = 0f)
    )
}

private fun numberBackground(style: DamageNumberStyle): Modifier = when (style) {
    DamageNumberStyle.RECEIPT -> Modifier
        .background(Color(0xFFFAFAF0), RoundedCornerShape(1.dp))
        .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(1.dp))
        .padding(horizontal = 6.dp, vertical = 1.dp)
    else -> Modifier
}

private fun labelBackground(style: DamageNumberStyle): Modifier = when (style) {
    DamageNumberStyle.COMIC -> Modifier
        .background(Color(0xFFFFEB3B), RoundedCornerShape(3.dp))
        .border(2.dp, Color.Black, RoundedCornerShape(3.dp))
        .padding(horizontal = 5.dp)
    DamageNumberStyle.RECEIPT -> Modifier.background(Color(0xFFFAFAF0)).padding(horizontal = 4.dp)
    DamageNumberStyle.POP -> Modifier
}

/** The full-screen white flash (Transcendental hits), already limited by the director. */
@Composable
fun ScreenFlashLayer(director: BattleFxDirector, modifier: Modifier = Modifier) {
    val a = director.screenFlash
    if (a > 0f) Box(modifier = modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f * a)))
}

/** The current move name, in the game's title-card style; slides in from the acting side. */
@Composable
fun TitleCardLayer(director: BattleFxDirector, modifier: Modifier = Modifier) {
    val card = director.titleCard ?: return
    director.frameTick
    val age = director.time - card.bornAt
    val slideIn = (age / 0.16f).coerceIn(0f, 1f)
    val fadeOut = ((ShownTitleCard.LIFETIME - age) / 0.25f).coerceIn(0f, 1f)
    val fromSide = if (card.fromParty) -1f else 1f
    Box(modifier = modifier.fillMaxSize().padding(top = 36.dp), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = fromSide * (1f - slideIn) * size.width * 0.8f
                alpha = fadeOut
            }
        ) {
            TitleCardBody(card.text, director.presentation.titleCardStyle)
        }
    }
}

@Composable
private fun TitleCardBody(text: String, style: com.voicerpg.engine.content.PresentationConfig.TitleCardStyle) {
    when (style) {
        com.voicerpg.engine.content.PresentationConfig.TitleCardStyle.BANNER -> Text(
            text = text.uppercase(),
            color = Color(0xFFFFE082),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Transparent, Color(0xE6101018), Color(0xE6101018), Color.Transparent)))
                .padding(horizontal = 36.dp, vertical = 6.dp)
        )
        com.voicerpg.engine.content.PresentationConfig.TitleCardStyle.HEADLINE -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color(0xFFF5F1E6), RoundedCornerShape(2.dp))
                .border(2.dp, Color(0xFF111111), RoundedCornerShape(2.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text("— BREAKING —", color = Color(0xFFB71C1C), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(text.uppercase(), color = Color(0xFF111111), fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif)
        }
        com.voicerpg.engine.content.PresentationConfig.TitleCardStyle.PA_ANNOUNCEMENT -> Text(
            text = "📢  ${text.uppercase()}",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .background(Color(0xFF1565C0), RoundedCornerShape(6.dp))
                .border(2.dp, Color(0xFFFFEB3B), RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
        com.voicerpg.engine.content.PresentationConfig.TitleCardStyle.SCROLL -> Text(
            text = "❦  $text  ❦",
            color = Color(0xFF4E342E),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
            modifier = Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFE8D5A3), Color(0xFFF6E7C1), Color(0xFFE0C98F))),
                    RoundedCornerShape(10.dp)
                )
                .border(2.dp, Color(0xFF8D6E63), RoundedCornerShape(10.dp))
                .padding(horizontal = 20.dp, vertical = 6.dp)
        )
    }
}
