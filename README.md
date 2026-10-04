# Диспетчерская 112

Консольное приложение на Kotlin. Ты работаешь диспетчером службы 112: в фоне поступают вызовы
(пожар, ДТП, медицина), ты принимаешь их, назначаешь бригаду, редактируешь и закрываешь.
Бригады едут и работают параллельно в корутинах, в конце смены выводится статистика.

## Запуск

Нужен JDK 25.

```bash
./gradlew run --console=plain -q
```

Или запустить `main()` в `Main.kt` в IntelliJ IDEA.

Команды: `list`, `units`, `accept <№>`, `assign <№>`, `edit <№>`, `close <№>`, `stats`, `help`, `exit`.

## Где требования

- **Переменные, типы, условия, циклы**: `Main.kt` (цикл команд, `when`), `Vehicle.respond`, `generateCalls`
- **List, Set, Map**: список бригад, `calls: Map<Int, Alarm>`, `Set` задействованных бригад в `printStats`
- **map, filter, reduce**: `printStats` и `dispatch` в `EmergencyServiceEmulation.kt`
- **Функции высшего порядка и лямбды**: `printGrouped(..., key)`, `respond(..., report)`, `withAlarm { }`
- **Классы и объекты**: `Vehicle`, `EmergencyServiceEmulation`, `object ShiftClock`, `object EntityRandomizer`
- **Наследование**: `Vehicle` → `FireTruck`, `Ambulance`
- **Интерфейс и полиморфизм**: `Respondable`; каждая бригада по-своему реализует `canHandle` и `work`
- **Data class**: `Alarm`, `Dispatcher`
- **Sealed class**: `Incident` (Fire, Accident, Medical), `DispatchResult` (Available, AllBusy, NoUnits)
- **Suspend-функции и корутины**: `generateCalls`, `assign`, `respond`; `runBlocking` и `launch` в `main`
