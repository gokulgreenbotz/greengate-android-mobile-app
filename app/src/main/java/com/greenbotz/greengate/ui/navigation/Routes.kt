package com.greenbotz.greengate.ui.navigation

import com.greenbotz.greengate.ui.screens.facilities.BookingDay
import com.greenbotz.greengate.ui.screens.visitors.InviteType

// Package stays com.greenbotz.greengate until the project-wide
// "Package name does not match containing directory" fix moves it to ui.navigation.

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Community : Screen("community")
    data object Access : Screen("access")
    data object Profile : Screen("profile")
    data object BookFacility : Screen("book_facility")
    data object FacilityAbout : Screen("facility/{id}") {
        fun create(facilityId: String) = "facility/$facilityId"
    }
    data object InviteVisitors : Screen("invite_visitors")
    data object EForms : Screen("e_forms")
    data object Feedback : Screen("feedback")
    data object Announcements : Screen("announcements")
    data object Search : Screen("search")
    data object Notifications : Screen("notifications")
    data object LocationPicker : Screen("location_picker")
    data object Bookings : Screen("bookings")
    data object BookingPayment : Screen("booking_payment/{facility}/{date}/{hour}/{guests}") {
        internal fun create(facilityId: String, day: BookingDay, hour: Int, guests: Int) =
            "booking_payment/${bookingPath(facilityId, day, hour, guests)}"
    }
    data object BookingConfirmed : Screen("booking_confirmed/{id}") {
        fun create(bookingId: String) = "booking_confirmed/$bookingId"
    }
    data object BookingDetail : Screen("booking/{id}") {
        fun create(bookingId: String) = "booking/$bookingId"
    }
    data object Payments : Screen("payments")
    data object GreenBot : Screen("green_bot")
    data object CreateInvite : Screen("create_invite/{type}") {
        internal fun create(type: InviteType) = "create_invite/${type.name}"
    }
    data object TransactionDetail : Screen("transaction/{id}") {
        fun create(transactionId: String) = "transaction/$transactionId"
    }
    data object Visitors : Screen("visitors")
    data object InviteCreated : Screen("invite_created/{id}") {
        fun create(inviteId: String) = "invite_created/$inviteId"
    }
    data object InviteDetail : Screen("invite/{id}") {
        fun create(inviteId: String) = "invite/$inviteId"
    }
    data object CallTest : Screen("call_test")
    data object CommunityInfo : Screen("community_info")
}

// The booking routes share {facility}/{date}/{hour}/{guests}; the date is "yyyy-M-d".
private fun bookingPath(facilityId: String, day: BookingDay, hour: Int, guests: Int) =
    "$facilityId/${day.year}-${day.month + 1}-${day.day}/$hour/$guests"

fun isMainTab(route: String?): Boolean {
    return route in listOf(Screen.Home.route, Screen.Community.route, Screen.Access.route, Screen.Profile.route)
}
