package com.greenbotz.greengate.core.assistant

import com.greenbotz.greengate.core.session.ResidentApiException
import com.greenbotz.greengate.core.session.ResidentSession
import org.json.JSONArray
import org.json.JSONObject

/** Earlier question/reply pairs sent along with the new question. */
private const val HistoryTurns = 4

/**
 * Green Bot's answer to the last of [questions], from Claude through the onboarding service
 * (`POST /api/db/resident-app/assistant/chat`). The Anthropic key lives only on that server.
 * [replies] holds the answers already given, one per earlier question.
 *
 * Uses the built-in guide without calling the server unless [ResidentSession.greenBotOnline]
 * (signed in, and the server has a key), and falls back to it on any error. Nothing here is logged.
 */
internal suspend fun askGreenBot(questions: List<String>, replies: List<String>): GreenBotReply {
    val question = questions.last()
    if (!ResidentSession.greenBotOnline) return replyToGreenBot(question)
    val messages = JSONArray()
    for (index in (questions.lastIndex - HistoryTurns).coerceAtLeast(0) until questions.lastIndex) {
        messages.put(turn("user", questions[index]))
        replies.getOrNull(index)?.let { messages.put(turn("assistant", it)) }
    }
    messages.put(turn("user", question))
    return try {
        val body = ResidentSession.askGreenBot(messages)
        val text = body.optString("reply").trim()
        if (text.isEmpty()) replyToGreenBot(question)
        else GreenBotReply(text, GreenBotAction.entries.find { it.name == body.optString("action") })
    } catch (_: ResidentApiException) {
        replyToGreenBot(question)
    }
}

private fun turn(role: String, text: String) = JSONObject().put("role", role).put("text", text)
