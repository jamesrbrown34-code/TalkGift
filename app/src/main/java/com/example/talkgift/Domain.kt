package com.example.talkgift

data class Person(
    val id: Int,
    val name: String,
    val aliases: String = ""
)

data class Gift(
    val id: Int,
    val personId: Int,
    val eventType: String,
    val title: String,
    val budget: Int?,
    val comments: String = "",
    val status: GiftStatus = GiftStatus.IDEA
)

enum class GiftStatus {
    IDEA,
    ORDERED,
    DELIVERED
}

data class ParsedGiftDraft(
    val person: String,
    val event: String,
    val gift: String,
    val budget: Int?
) {
    companion object {
        fun empty() = ParsedGiftDraft("", "general", "", null)
    }
}

class ParserService {
    private val events = listOf("birthday", "christmas", "xmas", "anniversary")

    fun parse(input: String, knownPeople: List<String>): ParsedGiftDraft {
        val normalized = input.lowercase().replace(Regex("[^a-z0-9£\\s]"), " ").replace(Regex("\\s+"), " ").trim()
        val budget = Regex("£?\\d+").find(normalized)?.value?.replace("£", "")?.toIntOrNull()
        val event = events.firstOrNull { normalized.contains(it) }?.replace("xmas", "christmas") ?: "general"

        val person = knownPeople.firstOrNull { normalized.contains(it.lowercase()) }
            ?: normalized.split(" ").firstOrNull().orEmpty().replaceFirstChar { it.uppercase() }

        val cleaned = normalized
            .replace(Regex("£?\\d+\\s?(quid|pounds)?"), "")
            .replace("$event", "")
            .replace(person.lowercase(), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return ParsedGiftDraft(
            person = person,
            event = event,
            gift = cleaned.ifBlank { input },
            budget = budget
        )
    }
}

object QueryBuilder {
    fun build(title: String, budget: Int?): String {
        return if (budget != null) "$title under £$budget" else title
    }
}
