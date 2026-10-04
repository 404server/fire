package kz

import kotlinx.coroutines.delay
import kz.ShiftClock.MINUTE_MS
import kz.ShiftClock.log
import kz.entity.Alarm
import kz.entity.CallStatus
import kz.entity.Dispatcher
import kz.entity.Vehicle
import kotlin.random.Random

sealed class DispatchResult {
    data class Available(val units: List<Vehicle>) : DispatchResult()
    data object AllBusy : DispatchResult()
    data object NoUnits : DispatchResult()
}

class EmergencyServiceEmulation(private val vehicles: List<Vehicle>) {

    val calls = mutableMapOf<Int, Alarm>()

    suspend fun generateCalls() {
        var id = 1
        while (true) {
            val alarm = EntityRandomizer.createAlarm(id++)
            calls[alarm.id] = alarm
            log("НОВЫЙ ВЫЗОВ #${alarm.id}: ${alarm.summary()}")
            delay(Random.nextLong(20, 40) * MINUTE_MS)
        }
    }

    fun update(alarm: Alarm) {
        calls[alarm.id] = alarm
    }

    fun accept(alarm: Alarm, dispatcher: Dispatcher) {
        if (alarm.status != CallStatus.NEW) return println("Вызов #${alarm.id} уже ${alarm.status.label}")
        update(alarm.copy(status = CallStatus.ACCEPTED, acceptedBy = dispatcher))
        log("Вызов #${alarm.id} принят: ${dispatcher.name}")
    }

    fun dispatch(alarm: Alarm): DispatchResult {
        val suitable = vehicles.filter { it.canHandle(alarm.incident) }
        if (suitable.isEmpty()) return DispatchResult.NoUnits

        val free = suitable.filter { !it.isBusy }.sortedBy { it.distanceTo(alarm) }
        return if (free.isEmpty()) DispatchResult.AllBusy else DispatchResult.Available(free)
    }

    suspend fun assign(alarm: Alarm, vehicle: Vehicle) {
        val distance = vehicle.distanceTo(alarm)
        vehicle.currentCall = alarm.id
        update(alarm.copy(status = CallStatus.ASSIGNED, unit = vehicle.callSign, distanceKm = distance))
        vehicle.respond(alarm, distance) { status ->
            calls[alarm.id]?.let { update(it.copy(status = status)) }
        }
    }

    fun close(alarm: Alarm) {
        when (alarm.status) {
            CallStatus.ASSIGNED, CallStatus.ON_SITE -> println("Бригада ещё работает на вызове #${alarm.id}")
            CallStatus.CLOSED -> println("Вызов #${alarm.id} уже закрыт")
            CallStatus.NEW, CallStatus.ACCEPTED, CallStatus.DONE -> {
                update(alarm.copy(status = CallStatus.CLOSED))
                log("Вызов #${alarm.id} закрыт")
            }
        }
    }

    fun printCalls() {
        val active = calls.values.filter { it.status != CallStatus.CLOSED }
        if (active.isEmpty()) return println("Активных вызовов нет")
        for (alarm in active) {
            val unit = alarm.unit?.let { ", бригада $it" } ?: ""
            println("  #${alarm.id} [${alarm.status.label}$unit] ${alarm.summary()}")
        }
    }

    fun printUnits() {
        for (vehicle in vehicles) {
            val state = vehicle.currentCall?.let { "занята, вызов #$it" } ?: "свободна"
            println("  ${vehicle.callSign} (${vehicle.type}): $state")
        }
    }

    fun printStats() {
        val all = calls.values.toList()
        val served = all.filter { it.unit != null }
        val totalKm = served.map { it.distanceKm }.reduceOrNull { sum, km -> sum + km } ?: 0.0
        val usedUnits: Set<String> = served.mapNotNull { it.unit }.toSet()

        println("\n========== Статистика смены ==========")
        println("Вызовов: ${all.size}, закрыто: ${all.count { it.status == CallStatus.CLOSED }}")
        printGrouped("По типам происшествий", all) { it.incident.title }
        printGrouped("По статусам", all) { it.status.label }
        printGrouped("Выезды по бригадам", served) { it.unit }
        println("Общий пробег: %.1f км".format(totalKm))
        println("Бригады без выездов: ${(vehicles.map { it.callSign }.toSet() - usedUnits).joinToString().ifEmpty { "нет" }}")
        println("======================================\n")
    }

    private fun <K> printGrouped(title: String, items: List<Alarm>, key: (Alarm) -> K) {
        println("$title:")
        val counts: Map<K, Int> = items.groupingBy(key).eachCount()
        for ((group, count) in counts) println("  $group: $count")
    }
}
