package com.rahul.gujaraticalendar

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.*

/**
 * Offline Gujarati (Vikram Samvat, Kartakadi, Amanta) panchang.
 * Astronomy: Meeus "Astronomical Algorithms" (Moon ch.47, Sun ch.25). Accuracy ~1-2 minutes.
 * Day tithi = tithi prevailing at local sunrise (Ahmedabad).
 */
object Panchang {
    const val LAT = 23.0225
    const val LON = 72.5714
    private const val RAD = PI / 180.0
    private const val SYN_RATE = 12.1907 // mean elongation deg/day

    data class Day(
        val date: LocalDate,
        val tithi: Int,        // 1..30
        val monthIdx: Int,     // 0=Kartak .. 11=Aso
        val adhik: Boolean,
        val vsYear: Int,
        val sunriseJd: Double
    ) {
        val paksha get() = if (tithi <= 15) 0 else 1   // 0 = sud, 1 = vad
        val tithiInPaksha get() = if (tithi <= 15) tithi else tithi - 15
    }

    // ---------- math ----------
    private fun norm360(x: Double) = x - floor(x / 360.0) * 360.0
    private fun normSigned(x: Double) = norm360(x + 180.0) - 180.0
    private fun centuries(jdUt: Double) = (jdUt - 2451545.0 + 69.0 / 86400.0) / 36525.0

    private val MOON = arrayOf(
        intArrayOf(0, 0, 1, 0, 6288774), intArrayOf(2, 0, -1, 0, 1274027), intArrayOf(2, 0, 0, 0, 658314),
        intArrayOf(0, 0, 2, 0, 213618), intArrayOf(0, 1, 0, 0, -185116), intArrayOf(0, 0, 0, 2, -114332),
        intArrayOf(2, 0, -2, 0, 58793), intArrayOf(2, -1, -1, 0, 57066), intArrayOf(2, 0, 1, 0, 53322),
        intArrayOf(2, -1, 0, 0, 45758), intArrayOf(0, 1, -1, 0, -40923), intArrayOf(1, 0, 0, 0, -34720),
        intArrayOf(0, 1, 1, 0, -30383), intArrayOf(2, 0, 0, -2, 15327), intArrayOf(0, 0, 1, 2, -12528),
        intArrayOf(0, 0, 1, -2, 10980), intArrayOf(4, 0, -1, 0, 10675), intArrayOf(0, 0, 3, 0, 10034),
        intArrayOf(4, 0, -2, 0, 8548), intArrayOf(2, 1, -1, 0, -7888), intArrayOf(2, 1, 0, 0, -6766),
        intArrayOf(1, 0, -1, 0, -5163), intArrayOf(1, 1, 0, 0, 4987), intArrayOf(2, -1, 1, 0, 4036),
        intArrayOf(2, 0, 2, 0, 3994), intArrayOf(4, 0, 0, 0, 3861), intArrayOf(2, 0, -3, 0, 3665),
        intArrayOf(0, 1, -2, 0, -2689), intArrayOf(2, 0, -1, 2, -2602), intArrayOf(2, -1, -2, 0, 2390),
        intArrayOf(1, 0, 1, 0, -2348), intArrayOf(2, -2, 0, 0, 2236), intArrayOf(0, 1, 2, 0, -2120),
        intArrayOf(0, 2, 0, 0, -2069), intArrayOf(2, -2, -1, 0, 2048), intArrayOf(2, 0, 1, -2, -1773),
        intArrayOf(2, 0, 0, 2, -1595), intArrayOf(4, -1, -1, 0, 1215), intArrayOf(0, 0, 2, 2, -1110),
        intArrayOf(3, 0, -1, 0, -892), intArrayOf(2, 1, 1, 0, -810), intArrayOf(4, -1, -2, 0, 759),
        intArrayOf(0, 2, -1, 0, -713), intArrayOf(2, 2, -1, 0, -700), intArrayOf(2, 1, -2, 0, 691),
        intArrayOf(2, -1, 0, -2, 596), intArrayOf(4, 0, 1, 0, 549), intArrayOf(0, 0, 4, 0, 537),
        intArrayOf(4, -1, 0, 0, 520), intArrayOf(1, 0, -2, 0, -487), intArrayOf(2, 1, 0, -2, -399),
        intArrayOf(0, 0, 2, -2, -381), intArrayOf(1, 1, 1, 0, 351), intArrayOf(3, 0, -2, 0, -340),
        intArrayOf(4, 0, -3, 0, 330), intArrayOf(2, -1, 2, 0, 327), intArrayOf(0, 2, 1, 0, -323),
        intArrayOf(1, 1, -1, 0, 299), intArrayOf(2, 0, 3, 0, 294)
    )

