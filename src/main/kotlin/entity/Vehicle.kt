package kz.entity

import kotlinx.coroutines.delay
import kz.ShiftClock.MINUTE_MS
import kz.ShiftClock.log
import kotlin.math.hypot
import kotlin.math.roundToInt

interface Respondable {
    fun canHandle(incident: Incident): Boolean
    suspend fun respond(alarm: Alarm, distanceKm: Double, report: (CallStatus) -> Unit)
}

abstract class Vehicle(val callSign: String, val x: Double, val y: Double, val speedKmh: Int) : Respondable {
    abstract val type: String
    var currentCall: Int? = null
    val isBusy: Boolean get() = currentCall != null

    fun distanceTo(alarm: Alarm) = hypot(x - alarm.x, y - alarm.y)

    override suspend fun respond(alarm: Alarm, distanceKm: Double, report: (CallStatus) -> Unit) {
        val travelMin = (distanceKm / speedKmh * 60).roundToInt().coerceAtLeast(1)
        log("$callSign выехал на вызов #${alarm.id}: ${"%.1f".format(distanceKm)} км, ~$travelMin мин")
        delay(travelMin * MINUTE_MS)
        report(CallStatus.ON_SITE)
        work(alarm)
        currentCall = null
        report(CallStatus.DONE)
        log("$callSign свободен. Вызов #${alarm.id}: работа завершена, можно закрывать (close ${alarm.id})")
    }

    protected abstract suspend fun work(alarm: Alarm)
}

class FireTruck(callSign: String, x: Double, y: Double) : Vehicle(callSign, x, y, speedKmh = 40) {
    override val type = "пожарная"

    override fun canHandle(incident: Incident) = when (incident) {
        is Incident.Fire -> true
        is Incident.Accident -> incident.trapped
        is Incident.Medical -> false
    }

    override suspend fun work(alarm: Alarm) {
        val action = if (alarm.incident is Incident.Fire) "тушение пожара" else "деблокирование пострадавших"
        log("$callSign на месте (#${alarm.id}): $action")
        delay(alarm.incident.workMinutes * MINUTE_MS)
    }
}

class Ambulance(callSign: String, x: Double, y: Double) : Vehicle(callSign, x, y, speedKmh = 60) {
    override val type = "скорая"

    override fun canHandle(incident: Incident) = when (incident) {
        is Incident.Medical -> true
        is Incident.Accident -> incident.injured > 0
        is Incident.Fire -> false
    }

    override suspend fun work(alarm: Alarm) {
        log("$callSign на месте (#${alarm.id}): оказание помощи")
        delay(alarm.incident.workMinutes * MINUTE_MS)
        val incident = alarm.incident
        if (incident is Incident.Medical && incident.critical) {
            log("$callSign: госпитализация пациента (#${alarm.id})")
            delay(20 * MINUTE_MS)
        }
    }
}
