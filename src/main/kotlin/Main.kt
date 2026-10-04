package kz

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kz.ShiftClock.log
import kz.entity.Alarm
import kz.entity.Ambulance
import kz.entity.CallStatus
import kz.entity.Dispatcher
import kz.entity.FireTruck
import kz.entity.Incident
import kz.entity.Vehicle

private enum class Command(val text: String, val description: String) {
    LIST("list", "активные вызовы"),
    UNITS("units", "бригады"),
    ACCEPT("accept", "<№> принять вызов"),
    ASSIGN("assign", "<№> назначить бригаду"),
    EDIT("edit", "<№> изменить вызов"),
    CLOSE("close", "<№> закрыть вызов"),
    STATS("stats", "статистика смены"),
    HELP("help", "список команд"),
    EXIT("exit", "закончить смену"),
}

fun main() = runBlocking {
    val service = EmergencyServiceEmulation(
        listOf(
            FireTruck("АЦ-1", x = 3.0, y = 4.0),
            FireTruck("АЦ-2", x = 15.0, y = 14.0),
            Ambulance("СМП-1", x = 6.0, y = 12.0),
            Ambulance("СМП-2", x = 12.0, y = 5.0),
            Ambulance("СМП-3", x = 17.0, y = 18.0),
        )
    )

    print("Диспетчерская 112. Ваше имя: ")
    val me = Dispatcher(readInput()?.trim()?.ifEmpty { null } ?: "Диспетчер")
    ShiftClock.start()
    log("${me.name} заступил(а) на смену")
    printHelp()
    launch { service.generateCalls() }

    while (true) {
        val parts = (readInput() ?: break).trim().split(Regex("\\s+"))
        val command = Command.entries.find { it.text == parts[0].lowercase() }

        suspend fun withAlarm(action: suspend (Alarm) -> Unit) {
            val alarm = parts.getOrNull(1)?.toIntOrNull()?.let { service.calls[it] }
            if (alarm == null) println("Нет такого вызова. Пример: ${parts[0]} 1") else action(alarm)
        }

        when (command) {
            Command.LIST -> service.printCalls()
            Command.UNITS -> service.printUnits()
            Command.ACCEPT -> withAlarm { service.accept(it, me) }
            Command.ASSIGN -> withAlarm { alarm ->
                val vehicle = chooseVehicle(service, alarm)
                if (vehicle != null) launch { service.assign(alarm, vehicle) }
            }
            Command.EDIT -> withAlarm { edit(service, it) }
            Command.CLOSE -> withAlarm { service.close(it) }
            Command.STATS -> service.printStats()
            Command.HELP -> printHelp()
            Command.EXIT -> break
            null -> if (parts[0].isNotEmpty()) println("Неизвестная команда, help — список команд")
        }
    }

    coroutineContext.cancelChildren()
    log("Смена окончена")
    service.printStats()
}

private fun printHelp() {
    println("Команды:")
    for (command in Command.entries) println("  ${command.text} ${command.description}")
}

private suspend fun readInput(): String? = withContext(Dispatchers.IO) { readlnOrNull() }

private suspend fun ask(question: String, current: String): String {
    print("  $question [$current]: ")
    return readInput()?.trim()?.ifEmpty { null } ?: current
}

private suspend fun askYesNo(question: String, current: Boolean): Boolean =
    ask("$question (да/нет)", if (current) "да" else "нет").lowercase() in setOf("да", "д", "yes", "y")

private suspend fun chooseVehicle(service: EmergencyServiceEmulation, alarm: Alarm): Vehicle? {
    if (alarm.status != CallStatus.ACCEPTED) {
        println("Бригаду можно назначить только на принятый вызов (сейчас: ${alarm.status.label})")
        return null
    }
    return when (val result = service.dispatch(alarm)) {
        DispatchResult.NoUnits -> {
            println("Нет подходящих бригад: передайте вызов в профильную службу и закройте его (close ${alarm.id})")
            null
        }
        DispatchResult.AllBusy -> {
            println("Все подходящие бригады заняты, попробуйте позже")
            null
        }
        is DispatchResult.Available -> {
            result.units.forEachIndexed { i, vehicle ->
                println("  ${i + 1}) ${vehicle.callSign} (${vehicle.type}), %.1f км".format(vehicle.distanceTo(alarm)))
            }
            val vehicle = ask("Номер бригады", "1").toIntOrNull()?.let { result.units.getOrNull(it - 1) }
            if (vehicle == null) println("Нет такой бригады")
            vehicle
        }
    }
}

private suspend fun edit(service: EmergencyServiceEmulation, alarm: Alarm) {
    if (alarm.status == CallStatus.CLOSED) return println("Закрытый вызов изменить нельзя")
    val address = ask("Адрес", alarm.address)
    val incident = when (val i = alarm.incident) {
        is Incident.Fire -> i.copy(floor = ask("Этаж", "${i.floor}").toIntOrNull() ?: i.floor)
        is Incident.Accident -> i.copy(
            injured = ask("Пострадавших", "${i.injured}").toIntOrNull() ?: i.injured,
            trapped = askYesNo("Зажаты в машине", i.trapped),
        )
        is Incident.Medical -> i.copy(critical = askYesNo("Критическое состояние", i.critical))
    }
    val updated = service.calls.getValue(alarm.id).copy(address = address, incident = incident)
    service.update(updated)
    log("Вызов #${alarm.id} изменён: ${updated.summary()}")
}
