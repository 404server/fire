package kz.entity

enum class CallStatus(val label: String) {
    NEW("новый"),
    ACCEPTED("принят"),
    ASSIGNED("бригада в пути"),
    ON_SITE("бригада на месте"),
    DONE("работа завершена"),
    CLOSED("закрыт"),
}

data class Alarm(
    val id: Int,
    val incident: Incident,
    val address: String,
    val x: Double,
    val y: Double,
    val status: CallStatus = CallStatus.NEW,
    val acceptedBy: Dispatcher? = null,
    val unit: String? = null,
    val distanceKm: Double = 0.0,
) {
    fun summary() = "${incident.title} (${incident.details()}), $address"
}
