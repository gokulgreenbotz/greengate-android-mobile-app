package com.example.greengate

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GreenBotAssistantTest {
    @Test
    fun quickSuggestionsOpenTheIntendedWorkflow() {
        val suggestions = mapOf(
            "Book a facility" to GreenBotAction.BOOK_FACILITY,
            "Invite a visitor" to GreenBotAction.INVITE_VISITOR,
            "Payments & deposits" to GreenBotAction.PAYMENTS,
            "My bookings" to GreenBotAction.MY_BOOKINGS,
        )
        suggestions.forEach { (query, action) ->
            assertEquals(query, action, replyToGreenBot(query).action)
        }
    }

    @Test
    fun matchingIgnoresCaseWhitespaceAndPunctuation() {
        assertEquals(GreenBotAction.MY_BOOKINGS, replyToGreenBot("  MY\nBOOKINGS?!  ").action)
        assertEquals(GreenBotAction.PROFILE, replyToGreenBot("Which ICON-SET can I choose?").action)
    }

    @Test
    fun normalizationDoesNotDependOnTheDevicesLocale() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(GreenBotAction.INVITE_VISITOR, replyToGreenBot("INVITE A VISITOR").action)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun cancellationTakesPriorityOverNewBooking() {
        val reply = replyToGreenBot("How do I cancel my gym booking?")
        assertEquals(GreenBotAction.MY_BOOKINGS, reply.action)
        assertTrue(reply.text.contains("Cancel Booking"))
        assertTrue(reply.text.contains("upcoming"))
        assertTrue(reply.text.contains("doesn't change bookings"))
    }

    @Test
    fun refundTakesPriorityOverCancellationAndBooking() {
        val reply = replyToGreenBot("When will I get a refund for my cancelled facility booking?")
        assertEquals(GreenBotAction.PAYMENTS, reply.action)
        assertTrue(reply.text.contains("Refunds"))
        assertTrue(reply.text.contains("can't verify a bank refund"))
        assertTrue(reply.text.contains("promise a refund time"))
    }

    @Test
    fun visitorCancellationDoesNotOfferFacilityCancellation() {
        val reply = replyToGreenBot("Cancel my visitor invite")
        assertEquals(GreenBotAction.INVITE_VISITOR, reply.action)
        assertTrue(reply.text.contains("Revoke"))
        assertFalse(reply.text.contains("Cancel Booking"))
    }

    @Test
    fun ambiguousCancellationAsksWhichWorkflowWithoutAnAction() {
        val reply = replyToGreenBot("How can I cancel?")
        assertNull(reply.action)
        assertTrue(reply.text.contains("facility booking or a visitor invite"))
    }

    @Test
    fun facilityAndVisitorPassesLeadToTheirOwnScreens() {
        val facilityReply = replyToGreenBot("Where is my booking QR pass?")
        assertEquals(GreenBotAction.MY_BOOKINGS, facilityReply.action)
        assertTrue(facilityReply.text.contains("View QR"))

        val visitorReply = replyToGreenBot("Where is my visitor QR pass?")
        assertEquals(GreenBotAction.INVITE_VISITOR, visitorReply.action)
        assertFalse(visitorReply.text.contains("My Bookings"))
    }

    @Test
    fun paymentDetailsTakePriorityOverGenericBooking() {
        val reply = replyToGreenBot("Find the receipt for my gym booking")
        assertEquals(GreenBotAction.PAYMENTS, reply.action)
        assertTrue(reply.text.contains("Choose an entry"))
    }

    @Test
    fun greetingWordInsideAnotherWordDoesNotMatch() {
        val reply = replyToGreenBot("What is this?")
        assertNull(reply.action)
        assertTrue(reply.text.startsWith("I don't have an answer"))
        assertFalse(reply.text.startsWith("Hello"))
    }

    @Test
    fun paymentWordInsideAnotherWordDoesNotMatch() {
        val reply = replyToGreenBot("How does repayment work?")
        assertNull(reply.action)
        assertTrue(reply.text.startsWith("I don't have an answer"))
    }

    @Test
    fun unrelatedHistoryQuestionDoesNotOpenBookings() {
        assertNull(replyToGreenBot("Tell me about world history").action)
        val reply = replyToGreenBot("Show my booking history")
        assertEquals(GreenBotAction.MY_BOOKINGS, reply.action)
        assertTrue(reply.text.contains("Upcoming, Completed, and Cancelled"))
    }

    @Test
    fun greetingDoesNotHideASpecificRequest() {
        assertTrue(replyToGreenBot("Hi!").text.startsWith("Hello"))
        assertEquals(GreenBotAction.BOOK_FACILITY, replyToGreenBot("Hi, how do I book the pool?").action)
    }

    @Test
    fun unsupportedAiCapabilitiesAreExplicit() {
        listOf("Are you AI?", "Who are you?", "Are you ChatGPT?").forEach { query ->
            val reply = replyToGreenBot(query)
            assertNull(query, reply.action)
            assertTrue(query, reply.text.contains("prepared tips"))
            assertTrue(query, reply.text.contains("don't have a connected AI service or live community data"))
            assertFalse(query, reply.text.contains("don't have a connected AI service, microphone"))
        }
    }

    @Test
    fun voiceCapabilityQueriesExplainHowToUseVoiceMode() {
        listOf("voice", "microphone", "listen", "listening", "speak", "audio").forEach { capability ->
            val query = "Can I use $capability?"
            val reply = replyToGreenBot(query)
            assertNull(query, reply.action)
            assertTrue(query, reply.text.contains("Switch to Voice and tap the microphone"))
            assertTrue(query, reply.text.contains("read its reply aloud"))
            assertTrue(query, reply.text.contains("Microphone access and speech services"))
            assertTrue(query, reply.text.contains("switch back to Chat to type"))
        }
    }

    @Test
    fun voiceWordInsideAnotherWordDoesNotMatch() {
        val reply = replyToGreenBot("Where is my invoice?")
        assertNull(reply.action)
        assertTrue(reply.text.startsWith("I don't have an answer"))
        assertFalse(reply.text.contains("Switch to Voice"))
    }

    @Test
    fun liveAvailabilityRequestGivesGuidanceWithoutClaimingAccess() {
        val reply = replyToGreenBot("Are there gym slots available right now?")
        assertEquals(GreenBotAction.BOOK_FACILITY, reply.action)
        assertTrue(reply.text.contains("can't check live availability"))
        assertTrue(reply.text.contains("reserve a slot from this chat"))
    }

    @Test
    fun visitorGuideDoesNotClaimToSendAnInvite() {
        val reply = replyToGreenBot("Invite a visitor")
        assertEquals(GreenBotAction.INVITE_VISITOR, reply.action)
        assertTrue(reply.text.contains("Create Invite"))
        assertTrue(reply.text.contains("can't create or send an invite"))
    }

    @Test
    fun communityQuestionsDoNotInventRulesOrContactDetails() {
        val reply = replyToGreenBot("What are the community rules?")
        assertEquals(GreenBotAction.COMMUNITY, reply.action)
        assertTrue(reply.text.contains("check with your community management"))
        assertTrue(reply.text.contains("don't have live community information"))
        assertFalse(reply.text.contains("+65"))
        assertFalse(reply.text.contains("24-hour"))
    }

    @Test
    fun facilityRulesAndHoursDoNotGetANewBookingAnswer() {
        listOf("What are the pool rules?", "What are the gym opening hours?").forEach { query ->
            assertEquals(query, GreenBotAction.COMMUNITY, replyToGreenBot(query).action)
        }
    }

    @Test
    fun profileAndHelpFindExistingOptions() {
        val profileReply = replyToGreenBot("How do I change the theme in settings?")
        assertEquals(GreenBotAction.PROFILE, profileReply.action)
        assertTrue(profileReply.text.contains("Show Feedback"))

        val helpReply = replyToGreenBot("How to use this app?")
        assertNull(helpReply.action)
        assertTrue(helpReply.text.contains("Choose a suggestion"))
    }

    @Test
    fun emptyAndUnknownPromptsOfferTheAvailableSubjects() {
        listOf("", "  \n  ", "?!", "Tell me the weather").forEach { query ->
            val reply = replyToGreenBot(query)
            assertNull(query, reply.action)
            assertTrue(query, reply.text.contains("visitor invites"))
            assertTrue(query, reply.text.contains("payments and deposits"))
            assertTrue(query, reply.text.contains("profile settings"))
        }
    }
}
