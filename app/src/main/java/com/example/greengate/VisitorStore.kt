package com.example.greengate

import androidx.compose.runtime.mutableStateListOf
import java.util.Calendar

private const val MinuteMs = 60_000L

internal enum class InviteStatus { ACTIVE, EXPECTED, UPCOMING, USED, EXPIRED, REVOKED }

internal class AccessEvent(val place: String, val at: Long, val entry: Boolean)

internal class VehicleRequest(val region: String, val plate: String, val reason: String, val requestedAt: Long)

internal enum class VehicleChangeKind { REQUESTED, APPROVED, REJECTED }

/** One step in an invite's vehicle history: a visitor's request, or the resident's decision on it. */
internal class VehicleChange(val kind: VehicleChangeKind, val region: String, val plate: String, val at: Long, val reason: String = "")

internal data class VisitorInvite(
    val id: String,
    val name: String,
    val type: InviteType,
    // e.g. "Family", "Friend", "Pickup"; shown under the name.
    val relationship: String,
    val countryCode: String,
    val mobile: String,
    val day: BookingDay,
    val startMinutes: Int,
    val validHours: Int,
    val vehicleRegion: String?,
    val vehicle: String?,
    val visitorCanEditVehicle: Boolean,
    val faceRegistration: Boolean,
    val faceRegistered: Boolean = false,
    val repeat: String?,
    val notifyOnEntry: Boolean,
    // null means unlimited.
    val maxEntries: Int?,
    val note: String,
    val createdAt: Long,
    val revokedAt: Long? = null,
    val access: List<AccessEvent> = emptyList(),
    val vehicleRequest: VehicleRequest? = null,
    // The vehicle given at creation, before any approved changes.
    val createdVehicleRegion: String? = vehicleRegion,
    val createdVehicle: String? = vehicle,
    val vehicleChanges: List<VehicleChange> = vehicleRequest?.let {
        listOf(VehicleChange(VehicleChangeKind.REQUESTED, it.region, it.plate, it.requestedAt, it.reason))
    } ?: emptyList(),
) {
    val startAt: Long get() = day.toCalendar().timeInMillis + startMinutes * MinuteMs
    val endAt: Long get() = startAt + validHours * 60 * MinuteMs
    val entries: Int get() = access.count { it.entry }

    /** Who the visitor is, as listed: "Family", or e.g. "Cab • Pickup" or "Delivery • GrabFood". */
    val kindLabel: String get() = when (type) {
        InviteType.FAMILY -> relationship
        InviteType.CAB -> "Cab • $relationship"
        InviteType.DELIVERY -> "Delivery • $relationship"
        else -> type.title
    }

    fun status(now: Long = System.currentTimeMillis()): InviteStatus = when {
        revokedAt != null -> InviteStatus.REVOKED
        maxEntries != null && entries >= maxEntries && endAt <= now -> InviteStatus.USED
        endAt <= now -> if (entries > 0) InviteStatus.USED else InviteStatus.EXPIRED
        startAt <= now -> InviteStatus.ACTIVE
        sameDay(startAt, now) -> InviteStatus.EXPECTED
        else -> InviteStatus.UPCOMING
    }

    val timeRange: String get() =
        if (validHours >= 24) "From ${clockLabel(startMinutes)} (24 hours)"
        else "${clockLabel(startMinutes)} – ${clockLabel(startMinutes + validHours * 60)}"

    /** "Today, 10:00 AM – 12:00 PM", or e.g. "10 Mar, 10:00 AM – 12:00 PM". */
    fun whenLabel(now: Long = System.currentTimeMillis()): String {
        val dayPart = when {
            sameDay(startAt, now) -> "Today"
            sameDay(startAt, now + 24 * 60 * MinuteMs) -> "Tomorrow"
            else -> day.shortLabel()
        }
        return "$dayPart, $timeRange"
    }

    val validForLabel: String get() = if (validHours >= 24) "Full Day" else if (validHours == 1) "1 Hour" else "$validHours Hours"
    val vehicleLabel: String get() = if (vehicle.isNullOrBlank()) "-" else "${vehicleRegion ?: "SG"} • $vehicle"
    val mobileLabel: String get() = "$countryCode $mobile"

    // TODO: encode the signed pass from the visitor API; this only identifies the invite.
    fun qrContent() = "GREENGATE|VISITOR|$id"
}

private fun sameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

/**
 * In-memory visitor invites until a visitor API exists: seeded with samples around now so
 * every tab has something in it, plus whatever the resident creates this session.
 */
internal object VisitorStore {
    val invites = mutableStateListOf<VisitorInvite>().apply { addAll(sampleInvites()) }

    fun find(id: String?) = invites.find { it.id == id }

    /** Gives the invite an id like "VIS-2026-03-12-1000" from its date and start time. */
    fun add(draft: VisitorInvite): VisitorInvite {
        val base = "VIS-%04d-%02d-%02d-%02d%02d".format(
            draft.day.year, draft.day.month + 1, draft.day.day, draft.startMinutes / 60, draft.startMinutes % 60
        )
        var id = base
        var n = 2
        while (find(id) != null) id = "$base-${n++}"
        return draft.copy(id = id).also { invites.add(0, it) }
    }

