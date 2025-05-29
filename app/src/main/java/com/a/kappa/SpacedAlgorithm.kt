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

    // Генерація списку дат від sp до ep
    private fun gdl(sp: LocalDateTime, ep: LocalDateTime): List<LocalDateTime> {
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

    // Коригування дат до робочих годин
    private fun atwh(dtArr: List<LocalDateTime>): List<LocalDateTime> {
        if (dtArr.isEmpty()) return emptyList()

        var opDtArr = dtArr.toMutableList()
        var sp = opDtArr.first()

        if (sp.hour >= WEH || sp.hour < WSH) {
            var nextMorning = sp.withHour(WSH).withMinute(sp.minute).withSecond(sp.second).withNano(0)
            if (sp.hour >= WEH) {
                nextMorning = nextMorning.plusDays(1)
            }
            val hoursToAdd = Duration.between(sp, nextMorning)

            val tempOpDtArr = mutableListOf<LocalDateTime>()
            for (dt in opDtArr) {
                tempOpDtArr.add(dt.plus(hoursToAdd))
            }
            opDtArr = tempOpDtArr
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

    // Генерація майбутніх дат і часу
    private fun gfdt2(n: Int, t: Double): List<LocalDateTime> {
        if (n <= 1 && t / KOEF != 1.0) {
            return listOf(LocalDateTime.now().plusMinutes(30))
        }

        val j = if (n - 1 == 0) {
            if (t / KOEF == 1.0) 1.0 else Double.NaN
        } else {
            (t / KOEF).pow(1.0 / (n - 1))
        }

        val now = LocalDateTime.now()
        val dtArr = mutableListOf<LocalDateTime>()

        if (j.isNaN()) {
            dtArr.add(now.plusMinutes(30))
            return dtArr
        }

        if (kotlin.math.abs(j - 1.0) < 0.000001) {
            for (i in 0 until 30) {
                val daysInNanos = ((i / KOEF) * 24 * 60 * 60 * 1_000_000_000L).toLong()
                val durationFromDays = Duration.ofNanos(daysInNanos)
                val ft = now.plus(durationFromDays).plusMinutes(30)
                dtArr.add(ft)
            }
        } else if (j > 1.0) {
            val jH = j.pow(1.0 / (24 * 60))
            for (i in 0 until (n * 24 * 60)) {
                if (i % (24 * 60) == 0) {
                    val unitDays = jH.pow(i.toDouble())
                    val totalMinutesOffset = 30.0 + unitDays * 24.0 * 60.0 - 1440.0
                    val td = Duration.ofMinutes(totalMinutesOffset.toLong())
                    val ft = now.plus(td)
                    dtArr.add(ft)
                }
            }
        } else {
            dtArr.add(now.plusMinutes(30))
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
