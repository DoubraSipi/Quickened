package com.quickened.content

import android.content.Context
import java.util.Calendar

// Offline "fresh word" composer: weaves a novel reflection from local verses +
// tone openers + moment closers. Deterministic variety, zero network, tiny RAM.
// Seam: replace compose() internals with an on-device model later; callers stay same.
object FreshWord {
    private val openers = mapOf(
        "gentle" to listOf("Rest a moment.", "Breathe slowly.", "Be still with me."),
        "encouraging" to listOf("Lift your head.", "Today is yours in Christ.", "Take heart."),
        "contemplative" to listOf("Pause and listen.", "Quiet your heart.", "Consider this."),
        "challenging" to listOf("Hear this clearly.", "Decide today.", "Do not delay.")
    )

    private val closers = mapOf(
        "walking" to "Carry this with each step.",
        "resting" to "Sit with this a while.",
        "stationary" to "Let this settle over you.",
        "driving" to "Hold this thought for the road.",
        "workout" to "Push through with this in mind.",
        "unknown" to "Keep this with you."
    )

    private fun daypart(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "this morning"
            in 12..17 -> "this afternoon"
            in 18..22 -> "this evening"
            else -> "tonight"
        }
    }

    fun compose(context: Context, tone: String, activity: String, translation: String = "simple"): Reflection {
        val verses = ContentRepository.load(context, tone, translation).filter { it.type == "verse" }
        val base = if (verses.isNotEmpty()) verses.random()
        else Reflection("Quiet moment", "Be still, and know that I am God.")
        val opener = openers[tone]?.random() ?: "Listen."
        val closer = closers[activity] ?: closers.getValue("unknown")
        val text = "$opener ${base.text} Given ${daypart()}, ${closer}"
        return Reflection(title = "Fresh word — ${base.title}", text = text, type = "fresh")
    }
}