    fun moonLon(jd: Double): Double {
        val t = centuries(jd)
        val lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t * t + t * t * t / 538841.0 - t * t * t * t / 65194000.0
        val d = (297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868.0 - t * t * t * t / 113065000.0) * RAD
        val m = (357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000.0) * RAD
        val mp = (134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699.0 - t * t * t * t / 14712000.0) * RAD
        val f = (93.2720950 + 483202.0175233 * t - 0.0036539 * t * t - t * t * t / 3526000.0 + t * t * t * t / 863310000.0) * RAD
        val e = 1.0 - 0.002516 * t - 0.0000074 * t * t
        var sum = 0.0
        for (r in MOON) {
            val arg = r[0] * d + r[1] * m + r[2] * mp + r[3] * f
            val ef = when (abs(r[1])) { 1 -> e; 2 -> e * e; else -> 1.0 }
            sum += r[4] * ef * sin(arg)
        }
        val a1 = (119.75 + 131.849 * t) * RAD
        val a2 = (53.09 + 479264.290 * t) * RAD
        sum += 3958.0 * sin(a1) + 1962.0 * sin((lp * RAD) - f) + 318.0 * sin(a2)
        return norm360(lp + sum / 1000000.0)
    }

    fun sunLon(jd: Double): Double {
        val t = centuries(jd)
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = (357.52911 + 35999.05029 * t - 0.0001537 * t * t) * RAD
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(m) +
            (0.019993 - 0.000101 * t) * sin(2 * m) + 0.000289 * sin(3 * m)
        return norm360(l0 + c - 0.00569)
    }

    private fun siderealSun(jd: Double): Double {
        val ayanamsa = 23.85306 + (jd - 2451545.0) / 365.25 * 0.013969 // Lahiri
        return norm360(sunLon(jd) - ayanamsa)
    }

    fun elong(jd: Double) = norm360(moonLon(jd) - sunLon(jd))

    // ---------- time ----------
    fun jdToIst(jd: Double): LocalDateTime {
        val sec = Math.round((jd - 2440587.5) * 86400.0) + 19800L
        return LocalDateTime.ofEpochSecond(sec, 0, ZoneOffset.UTC)
    }

    /** Returns sunrise (isRise=true) or sunset JD (UT) for the local date. */
    fun sunTimeJd(date: LocalDate, isRise: Boolean): Double {
        val jd0 = date.toEpochDay() + 2440587.5
        var ut = 1.0
        repeat(4) {
            val jd = jd0 + ut / 24.0
            val t = centuries(jd)
            val l0 = norm360(280.46646 + 36000.76983 * t)
            val lam = sunLon(jd)
            val eps = (23.439291 - 0.0130042 * t) * RAD
            val alpha = atan2(cos(eps) * sin(lam * RAD), cos(lam * RAD)) / RAD
            val decl = asin(sin(eps) * sin(lam * RAD))
            val eot = 4.0 * normSigned(l0 - alpha)
            val cosH = (sin(-0.833 * RAD) - sin(LAT * RAD) * sin(decl)) / (cos(LAT * RAD) * cos(decl))
            val h0 = acos(cosH.coerceIn(-1.0, 1.0)) / RAD
            ut = 12.0 - LON / 15.0 - eot / 60.0 + (if (isRise) -h0 else h0) / 15.0
        }
        return jd0 + ut / 24.0
    }

    fun sunriseJd(date: LocalDate) = sunTimeJd(date, true)

    fun tithiAt(jd: Double) = floor(elong(jd) / 12.0).toInt() + 1

    /** Reference instants: 0 sunrise, 1 madhyahna (midday), 2 pradosh (sunset+1h), 3 madhyaratri (midnight). */
    fun refJd(date: LocalDate, kind: Int): Double = when (kind) {
        1 -> { val a = sunTimeJd(date, true); val b = sunTimeJd(date, false); (a + b) / 2.0 }
        2 -> sunTimeJd(date, false) + 1.0 / 24.0
        4 -> { val a = sunTimeJd(date, true); val b = sunTimeJd(date, false); a + 0.7 * (b - a) }
        3 -> date.plusDays(1).toEpochDay() + 2440587.5 - 5.5 / 24.0
        else -> sunTimeJd(date, true)
    }

    private fun newMoonNear(guess: Double): Double {
        var jd = guess
        repeat(8) { jd -= normSigned(elong(jd)) / SYN_RATE }
        return jd
    }

