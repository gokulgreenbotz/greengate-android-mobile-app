package com.example.greengate

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private const val MinuteMs = 60_000L
private const val HourMs = 60 * MinuteMs

internal enum class BookingStatus { UPCOMING, COMPLETED, CANCELLED }

internal data class Booking(
    val id: String,
    val facilityId: String,
    val day: BookingDay,
    val hour: Int,
    val hours: Int,
    val guests: Int,
    val bookingFee: Int,
    val deposit: Int,
    val method: PaymentMethod,
    val bookedAt: Long,
    val cancelledAt: Long? = null,
) {
    val facility: Facility get() = Facilities.first { it.id == facilityId }
    val startAt: Long get() = day.toCalendar().apply { set(Calendar.HOUR_OF_DAY, hour) }.timeInMillis
    val endAt: Long get() = startAt + hours * HourMs
    val total: Int get() = bookingFee + deposit

    fun status(now: Long = System.currentTimeMillis()) = when {
        cancelledAt != null -> BookingStatus.CANCELLED
        endAt <= now -> BookingStatus.COMPLETED
        else -> BookingStatus.UPCOMING
    }

    /** A cancellation returns everything paid; a completed booking returns only its deposit. */
    val refundAmount: Int get() = if (cancelledAt != null) total else deposit

    // Stand-in refund timeline until a payments API exists: processed 30 minutes after the
    // slot ends (or the booking is cancelled), then paid out 45 minutes after that.
    val refundProcessedAt: Long? get() = if (refundAmount == 0) null else (cancelledAt ?: endAt) + 30 * MinuteMs
    val refundedAt: Long? get() = refundProcessedAt?.plus(45 * MinuteMs)

    fun isRefunded(now: Long = System.currentTimeMillis()) = refundedAt?.let { it <= now } == true

    /** The QR pass is shown as "Confirmed" once the slot is within three days; until then "Upcoming". */
    fun qrReady(now: Long = System.currentTimeMillis()) = status(now) == BookingStatus.UPCOMING && startAt - now <= 72 * HourMs

    // TODO: encode the signed pass from the bookings API; this only identifies the booking.
    fun qrContent() = "GREENGATE|BOOKING|$id|$facilityId|${day.year}-${day.month + 1}-${day.day}|$hour"

    val timeRange: String get() = "${hourLabel(hour)} – ${hourLabel(hour + hours)}"
    val durationLabel: String get() = if (hours == 1) "1 hour" else "$hours hours"
    val guestsLabel: String get() = if (guests == 1) "1 Person" else "$guests People"
}

internal enum class TransactionKind(val idPrefix: String) { PAYMENT("PY"), DEPOSIT("DP"), REFUND("RF") }

/** e.g. "RF829301" for the refund on booking "GG829301". */
internal fun Booking.transactionId(kind: TransactionKind) = kind.idPrefix + id.filter { it.isDigit() }

internal data class Transaction(val id: String, val booking: Booking, val kind: TransactionKind, val amount: Int, val at: Long) {
    val description: String get() = when (kind) {
        TransactionKind.PAYMENT -> "Booking payment"
        TransactionKind.DEPOSIT -> "Security deposit"
        TransactionKind.REFUND -> if (booking.cancelledAt != null) "Cancellation refund" else "Security deposit refund"
    }
}

/**
 * Payments, held deposits and refunds, newest first. A deposit shows as held until its refund
 * lands, at which point the refund replaces it.
 */
internal fun transactionsOf(bookings: List<Booking>, now: Long = System.currentTimeMillis()): List<Transaction> =
    bookings.flatMap { b ->
        buildList {
            if (b.total > 0) add(Transaction(b.transactionId(TransactionKind.PAYMENT), b, TransactionKind.PAYMENT, b.total, b.bookedAt))
            val refundedAt = b.refundedAt
            if (refundedAt != null && refundedAt <= now) add(Transaction(b.transactionId(TransactionKind.REFUND), b, TransactionKind.REFUND, b.refundAmount, refundedAt))
            else if (b.deposit > 0) add(Transaction(b.transactionId(TransactionKind.DEPOSIT), b, TransactionKind.DEPOSIT, b.deposit, b.bookedAt))
        }
    }.sortedByDescending { it.at }

/**
 * In-memory bookings until a bookings API exists: seeded with sample history around today so
 * every tab has something in it, plus whatever the resident books this session.
 */
internal object BookingStore {
    val bookings = mutableStateListOf<Booking>().apply { addAll(sampleBookings()) }

    fun find(id: String?) = bookings.find { it.id == id }

    fun add(facility: Facility, day: BookingDay, hour: Int, guests: Int, bookingFee: Int, method: PaymentMethod): Booking {
        val booking = Booking(
            id = "GG${Random.nextInt(100_000, 999_999)}", facilityId = facility.id, day = day, hour = hour, hours = 1,
            guests = guests, bookingFee = bookingFee, deposit = facility.deposit, method = method,
            bookedAt = System.currentTimeMillis()
        )
        bookings.add(0, booking)
        return booking
    }

    fun cancel(id: String) {
        val index = bookings.indexOfFirst { it.id == id }
        if (index >= 0) bookings[index] = bookings[index].copy(cancelledAt = System.currentTimeMillis())
    }
}

private fun sampleBookings(): List<Booking> {
    fun day(offset: Int) = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offset) }
        .let { BookingDay(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH)) }
    fun at(offset: Int, hour: Int, minute: Int) = day(offset).toCalendar()
        .apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute) }.timeInMillis
    return listOf(
        Booking("GG829301", "tennis", day(1), 11, 1, 2, 8, 8, PaymentMethod.CARD, at(-3, 10, 32)),
        Booking("GG514207", "pool", day(5), 16, 1, 4, 10, 10, PaymentMethod.GOOGLE_PAY, at(-1, 9, 15)),
        Booking("GG730518", "bbq", day(8), 17, 3, 8, 20, 20, PaymentMethod.CARD, at(-2, 17, 20)),
        Booking("GG402116", "pool", day(-22), 18, 1, 4, 10, 10, PaymentMethod.CARD, at(-22, 17, 45)),
        Booking("GG655842", "gym", day(-9), 7, 1, 1, 0, 5, PaymentMethod.WALLET, at(-10, 21, 5)),
        Booking("GG918374", "tennis", day(-4), 19, 1, 2, 8, 8, PaymentMethod.GOOGLE_PAY, at(-12, 13, 40), cancelledAt = at(-6, 8, 10)),
    )
}

private val DateTimeFormat get() = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH)
private val ShortDateTimeFormat get() = SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH)
private val DayFormat get() = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)

/** e.g. "2 Oct 2026, 2:30 PM". */
internal fun dateTimeLabel(ms: Long): String = DateTimeFormat.format(Date(ms))

/** e.g. "13 Mar, 5:45 PM". */
internal fun shortDateTimeLabel(ms: Long): String = ShortDateTimeFormat.format(Date(ms))

/** "Today", "Yesterday", or e.g. "15 Mar 2026". */
internal fun dayHeaderLabel(ms: Long, now: Long = System.currentTimeMillis()): String {
    fun dayOf(t: Long) = Calendar.getInstance().apply { timeInMillis = t }.let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
    val yesterday = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_MONTH, -1) }.timeInMillis
    return when (dayOf(ms)) {
        dayOf(now) -> "Today"
        dayOf(yesterday) -> "Yesterday"
        else -> DayFormat.format(Date(ms))
    }
}

internal fun money(amount: Int) = "S$$amount.00"
