package com.a.kappa

import java.time.LocalDateTime
import java.time.Duration
import kotlin.math.pow

object SpacedAlgorithm {
    fun twilin(n: Int, j: Double): List<LocalDateTime> {
        val wsh = 8 // початок робочого дня
        val weh = 22 // кінець робочого дня
        val wd = weh - wsh
        val rd = 24 - wd
        val koef = 24.0 / wd

        fun gfdt(n: Int, j: Double): List<LocalDateTime> {
            val now = LocalDateTime.now()
            val dtArr = mutableListOf<LocalDateTime>()
            val jH = Math.pow(j, 1.0 / (24 * 60))

            if (j == 1.0) {
                for (i in 0 until 30) {
                    val ft = now.plusMinutes(30).plusSeconds((i / koef * 86400).toLong())
                    dtArr.add(ft)
                }
            } else if (j > 1.0) {
                for (i in 0 until n * 24 * 60) {
                    if (i % (24 * 60) == 0) {
                        val unitDays = Math.pow(jH, i.toDouble())
                        val totalMinutes = 30 + unitDays * 24 * 60 - 1440
                        val ft = now.plusMinutes(totalMinutes.toLong())
                        dtArr.add(ft)
                    }
                }
            } else {
                dtArr.add(now.plusMinutes(30))
            }

            return dtArr
        }

        fun gdl(sp: LocalDateTime, ep: LocalDateTime): List<LocalDateTime> {
            val days = mutableListOf<LocalDateTime>()
            var currentDay = sp
            while (currentDay.isBefore(ep.plusDays(3))) {
                days.add(currentDay.withHour(weh).withMinute(0).withSecond(0).withNano(0))
                currentDay = currentDay.plusDays(1)
            }
            return days
        }

        fun atwh(dtArr: List<LocalDateTime>): List<LocalDateTime> {
            val now = LocalDateTime.now()
            val opDtArr: MutableList<LocalDateTime>
            var sp = dtArr[0]

            if (sp.hour >= weh || sp.hour < wsh) {
                val nextMorning = now.withHour(wsh).withMinute(sp.minute).withSecond(sp.second)
                    .plusDays(if (sp.hour >= weh) 1 else 0)
                val hoursToAdd = Duration.between(sp, nextMorning)
                opDtArr = dtArr.map { it.plus(hoursToAdd) }.toMutableList()
                sp = opDtArr[0]
            } else {
                opDtArr = dtArr.toMutableList()
            }

            val ep = sp.plusDays(((opDtArr.last().dayOfYear - sp.dayOfYear) * koef).toLong())
            val result = mutableListOf<LocalDateTime>()

            for (gdlDt in gdl(sp, ep)) {
                val nextTempDtArr = mutableListOf<LocalDateTime>()
                for (dt in opDtArr) {
                    if (dt.isBefore(gdlDt)) {
                        result.add(dt)
                    } else {
                        nextTempDtArr.add(dt.plusHours(rd.toLong()))
                    }
                }
                opDtArr.clear()
                opDtArr.addAll(nextTempDtArr)
            }

            return result
        }

        return atwh(gfdt(n, j))
    }


}
