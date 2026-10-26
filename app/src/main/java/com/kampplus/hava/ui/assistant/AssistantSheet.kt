package com.kampplus.hava.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kampplus.hava.R
import com.kampplus.hava.model.AssistantReply
import com.kampplus.hava.model.ReplyArg
import com.kampplus.hava.model.ReplyLine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Hava Robotu sohbeti: alttan açılan panel.
 *
 * Mesajlar ve şehir araması [AssistantViewModel]'dadır; panel yalnızca çizer ve
 * gönderilen soruyu ona iletir. Cevap beklenirken robot "yazıyor…" balonu gösterir.
 *
 * @param cityName Sohbet bir şehirden açıldıysa adı (başlıkta yazar); şehir ekranından
 *   açıldıysa null.
 * @param suggestions Panelin altındaki örnek sorular.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSheet(viewModel: AssistantViewModel, cityName: String?, suggestions: List<Int>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val thinking by viewModel.isThinking.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .imePadding()
        ) {
            AssistantHeader(cityName = cityName)

            val listState = rememberLazyListState()
            // Yeni mesaj ya da "yazıyor…" balonu gelince en alta kaydır.
            LaunchedEffect(messages.size, thinking) {
                listState.animateScrollToItem(messages.size - 1 + if (thinking) 1 else 0)
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    when (message) {
                        is ChatMessage.User -> UserBubble(text = message.text)
                        is ChatMessage.Bot -> BotBubble(text = message.reply.asText(message.today))
                    }
                }
                if (thinking) {
                    item(key = "thinking") { BotBubble(text = stringResource(R.string.bot_thinking), italic = true) }
                }
            }

            SuggestionRow(suggestions = suggestions, onAsk = viewModel::ask)
            QuestionInput(onSend = viewModel::ask)
        }
    }
}

@Composable
private fun AssistantHeader(cityName: String?) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RobotAvatar(size = 52.dp)
        Column {
            Text(text = stringResource(R.string.bot_name), style = MaterialTheme.typography.titleLarge)
            Text(
                text = cityName?.let { stringResource(R.string.bot_subtitle, it) } ?: stringResource(R.string.bot_subtitle_general),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Robotun balonu: solda, avatarla birlikte, açık zeminde. */
@Composable
private fun BotBubble(text: String, italic: Boolean = false) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RobotAvatar(size = 36.dp)
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = if (italic) FontStyle.Italic else null,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

/** Kullanıcının balonu: sağda, koyu mavi zeminde beyaz yazı. */
@Composable
private fun UserBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp))
        }
    }
}

/** Örnek sorular; dokununca o soru gönderilir. Robotun anladığı türden sorular olduğunu testler garanti eder. */
@Composable
private fun SuggestionRow(suggestions: List<Int>, onAsk: (String) -> Unit) {
    val questions = suggestions.map { stringResource(it) }
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(questions) { question ->
            SuggestionChip(onClick = { onAsk(question) }, label = { Text(question) })
        }
    }
}

/** Soru kutusu. Gönderilen soru kutudan silinir; boş soru gönderilmez. */
@Composable
private fun QuestionInput(onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val send = {
        val question = text.trim()
        if (question.isNotEmpty()) {
            onSend(question)
            text = ""
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            // Kalın mavi çerçeve: alanın kendi ince kenarı yerine dışarıdan çizilir.
            .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape),
        placeholder = { Text(stringResource(R.string.bot_input_hint)) },
        singleLine = true,
        shape = CircleShape,
        // Beyaz soru kutusu; çerçeveyi yukarıdaki `border` çizer, alanın kendi kenarı gizlenir.
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        trailingIcon = {
            IconButton(onClick = send, enabled = text.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.bot_send))
            }
        },
        // Klavyedeki Enter tuşu "Gönder" olur.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { send() })
    )
}

/** Cevabın bütün cümlelerini o anki dilde, alt alta birleştirir. */
@Composable
private fun AssistantReply.asText(today: LocalDate?): String = lines.map { it.asText(today) }.joinToString("\n")

/**
 * Bir cümleyi o anki dilde kurar: boşluklardaki gün, hava durumu adı ve liste
 * değerleri çözülür, cümlenin ilk harfi büyütülür ("yarın yağmur…" → "Yarın yağmur…").
 */
@Composable
private fun ReplyLine.asText(today: LocalDate?): String {
    val locale = LocalConfiguration.current.locales[0]
    val values = args.map { arg ->
        when (arg) {
            is ReplyArg.Text -> arg.value
            is ReplyArg.Number -> arg.value
            is ReplyArg.Res -> stringResource(arg.res)
            is ReplyArg.Day -> dayText(arg.date, today)
            is ReplyArg.Items -> arg.res.map { stringResource(it) }.joinToString(", ")
        }
    }
    return stringResource(res, *values.toTypedArray()).replaceFirstChar { it.titlecase(locale) }
}

/**
 * "bugün", "yarın" ya da gün adı. Türkçede gün adları cümle içinde küçük yazılır
 * ("cuma"), İngilizcede her zaman büyük ("Friday").
 */
@Composable
private fun dayText(date: LocalDate, today: LocalDate?): String {
    val locale = LocalConfiguration.current.locales[0]
    return when (date) {
        today -> stringResource(R.string.day_today_lower)
        today?.plusDays(1) -> stringResource(R.string.day_tomorrow_lower)
        else -> {
            val name = date.format(DateTimeFormatter.ofPattern("EEEE", locale))
            if (locale.language == "tr") name.lowercase(locale) else name
        }
    }
}

/** Bir şehrin tahmininden açılan sohbetin örnek soruları. */
val CitySuggestions = listOf(
    R.string.bot_suggest_rain,
    R.string.bot_suggest_wear,
    R.string.bot_suggest_picnic,
    R.string.bot_suggest_best,
    R.string.bot_suggest_uv,
    R.string.bot_suggest_sun
)

/** Şehir seçmeden açılan sohbetin örnek soruları; her biri bir şehir adı içerir. */
val GeneralSuggestions = listOf(
    R.string.bot_suggest_city_1,
    R.string.bot_suggest_city_2,
    R.string.bot_suggest_city_3,
    R.string.bot_suggest_city_4
)