    /** JD (UT) at which the given absolute tithi (1..30) ends, for the tithi prevailing at sunrise sr. */
    fun tithiEndJd(sr: Double, tithi: Int): Double {
        val target = 12.0 * tithi
        var jd = sr + (target - elong(sr)) / SYN_RATE
        repeat(8) { jd -= normSigned(elong(jd) - target) / SYN_RATE }
        return jd
    }

    // ---------- day info ----------
    fun dayInfo(date: LocalDate): Day {
        val sr = sunriseJd(date)
        val e = elong(sr)
        val tithi = floor(e / 12.0).toInt() + 1
        var prev = newMoonNear(sr - e / SYN_RATE)
        if (prev > sr) prev = newMoonNear(prev - 29.53)
        var next = newMoonNear(prev + 29.530588)
        if (next <= sr) next = newMoonNear(next + 29.53)
        val r0 = floor(siderealSun(prev) / 30.0).toInt()
        val r1 = floor(siderealSun(next) / 30.0).toInt()
        val adhik = r0 == r1
        val k = (r0 + 1) % 12          // 0 = Chaitra order
        val g = (k + 5) % 12           // 0 = Kartak order
        val s = jdToIst(prev).toLocalDate()
        val vs = s.year + if (g <= 2 && s.monthValue >= 10) 57 else 56
        return Day(date, tithi, g, adhik, vs, sr)
    }

    // ---------- festivals ----------
    private class Rule(val month: Int, val paksha: Int, val tp: Int, val name: String) {
        val abs get() = if (paksha == 0) tp else 15 + tp
    }

    private val RULES = listOf(
        Rule(0, 0, 1, "બેસતું વર્ષ (નૂતન વર્ષ)"), Rule(0, 0, 2, "ભાઈબીજ"), Rule(0, 0, 5, "લાભ પાંચમ"),
        Rule(0, 0, 11, "દેવઉઠી એકાદશી"), Rule(0, 0, 12, "તુલસી વિવાહ"), Rule(0, 0, 15, "દેવ દિવાળી (કારતક પૂર્ણિમા)"),
        Rule(1, 0, 11, "મોક્ષદા એકાદશી (ગીતા જયંતી)"), Rule(1, 0, 15, "દત્ત જયંતી"),
        Rule(3, 0, 5, "વસંત પંચમી"), Rule(3, 0, 15, "મહા પૂનમ"), Rule(3, 1, 14, "મહાશિવરાત્રિ"),
        Rule(4, 0, 15, "હોળી (હોલિકા દહન)"), Rule(4, 1, 1, "ધુળેટી"),
        Rule(5, 0, 1, "ચૈત્રી નવરાત્રિ આરંભ"), Rule(5, 0, 9, "રામ નવમી"), Rule(5, 0, 13, "મહાવીર જયંતી"),
        Rule(5, 0, 15, "હનુમાન જયંતી"),
        Rule(6, 0, 3, "અખાત્રીજ"), Rule(6, 0, 14, "નૃસિંહ જયંતી"), Rule(6, 0, 15, "બુદ્ધ પૂર્ણિમા"),
        Rule(7, 0, 10, "ગંગા દશેરા"), Rule(7, 0, 11, "નિર્જળા એકાદશી"), Rule(7, 0, 15, "વટ સાવિત્રી પૂનમ"),
        Rule(8, 0, 2, "રથયાત્રા"), Rule(8, 0, 11, "દેવશયની એકાદશી"), Rule(8, 0, 15, "ગુરુ પૂર્ણિમા"),
        Rule(8, 1, 15, "દિવાસો (દીપ અમાસ)"),
        Rule(9, 0, 15, "રક્ષાબંધન / બળેવ"), Rule(9, 1, 5, "નાગ પાંચમ"), Rule(9, 1, 7, "શીતળા સાતમ"),
        Rule(9, 1, 8, "જન્માષ્ટમી"),
        Rule(10, 0, 4, "ગણેશ ચતુર્થી"), Rule(10, 0, 5, "ઋષિ પાંચમ"), Rule(10, 0, 11, "ડોલ અગિયારસ"),
        Rule(10, 0, 14, "અનંત ચતુર્દશી"), Rule(10, 0, 15, "ભાદરવી પૂનમ"), Rule(10, 1, 15, "સર્વપિતૃ અમાસ"),
        Rule(11, 0, 1, "નવરાત્રિ આરંભ"), Rule(11, 0, 8, "દુર્ગાષ્ટમી"), Rule(11, 0, 9, "મહાનવમી"),
        Rule(11, 0, 10, "દશેરા (વિજયાદશમી)"), Rule(11, 0, 15, "શરદ પૂનમ"), Rule(11, 1, 12, "વાઘ બારસ"),
        Rule(11, 1, 13, "ધનતેરસ"), Rule(11, 1, 14, "કાળી ચૌદસ"), Rule(11, 1, 15, "દિવાળી")
    )

