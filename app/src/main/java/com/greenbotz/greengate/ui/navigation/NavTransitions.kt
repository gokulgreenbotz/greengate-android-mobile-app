package com.greenbotz.greengate.ui.navigation

// Package stays com.greenbotz.greengate until the project-wide
// "Package name does not match containing directory" fix moves it to ui.navigation.

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

// NavHost's own default, kept for every screen outside the booking flow.
internal const val NavFadeMillis = 700
private const val BookingSlideMillis = 380

// Book Facility and the steps it leads to move like a stack: the new page slides in over the
// old one, which drifts a quarter-width behind it, and Back plays the same thing in reverse.
// Slides only, no fades: a partial alpha renders the whole screen offscreen every frame.
private val BookingFlowRoutes = setOf(Screen.BookFacility.route, Screen.FacilityAbout.route, Screen.BookingPayment.route, Screen.BookingConfirmed.route)

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.inBookingFlow() =
    initialState.destination.route in BookingFlowRoutes || targetState.destination.route in BookingFlowRoutes

private fun <T> bookingSpec() = tween<T>(BookingSlideMillis, easing = FastOutSlowInEasing)

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.bookingPushIn() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, bookingSpec())

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.bookingPushOut() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, bookingSpec()) { it / 4 }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.bookingPopIn() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, bookingSpec()) { it / 4 }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.bookingPopOut() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, bookingSpec())
