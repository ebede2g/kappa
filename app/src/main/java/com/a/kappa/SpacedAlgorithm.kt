package com.a.kappa
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import kotlin.math.pow

object SpacedAlgorithm {
    val wsh = 8
    val weh = 22
    val wd = weh - wsh
    val rd = 24 - wd
    val koef = 24.0 / wd

    fun gdl(sp: LocalDateTime, ep: LocalDateTime): List<LocalDateTime> {
        val days = mutableListOf<LocalDateTime>()
        var currentDay = sp
        while (currentDay < ep.plusDays(3)) {
            val endOfDay = currentDay.withHour(weh).withMinute(0).withSecond(0).withNano(0)
            days.add(endOfDay)
            currentDay = currentDay.plusDays(1)
        }
        return days
    }

    fun atwh(dtArr: List<LocalDateTime>): List<LocalDateTime> {
        val now = LocalDateTime.now()
        val result = mutableListOf<LocalDateTime>()
        var sp = dtArr[0]

        var opDtArr = dtArr.toMutableList()

        if (sp.hour >= weh || sp.hour < wsh) {
            println("first element in sleep time")
            var nextMorning = now.withHour(wsh).withMinute(sp.minute).withSecond(sp.second).withNano(0)
            if (sp.hour >= weh) {
                nextMorning = nextMorning.plusDays(1)
            }
            val hoursToAdd = Duration.between(sp, nextMorning)
            println("hrs to add: $hoursToAdd")
            opDtArr = dtArr.map { it.plus(hoursToAdd) }.toMutableList()
            sp = opDtArr[0]
        }

        val dtl = opDtArr.size
        val deltaDays = Duration.between(opDtArr[0], opDtArr[dtl - 1]).toDays()
        val ep = sp.plusDays((deltaDays * koef).toLong())
        println("it will be until: $ep")

        val ans = mutableListOf<LocalDateTime>()
        for (gdlDt in gdl(sp, ep)) {
            val nextTempDtArr = mutableListOf<LocalDateTime>()
            for (dt in opDtArr) {
                if (dt < gdlDt) {
                    ans.add(dt)
                } else {
                    nextTempDtArr.add(dt.plusHours(rd.toLong()))
                }
            }
            opDtArr = nextTempDtArr
        }

        return ans
    }

    fun printDt(dtArr: List<LocalDateTime>) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        for (dt in dtArr) {
            println(dt.format(formatter))
        }
    }

    fun gfdt(n: Int, j: Double): List<LocalDateTime> {
        val now = LocalDateTime.now()
        val dtArr = mutableListOf<LocalDateTime>()
        val jH = j.pow(1.0 / (24 * 60))

        when {
            j == 1.0 -> {
                for (i in 0 until 30) {
                    val ft = now.plusDays((i / koef).toLong()).plusMinutes(30)
                    dtArr.add(ft)
                }
            }

            j > 1.0 -> {
                for (i in 0 until n * 24 * 60) {
                    if (i % (24 * 60) == 0) {
                        val unitDays = jH.pow(i.toDouble())
                        val td = Duration.ofMinutes((30 + unitDays * 24 * 60 - 1440).toLong())
                        val ft = now.plus(td)
                        dtArr.add(ft)
                    }
                }
            }

            else -> {
                dtArr.add(now.plusMinutes(30))
            }
        }

        return dtArr
    }

    fun twlist(n: Int, j: Double): List<LocalDateTime> {
        return atwh(gfdt(n, j))
    }

    fun untilApro(n: Int, j: Double): String {
        val arr = gfdt(n, j)
        val sp = arr.first()
        val epRaw = arr.last()
        val daysSpan = Duration.between(sp, epRaw).toDays()
        val approxEp = sp.plusDays((daysSpan * koef).toLong())
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        return "[Останній таск : ~${approxEp.format(formatter)}]"
    }






}