    private val FIXED = mapOf(
        Pair(1, 14) to "ઉત્તરાયણ (મકરસંક્રાંતિ)", Pair(1, 15) to "વાસી ઉત્તરાયણ",
        Pair(1, 26) to "પ્રજાસત્તાક દિવસ", Pair(5, 1) to "ગુજરાત સ્થાપના દિવસ",
        Pair(8, 15) to "સ્વતંત્રતા દિવસ", Pair(10, 2) to "ગાંધી જયંતી", Pair(10, 31) to "સરદાર પટેલ જયંતી"
    )

    // Reference time of day for festivals that depend on it (default = sunrise).
    // 1 = midday, 2 = pradosh (just after sunset), 3 = midnight, 4 = aparahna (afternoon)
    private val REF = mapOf(
        Triple(10, 0, 4) to 1, Triple(5, 0, 9) to 1, Triple(11, 0, 10) to 4, Triple(10, 1, 15) to 1, Triple(6, 0, 3) to 1,
        Triple(11, 1, 15) to 2, Triple(11, 1, 13) to 2, Triple(11, 1, 12) to 2,
        Triple(11, 1, 14) to 2, Triple(11, 0, 15) to 2, Triple(0, 0, 15) to 2, Triple(0, 0, 12) to 2,
        Triple(1, 0, 15) to 2, Triple(5, 0, 15) to 2, Triple(6, 0, 14) to 2, Triple(8, 1, 15) to 2,
        Triple(9, 1, 8) to 3, Triple(3, 1, 14) to 3
    )

    private fun refTithi(date: LocalDate, kind: Int) = tithiAt(refJd(date, kind))

    /** Festival names for days[i]; needs days[i-1] and days[i+1] (so i must be 1..size-2). */
    fun festivals(days: List<Day>, i: Int): List<String> {
        val out = ArrayList<String>()
        val d = days[i]
        FIXED[Pair(d.date.monthValue, d.date.dayOfMonth)]?.let { out.add(it) }
        if (i <= 0 || i >= days.size - 1) return out
        val p = days[i - 1]
        val n = days[i + 1]
        val cache = HashMap<Pair<LocalDate, Int>, Int>()
        fun rt(day: Day, kind: Int) = cache.getOrPut(Pair(day.date, kind)) { refTithi(day.date, kind) }
        for (r in RULES) {
            val t = r.abs
            val kind = REF[Triple(r.month, r.paksha, r.tp)] ?: 0
            val cur = rt(d, kind)
            val hit = if (cur == t && rt(p, kind) != t) {
                !d.adhik && d.monthIdx == r.month
            } else {
                // tithi t never prevails at the reference time (kshaya): give it to the day holding the previous tithi
                val prevT = if (t == 1) 30 else t - 1
                val nextT = if (t == 30) 1 else t + 1
                if (cur == prevT && rt(n, kind) == nextT) {
                    val ref = if (t == 1) n else d
                    !ref.adhik && ref.monthIdx == r.month
                } else false
            }
            if (hit) out.add(r.name)
        }
        if (out.none { it.contains("એકાદશી") } && d.tithiInPaksha == 11 && p.tithi != d.tithi) out.add("એકાદશી")
        if (d.tithi == 15 && p.tithi != 15 && out.none { it.contains("પૂનમ") || it.contains("પૂર્ણિમા") || it.contains("હોળી") || it.contains("જયંતી") || it.contains("બળેવ") }) out.add("પૂનમ")
        if (d.tithi == 30 && p.tithi != 30 && out.none { it.contains("અમાસ") || it.contains("દિવાળી") }) out.add("અમાસ")
        return out
    }

    fun monthDays(year: Int, month: Int): List<Day> {
        val first = LocalDate.of(year, month, 1)
        val last = first.plusMonths(1).minusDays(1)
        val list = ArrayList<Day>()
        var d = first.minusDays(1)
        while (!d.isAfter(last.plusDays(1))) {
            list.add(dayInfo(d))
            d = d.plusDays(1)
        }
        return list // index 0 = day before first; last = day after last
    }
}
