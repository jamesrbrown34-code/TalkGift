package com.example.talkgift

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.talkgift.ui.theme.TalkGiftTheme

class MainActivity : ComponentActivity() {
    private val parserService = ParserService()

    private val people = mutableStateListOf<Person>()
    private val gifts = mutableStateListOf<Gift>()
    private val parsedDraftState = mutableStateOf(ParsedGiftDraft.empty())

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull().orEmpty()
            parsedDraftState.value = parserService.parse(spokenText, people.map { it.name })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TalkGiftTheme {
                GiftCaptureApp(
                    people = people,
                    gifts = gifts,
                    parsedDraft = parsedDraftState.value,
                    onStartCapture = ::startSpeechRecognition,
                    onDraftChange = { parsedDraftState.value = it },
                    onSaveDraft = { draft ->
                        val person = people.firstOrNull { it.name.equals(draft.person, ignoreCase = true) }
                            ?: Person(id = people.size + 1, name = draft.person.ifBlank { "Unknown" }).also {
                                people.add(it)
                            }
                        gifts.add(
                            Gift(
                                id = gifts.size + 1,
                                personId = person.id,
                                eventType = draft.event,
                                title = draft.gift,
                                budget = draft.budget
                            )
                        )
                        parsedDraftState.value = ParsedGiftDraft.empty()
                    }
                )
            }
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Capture gift idea")
        }
        speechLauncher.launch(intent)
    }
}

@Composable
private fun GiftCaptureApp(
    people: List<Person>,
    gifts: List<Gift>,
    parsedDraft: ParsedGiftDraft,
    onStartCapture: () -> Unit,
    onDraftChange: (ParsedGiftDraft) -> Unit,
    onSaveDraft: (ParsedGiftDraft) -> Unit
) {
    var selectedPerson by remember { mutableStateOf<Person?>(null) }
    var selectedGift by remember { mutableStateOf<Gift?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("TalkGift", style = MaterialTheme.typography.headlineMedium)
            CaptureCard(parsedDraft, onDraftChange, onStartCapture, onSaveDraft)
            PersonList(people = people, gifts = gifts, onSelect = { selectedPerson = it; selectedGift = null })
            selectedPerson?.let { person ->
                GiftList(person = person, gifts = gifts.filter { it.personId == person.id }, onSelect = { selectedGift = it })
            }
            selectedGift?.let { GiftDetail(it) }
        }
    }
}

@Composable
private fun CaptureCard(
    draft: ParsedGiftDraft,
    onDraftChange: (ParsedGiftDraft) -> Unit,
    onStartCapture: () -> Unit,
    onSaveDraft: (ParsedGiftDraft) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartCapture) { Text("🎤 Speak gift") }
            OutlinedTextField(draft.person, { onDraftChange(draft.copy(person = it)) }, label = { Text("Person") })
            OutlinedTextField(draft.event, { onDraftChange(draft.copy(event = it)) }, label = { Text("Event") })
            OutlinedTextField(draft.gift, { onDraftChange(draft.copy(gift = it)) }, label = { Text("Gift") })
            OutlinedTextField(draft.budget?.toString().orEmpty(), {
                onDraftChange(draft.copy(budget = it.toIntOrNull()))
            }, label = { Text("Budget (£)") })
            Button(onClick = { onSaveDraft(draft) }, enabled = draft.gift.isNotBlank()) { Text("Save") }
        }
    }
}

@Composable
private fun PersonList(people: List<Person>, gifts: List<Gift>, onSelect: (Person) -> Unit) {
    Text("People", style = MaterialTheme.typography.titleMedium)
    LazyColumn(modifier = Modifier.height(140.dp)) {
        items(people) { person ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(person) }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(person.name)
                Text("${gifts.count { it.personId == person.id }} gifts")
            }
        }
    }
}

@Composable
private fun GiftList(person: Person, gifts: List<Gift>, onSelect: (Gift) -> Unit) {
    Text("${person.name}'s gifts", style = MaterialTheme.typography.titleMedium)
    LazyColumn(modifier = Modifier.height(140.dp)) {
        items(gifts) { gift ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(gift) }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(gift.title)
                Text(gift.eventType)
            }
        }
    }
}

@Composable
private fun GiftDetail(gift: Gift) {
    val context = LocalContext.current
    val query = QueryBuilder.build(gift.title, gift.budget)
    Text("Gift detail", style = MaterialTheme.typography.titleMedium)
    Text("Title: ${gift.title}")
    Text("Budget: ${gift.budget?.let { "£$it" } ?: "N/A"}")
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.co.uk/s?k=${Uri.encode(query)}")))
        }) { Text("Search Amazon") }
        Button(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")))
        }) { Text("Search Google") }
    }
}
