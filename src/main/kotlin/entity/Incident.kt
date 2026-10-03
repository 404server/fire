package kz.entity

sealed class Incident(val title: String, val workMinutes: Int) {
    data class Fire(val floor: Int) : Incident("Пожар", 40)
    data class Accident(val injured: Int, val trapped: Boolean) : Incident("ДТП", 25)
    data class Medical(val critical: Boolean) : Incident("Медицина", 15)

    fun details(): String = when (this) {
        is Fire -> "этаж $floor"
        is Accident -> "пострадавших: $injured" + if (trapped) ", зажаты в машине" else ""
        is Medical -> if (critical) "критическое состояние" else "стабильное состояние"
    }
}
