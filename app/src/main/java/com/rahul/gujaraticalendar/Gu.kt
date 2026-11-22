package com.rahul.gujaraticalendar

import java.time.LocalDateTime

object Gu {
    val MONTHS = arrayOf("કારતક", "માગશર", "પોષ", "મહા", "ફાગણ", "ચૈત્ર", "વૈશાખ", "જેઠ", "અષાઢ", "શ્રાવણ", "ભાદરવો", "આસો")
    val GREG = arrayOf("જાન્યુઆરી", "ફેબ્રુઆરી", "માર્ચ", "એપ્રિલ", "મે", "જૂન", "જુલાઈ", "ઑગસ્ટ", "સપ્ટેમ્બર", "ઑક્ટોબર", "નવેમ્બર", "ડિસેમ્બર")
    val GREG_SHORT = arrayOf("જાન્યુ", "ફેબ્રુ", "માર્ચ", "એપ્રિલ", "મે", "જૂન", "જુલાઈ", "ઑગ", "સપ્ટે", "ઑક્ટો", "નવે", "ડિસે")
    val WEEK = arrayOf("રવિવાર", "સોમવાર", "મંગળવાર", "બુધવાર", "ગુરુવાર", "શુક્રવાર", "શનિવાર")
    val WEEK_SHORT = arrayOf("રવિ", "સોમ", "મંગળ", "બુધ", "ગુરુ", "શુક્ર", "શનિ")
    private val TITHI = arrayOf("એકમ", "બીજ", "ત્રીજ", "ચોથ", "પાંચમ", "છઠ", "સાતમ", "આઠમ", "નોમ", "દશમ", "અગિયારસ", "બારસ", "તેરસ", "ચૌદસ")

    fun num(n: Int): String = n.toString().map { if (it in '0'..'9') '૦' + (it - '0') else it }.joinToString("")

    fun tithiName(d: Panchang.Day): String =
        if (d.tithiInPaksha == 15) (if (d.paksha == 0) "પૂનમ" else "અમાસ") else TITHI[d.tithiInPaksha - 1]

    fun paksha(d: Panchang.Day) = if (d.paksha == 0) "સુદ" else "વદ"
    fun monthName(d: Panchang.Day) = (if (d.adhik) "અધિક " else "") + MONTHS[d.monthIdx]
    fun dateLine(d: Panchang.Day) = "${monthName(d)} ${paksha(d)} ${num(d.tithiInPaksha)}"

    /** Short label for the calendar cell, e.g. સુ૫, વ૧૨, પૂનમ, અમાસ */
    fun cell(d: Panchang.Day): String =
        if (d.tithiInPaksha == 15) tithiName(d) else (if (d.paksha == 0) "સુ" else "વ") + num(d.tithiInPaksha)

    fun time(dt: LocalDateTime): String {
        val h = dt.hour
        val period = when (h) { in 4..11 -> "સવારે"; in 12..15 -> "બપોરે"; in 16..19 -> "સાંજે"; else -> "રાત્રે" }
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$period ${num(h12)}:${num(dt.minute / 10)}${num(dt.minute % 10)}"
    }
}
