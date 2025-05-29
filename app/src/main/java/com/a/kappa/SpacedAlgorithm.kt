package com.a.kappa

import java.time.LocalDateTime
import java.time.Duration
import kotlin.math.pow

object SpacedAlgorithm {
    private const val WSH = 8         // Початок робочих годин
    private const val WEH = 22        // Кінець робочих годин
    private const val WD = WEH - WSH  // Робочі години на день
    private const val RD = 24 - WD    // Неробочі години на день
    private val KOEF = 24.0 / WD      // Коефіцієнт розтягування часу

    // Генерація списку кінців робочих днів від sp до ep +3 дні
    fun gdl(sp: LocalDateTime, ep: LocalDateTime): List<LocalDateTime> {
        val days = mutableListOf<LocalDateTime>()
        var currentDay = sp
        val limitEp = ep.plusDays(3)

        while (currentDay.isBefore(limitEp)) {
            val endOfDay = currentDay.withHour(WEH).withMinute(0).withSecond(0).withNano(0)
            days.add(endOfDay)
            currentDay = currentDay.plusDays(1)
        }

        return days
    }

    // Коригування списку дат до робочих годин
    fun atwh(dtArr: List<LocalDateTime>): List<LocalDateTime> {
        if (dtArr.isEmpty()) return emptyList()

        var opDtArr = dtArr.toMutableList()
        var sp = opDtArr.first()

        if (sp.hour >= WEH || sp.hour < WSH) {
            var nextMorning = sp.withHour(WSH).withMinute(sp.minute).withSecond(sp.second).withNano(0)
            if (sp.hour >= WEH) {
                nextMorning = nextMorning.plusDays(1)
            }
            val hoursToAdd = Duration.between(sp, nextMorning)
            opDtArr = opDtArr.map { it.plus(hoursToAdd) }.toMutableList()
            sp = opDtArr.first()
        }

        val daysDifference = Duration.between(opDtArr.first(), opDtArr.last()).toDays()
        val scaledDurationInNanos = (daysDifference * KOEF * 24 * 60 * 60 * 1_000_000_000L).toLong()
        val ep = sp.plus(Duration.ofNanos(scaledDurationInNanos))

        val ans = mutableListOf<LocalDateTime>()
        val gdlResult = gdl(sp, ep)

        for (gdlDt in gdlResult) {
            val nextTempDtArr = mutableListOf<LocalDateTime>()
            for (dt in opDtArr) {
                if (dt.isBefore(gdlDt)) {
                    ans.add(dt)
                } else {
                    nextTempDtArr.add(dt.plusHours(RD.toLong()))
                }
            }
            opDtArr = nextTempDtArr
            if (opDtArr.isEmpty()) break
        }

        ans.addAll(opDtArr)
        return ans
    }

    // Генерація майбутніх дат і часу з логарифмічною логікою
    fun gfdt2(n: Int, t: Double): List<LocalDateTime> {
        val now = LocalDateTime.now()

        if (n <= 1 && t / KOEF != 1.0) {
            return listOf(now.plusMinutes(30))
        }

        val j = if (n - 1 == 0) {
            if (t / KOEF == 1.0) 1.0 else Double.NaN
        } else {
            (t / KOEF).pow(1.0 / (n - 1))
        }

        if (j.isNaN()) {
            return listOf(now.plusMinutes(30))
        }

        val dtArr = mutableListOf<LocalDateTime>()

        when {
            kotlin.math.abs(j - 1.0) < 0.000001 -> {
                // Кейс j близький до 1 - рівномірне додавання з інтервалом
                for (i in 0 until n) {
                    val offsetMinutes = ((i / KOEF) * 24 * 60).toLong()
                    dtArr.add(now.plusMinutes(offsetMinutes + 30))
                }
            }
            j > 1.0 -> {
                // j більше 1 — експоненційне збільшення інтервалів
                for (i in 0 until n) {
                    val totalDays = (j.pow(i.toDouble()) - 1) / (j - 1)
                    val totalMinutesOffset = totalDays * 24.0 * 60.0
                    dtArr.add(now.plusMinutes(totalMinutesOffset.toLong() + 30))
                }
            }
            else -> {
                // Обробка j < 1 — зменшення інтервалів, але не менше 30 хв
                for (i in 0 until n) {
                    val interval = ((1.0 - j.pow(i.toDouble())) / (1.0 - j)) * 24.0 * 60.0
                    dtArr.add(now.plusMinutes(interval.toLong() + 30))
                }
            }
        }

        return dtArr
    }

    /**
     * Публічний метод для генерації списку LocalDateTime, скоригованих до робочих годин.
     * @param n Кількість точок
     * @param j Часовий коефіцієнт
     * @return Список дат, відкоригованих по робочому графіку
     */
    fun twilin(n: Int, j: Double): List<LocalDateTime> {
        val generated = gfdt2(n, j)
        return if (generated.isNotEmpty()) atwh(generated) else emptyList()
    }
}
