package com.kampplus.hava.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kampplus.hava.R
import kotlinx.coroutines.delay

/**
 * Hava Robotu'nun yüzü: renkli robot logosu, beyaz halkalı dairenin içinde.
 *
 * Halka açık mavi → turuncu degradedir; logo hem açık hem koyu zeminde belirgin kalır.
 */
@Composable
fun RobotAvatar(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, AvatarWarm)), CircleShape)
            .border(width = size / 20, color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_robot),
            // Avatar yanındaki metinle birlikte anlam kazanır; ekran okuyucu onu okur.
            contentDescription = null,
            modifier = Modifier.size(size * 0.78f)
        )
    }
}

/**
 * Ekranın sağ altındaki robot ve konuşma balonu.
 *
 * Ekran açılınca robot küçükten büyüyerek gelir ve bir kez hafifçe "zıplar" (büyüyüp
 * eski boyuna döner); yanında [message] yazan balon aynı anda belirir. Balon 10 saniye
 * sonra kendiliğinden kaybolur. Robota dokununca [onClick] çağrılır; [onClick]
 * verilmemişse robot yeniden zıplar ve balonu tekrar gösterir.
 *
 * `Animatable`: değeri bir coroutine içinde adım adım hedefe götürür. Sırayla iki
 * `animateTo` çağırarak "büyü, sonra yerine otur" hareketini kuruyoruz.
 *
 * Ekran döndüğünde animasyon ve balon baştan oynamasın diye, oynadığı `rememberSaveable`
 * ile hatırlanır.
 *
 * @param contentDescription Ekran okuyucunun robot düğmesi için söyleyeceği eylem.
 */
@Composable
fun RobotMascot(message: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    var introPlayed by rememberSaveable { mutableStateOf(false) }
    var bubbleVisible by remember { mutableStateOf(false) }
    // Robota her dokunuşta artar; LaunchedEffect bu sayıya bağlı olduğu için animasyon yeniden oynar.
    var showRequest by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(if (introPlayed) 1f else 0.3f) }

    LaunchedEffect(showRequest) {
        if (showRequest == 0 && introPlayed) return@LaunchedEffect
        // Balon robotla aynı anda belirir; kullanıcı hiçbir şeye dokunmadan mesajı görür.
        bubbleVisible = true
        if (showRequest == 0) {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
        scale.animateTo(1.18f, tween(durationMillis = 220))
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        introPlayed = true
        delay(BUBBLE_MILLIS)
        bubbleVisible = false
    }
    val click: () -> Unit = onClick ?: { showRequest += 1 }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            visible = bubbleVisible,
            enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 1f)),
            exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(1f, 1f))
        ) {
            SpeechBubble(text = message, onClick = click)
        }
        RobotAvatar(
            size = 68.dp,
            modifier = Modifier
                .scale(scale.value)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = click)
        )
    }
}

/** Konuşma balonu: sağ alt köşesi sivri, robota doğru bakar. */
@Composable
private fun SpeechBubble(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .padding(bottom = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

/** Avatar halkasının sıcak ucu (turuncu); robotun kulaklarıyla aynı aile. */
private val AvatarWarm = Color(0xFFFFCC80)

private const val BUBBLE_MILLIS = 10_000L