    fun revoke(id: String) = update(id) { it.copy(revokedAt = System.currentTimeMillis()) }

    fun resolveVehicleRequest(id: String, approve: Boolean) = update(id) {
        val request = it.vehicleRequest ?: return@update it
        val decision = VehicleChange(if (approve) VehicleChangeKind.APPROVED else VehicleChangeKind.REJECTED,
            request.region, request.plate, System.currentTimeMillis())
        val resolved = it.copy(vehicleRequest = null, vehicleChanges = it.vehicleChanges + decision)
        if (approve) resolved.copy(vehicleRegion = request.region, vehicle = request.plate) else resolved
    }

    private fun update(id: String, change: (VisitorInvite) -> VisitorInvite) {
        val index = invites.indexOfFirst { it.id == id }
        if (index >= 0) invites[index] = change(invites[index])
    }
}

private fun sampleInvites(): List<VisitorInvite> {
    val now = System.currentTimeMillis()
    fun dayAt(offset: Int) = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offset) }
        .let { BookingDay(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH)) }
    val nowMinutes = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
    // Started on the last half hour, so it's active now; may begin "yesterday" just after midnight.
    val activeStart = (nowMinutes - 30).coerceAtLeast(0) / 30 * 30
    fun base(id: String, name: String, type: InviteType, rel: String, day: BookingDay, start: Int, hours: Int) = VisitorInvite(
        id, name, type, rel, "+65", "8123 4567", day, start, hours, null, null,
        visitorCanEditVehicle = false, faceRegistration = false, repeat = null, notifyOnEntry = true, maxEntries = null,
        note = "", createdAt = now - 2 * 24 * 60 * MinuteMs
    )
    val john = base("VIS-SAMPLE-1", "John Lim", InviteType.FAMILY, "Family", dayAt(0), activeStart, 2).let { invite ->
        invite.copy(
            vehicleRegion = "SG", vehicle = "SMC 1234A", visitorCanEditVehicle = true,
            faceRegistration = true, faceRegistered = true,
            access = listOf(
                AccessEvent("Main Gate", invite.startAt + 5 * MinuteMs, entry = true),
                AccessEvent("Tower A Lift Lobby", invite.startAt + 7 * MinuteMs, entry = true),
                AccessEvent("Level 12 Access Door", invite.startAt + 8 * MinuteMs, entry = true),
            ).filter { it.at <= now },
            createdVehicleRegion = "SG", createdVehicle = "SMC 1234A",
            vehicleRequest = VehicleRequest("SG", "SNE 5678B", "Coming in a different car", invite.startAt - 30 * MinuteMs),
            vehicleChanges = listOf(VehicleChange(VehicleChangeKind.REQUESTED, "SG", "SNE 5678B",
                invite.startAt - 30 * MinuteMs, "Coming in a different car")),
        )
    }
    val sarah = base("VIS-SAMPLE-5", "Sarah Chen", InviteType.FAMILY, "Friend", dayAt(-4), 14 * 60, 4).let { invite ->
        invite.copy(
            createdAt = invite.startAt - 26 * 60 * MinuteMs,
            vehicleRegion = "SG", vehicle = "SKL 2468C", visitorCanEditVehicle = true,
            createdVehicleRegion = "SG", createdVehicle = "SBA 9012Z",
            vehicleChanges = listOf(
                VehicleChange(VehicleChangeKind.REQUESTED, "SG", "SKL 2468C", invite.startAt - 3 * 60 * MinuteMs, "Borrowing my brother's car"),
                VehicleChange(VehicleChangeKind.APPROVED, "SG", "SKL 2468C", invite.startAt - 2 * 60 * MinuteMs),
            ),
        )
    }
    val laterToday = ((nowMinutes + 150) / 30 * 30).coerceAtMost(23 * 60)
    val technician = base("VIS-SAMPLE-4", "AC Technician", InviteType.OTHER, "Technician", dayAt(-3), 10 * 60, 2).let { invite ->
        invite.copy(maxEntries = 1, access = listOf(
            AccessEvent("Main Gate", invite.startAt + 4 * MinuteMs, entry = true),
            AccessEvent("Main Gate", invite.startAt + 95 * MinuteMs, entry = false),
        ))
    }
    return listOf(
        john,
        base("VIS-SAMPLE-2", "Grab Driver", InviteType.CAB, "Pickup", dayAt(0), laterToday, 1),
        base("VIS-SAMPLE-3", "Delivery Person", InviteType.DELIVERY, "GrabFood", dayAt(1), 16 * 60, 2),
        technician,
        sarah,
        base("VIS-SAMPLE-6", "Mei Ling Tan", InviteType.FAMILY, "Guest", dayAt(3), 19 * 60, 3).copy(faceRegistration = true),
    )
}
