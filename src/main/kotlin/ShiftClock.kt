package kz

object ShiftClock {
    const val MINUTE_MS = 500L
    private var startedAt = 0L

    fun start() {
        startedAt = System.currentTimeMillis()
    }

    fun log(message: String) {
        val minutes = (System.currentTimeMillis() - startedAt) / MINUTE_MS
        println("[%02d:%02d] %s".format(8 + minutes / 60, minutes % 60, message))
    }
}
