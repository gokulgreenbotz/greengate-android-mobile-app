package com.example.greengate

import java.util.Locale

internal data class GreenBotReply(
    val text: String,
    val action: GreenBotAction? = null,
)

internal enum class GreenBotAction {
    BOOK_FACILITY,
    MY_BOOKINGS,
    INVITE_VISITOR,
    PAYMENTS,
    COMMUNITY,
    PROFILE,
}

/** Prepared app guidance only; this function never reads or changes a resident's records. */
internal fun replyToGreenBot(query: String): GreenBotReply {
    val words = GreenBotWords.findAll(query.lowercase(Locale.ROOT)).map { it.value }.toList()
    val normalized = words.joinToString(" ")
    val tokens = words.toSet()
    fun has(vararg values: String) = values.any { it in tokens }
    fun phrase(value: String) = " $normalized ".contains(" $value ")

    if (normalized.isEmpty()) {
        return GreenBotReply("I'm here to help. $GreenBotSubjects")
    }

    if (has("microphone", "listen", "listening", "voice", "speak", "audio")) {
        return GreenBotReply(
            "Switch to Voice and tap the microphone to speak. Green Bot can read its reply aloud. " +
                "Microphone access and speech services on your phone are needed. " +
                "You can switch back to Chat to type."
        )
    }

    if (has("ai", "chatgpt") ||
        phrase("who are you") || phrase("what are you") || phrase("are you a bot")
    ) {
        return GreenBotReply(
            "I'm GreenGate's built-in app guide with prepared tips. " +
                "I don't have a connected AI service or live community data. " +
                "Type a question about an app feature and I'll help you find it."
        )
    }

    val visitorTopic = has("visitor", "visitors", "invite", "invites", "invitation", "invitations")

    // Specific requests take priority over words such as "book" in the same question.
    if (has("refund", "refunds", "refunded", "refunding")) {
        return GreenBotReply(
            "Open Payments & Deposits and choose Refunds to review the status shown in the app. " +
                "A booking's details also show its deposit or refund summary. " +
                "I can't verify a bank refund or promise a refund time from this chat.",
            GreenBotAction.PAYMENTS,
        )
    }

    if (has("cancel", "cancellation", "cancellations", "cancelling", "canceling", "revoke")) {
        return if (visitorTopic) {
            GreenBotReply(
                "For an existing visitor invite, open Visitor Management and select the invite. " +
                    "Use Revoke when available and review the confirmation. This chat can't revoke invites.",
                GreenBotAction.INVITE_VISITOR,
            )
        } else if (has("book", "booking", "bookings", "reservation", "reservations", "facility", "facilities")) {
            GreenBotReply(
                "Open My Bookings, choose an upcoming booking, then View Details and Cancel Booking. " +
                    "Review the confirmation before cancelling. This chat doesn't change bookings.",
                GreenBotAction.MY_BOOKINGS,
            )
        } else {
            GreenBotReply("Do you mean a facility booking or a visitor invite? Ask about either and I'll show you where to go.")
        }
    }

    if (has("qr", "pass", "passes")) {
        return if (visitorTopic) {
            GreenBotReply(
                "Open Visitor Management and select an invite to see its available pass options. " +
                    "This chat can't generate or share a visitor pass.",
                GreenBotAction.INVITE_VISITOR,
            )
        } else {
            GreenBotReply(
                "Open My Bookings and find an upcoming booking. Tap View QR to see its facility pass, " +
                    "or View Details to check the booking information.",
                GreenBotAction.MY_BOOKINGS,
            )
        }
    }

    if (has("payment", "payments", "deposit", "deposits", "receipt", "receipts", "paid", "pay")) {
        return GreenBotReply(
            "Open Payments & Deposits to review the payment, deposit, and refund entries shown in the app. " +
                "Choose an entry to see its details. When booking a facility, review the payment summary " +
                "on the booking screen. This chat can't make a payment or verify a bank transaction.",
            GreenBotAction.PAYMENTS,
        )
    }

    if (visitorTopic) {
        return GreenBotReply(
            "Open Visitor Management and choose Create Invite. Select the visit type, fill in the visitor " +
                "details, and follow the options shown on that screen. I can guide you, but I can't " +
                "create or send an invite from this chat.",
            GreenBotAction.INVITE_VISITOR,
        )
    }

    if (phrase("my bookings") || phrase("my booking") || phrase("my reservations") ||
        has("bookings") ||
        (has("booking", "reservation") &&
            has("view", "find", "existing", "upcoming", "past", "completed", "cancelled", "canceled", "details", "status", "history"))
    ) {
        return GreenBotReply(
            "Open My Bookings to browse Upcoming, Completed, and Cancelled bookings. " +
                "Choose View Details to see a booking's information; upcoming bookings also have View QR.",
            GreenBotAction.MY_BOOKINGS,
        )
    }

    if (has("rules", "policy", "policies", "documents", "contacts", "management", "security") ||
        phrase("opening hours") || phrase("pool hours") || phrase("gym hours") ||
        phrase("bbq hours") || phrase("tennis hours")
    ) {
        return greenBotCommunityReply()
    }

    if (has("facility", "facilities", "pool", "gym", "bbq", "tennis", "book", "booking", "reserve", "reservation", "availability", "slots")) {
        return GreenBotReply(
            "Open Book Facility, select a facility, and choose a date, time slot, and guest count. " +
                "Continue to review the booking summary and the options shown there. " +
                "I can't check live availability or reserve a slot from this chat.",
            GreenBotAction.BOOK_FACILITY,
        )
    }

    if (has("profile", "settings", "theme", "themes", "icons", "logout") ||
        phrase("icon set") || phrase("log out") || phrase("sign out")
    ) {
        return GreenBotReply(
            "Open Profile to review your resident information, choose an app theme or icon set, " +
                "change the Show Feedback option, or log out.",
            GreenBotAction.PROFILE,
        )
    }

    if (has("community")) {
        return greenBotCommunityReply()
    }

    if (normalized in GreenBotGreetings) {
        return GreenBotReply("Hello! I'm here to help you use GreenGate. $GreenBotSubjects")
    }

    if (has("help") || phrase("how to use") || phrase("what can you do")) {
        return GreenBotReply(GreenBotSubjects)
    }

    return GreenBotReply("I don't have an answer for that topic in this app guide. $GreenBotSubjects")
}

private fun greenBotCommunityReply() = GreenBotReply(
    "Open Community Info to see the document hub and the contacts listed in the app. " +
        "For current rules, opening hours, charges, or contact information, check with your " +
        "community management. I don't have live community information here.",
    GreenBotAction.COMMUNITY,
)

private val GreenBotWords = Regex("[\\p{L}\\p{N}]+")
private val GreenBotGreetings = setOf("hi", "hello", "hey", "good morning", "good afternoon", "good evening")
private const val GreenBotSubjects = "I can help with booking facilities, My Bookings and QR passes, " +
    "visitor invites, payments and deposits, community information, and profile settings. " +
    "Choose a suggestion or ask about one of those."
