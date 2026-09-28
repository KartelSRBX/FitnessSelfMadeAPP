package com.kartel.fitness.feature.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kartel.fitness.FitnessApp
import com.kartel.fitness.R
import com.kartel.fitness.core.ai.ActionRouter
import com.kartel.fitness.core.ai.GeminiNanoClient
import com.kartel.fitness.core.calendar.CalendarBridge
import com.kartel.fitness.core.nutrition.FoodRepository
import com.kartel.fitness.core.voice.VoiceInputManager
import kotlinx.coroutines.delay

/**
 * Плавающая кнопка голосового ввода (roadmap п.2) — держишь, говоришь, отпустил,
 * запись уходит в тот же пайплайн мини-ИИ, что и текстовый ввод на вкладке AI.
 * Живёт внутри контента экрана (не поверх нижней навигации).
 */
@Composable
fun VoiceFabButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as FitnessApp

    val viewModel: VoiceCommandViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val geminiNanoClient = GeminiNanoClient()
                VoiceCommandViewModel(
                    VoiceInputManager(context),
                    geminiNanoClient,
                    ActionRouter(
                        app.database.planDao(),
                        app.database.workDao(),
                        app.database.mealLogDao(),
                        FoodRepository(context),
                        CalendarBridge(context),
                        geminiNanoClient,
                    ),
                )
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var micPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> micPermissionGranted = granted }

    Box(modifier = modifier) {
        state.feedback?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 72.dp)
                    .widthIn(max = 260.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.feedbackIsError) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
            ) {
                Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
            LaunchedEffect(message) {
                delay(4000)
                viewModel.dismissFeedback()
            }
        }

        Surface(
            shape = CircleShape,
            color = if (state.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(64.dp)
                .pointerInput(micPermissionGranted) {
                    detectTapGestures(
                        onPress = {
                            if (!micPermissionGranted) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@detectTapGestures
                            }
                            viewModel.startListening()
                            try {
                                awaitRelease()
                            } finally {
                                viewModel.stopListening()
                            }
                        }
                    )
                },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                if (state.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                } else {
                    Icon(
                        if (micPermissionGranted) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = stringResource(R.string.voice_input_hint),
                    )
                }
            }
        }
    }
}
