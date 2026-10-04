package kz

import kz.entity.Alarm
import kz.entity.Incident
import kotlin.random.Random

object EntityRandomizer {

    private val streets = listOf("пр. Абая", "ул. Толе би", "пр. Аль-Фараби", "ул. Жандосова", "пр. Райымбека", "ул. Сатпаева")

    fun createAlarm(id: Int) = Alarm(
        id = id,
        incident = randomIncident(),
        address = "${streets.random()}, ${Random.nextInt(1, 200)}",
        x = Random.nextDouble(0.0, 20.0),
        y = Random.nextDouble(0.0, 20.0),
    )

    private fun randomIncident(): Incident = when (Random.nextInt(3)) {
        0 -> Incident.Fire(floor = Random.nextInt(1, 17))
        1 -> {
            val injured = Random.nextInt(0, 4)
            Incident.Accident(injured, trapped = injured > 0 && Random.nextBoolean())
        }
        else -> Incident.Medical(critical = Random.nextBoolean())
    }
}
