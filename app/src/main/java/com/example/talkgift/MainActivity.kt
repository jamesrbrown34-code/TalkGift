package com.example.talkgift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.talkgift.ui.theme.TalkGiftTheme
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""

            // You’ll want to push this into Compose state
            spokenTextState.value = spokenText
        }
    }

    companion object {
        // simple shared state for now
        val spokenTextState = androidx.compose.runtime.mutableStateOf("")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TalkGiftTheme {
                Greeting(
                    name = "James",
                    onMicClick = {
                        startSpeechRecognition()
                    }
                )
            }
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        speechLauncher.launch(intent)
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    onMicClick: () -> Unit
) {
    val spokenText by MainActivity.spokenTextState

    Column(modifier = modifier) {
        Text(text = "Hello $name!")

        Button(onClick = onMicClick) {
            Text("🎤 Speak")
        }

        Text(text = "You said: $spokenText")
    }
}