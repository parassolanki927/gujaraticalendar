package com.rahul.gujaraticalendar

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate

class MainActivity : Activity() {

    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    private var dark = false
    private lateinit var content: LinearLayout
    private val today: LocalDate = LocalDate.now()
    private var viewYear = today.year
    private var viewMonth = today.monthValue
    private var selected: LocalDate = today

    // palette
    private var bg = 0; private var surface = 0; private var ink = 0; private var inkSoft = 0
    private val saffron = Color.parseColor("#F57C00")
    private val maroon = Color.parseColor("#B3261E")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        bg = Color.parseColor(if (dark) "#15110D" else "#FFF8F0")
        surface = Color.parseColor(if (dark) "#221C16" else "#FFFFFF")
        ink = Color.parseColor(if (dark) "#F5EDE4" else "#2B1D12")
        inkSoft = Color.parseColor(if (dark) "#B9A998" else "#7A6655")

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(28))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isVerticalScrollBarEnabled = false
            addView(content)
        }
        setContentView(scroll)
        styleBars()
        @Suppress("DEPRECATION")
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        render()
    }

    @Suppress("DEPRECATION")
    private fun styleBars() {
        window.statusBarColor = bg
        window.navigationBarColor = bg
        if (!dark) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    // ---------- helpers ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, r: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }

    private fun ripple(fill: Int, r: Int, rc: Int) =
        RippleDrawable(ColorStateList.valueOf(rc), rounded(fill, r), rounded(Color.WHITE, r))

    private fun tv(text: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun alpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    private fun lp(w: Int = MATCH, h: Int = WRAP, top: Int = 0) = LinearLayout.LayoutParams(w, h).apply { topMargin = dp(top) }

    // ---------- data ----------
    private fun daysFor(date: LocalDate): Triple<List<Panchang.Day>, Int, List<String>> {
        val list = listOf(Panchang.dayInfo(date.minusDays(1)), Panchang.dayInfo(date), Panchang.dayInfo(date.plusDays(1)))
        return Triple(list, 1, Panchang.festivals(list, 1))
    }

    // ---------- UI ----------
    private fun render() {
        content.removeAllViews()
        val days = Panchang.monthDays(viewYear, viewMonth) // index 0 = day before 1st
        val daysInMonth = days.size - 2

        // today card
        val (tl, ti, tfest) = daysFor(today)
        val td = tl[ti]
        content.addView(todayCard(td, tfest))

        // month navigation
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nav.addView(navBtn("‹") { shiftMonth(-1) }, LinearLayout.LayoutParams(dp(44), dp(44)))
        val title = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        title.addView(tv("${Gu.GREG[viewMonth - 1]} ${Gu.num(viewYear)}", 20f, ink, true).apply { gravity = Gravity.CENTER })
        val first = days[1]; val last = days[daysInMonth]
        val sub = if (first.monthIdx == last.monthIdx && first.adhik == last.adhik) Gu.monthName(first)
                  else "${Gu.monthName(first)} – ${Gu.monthName(last)}"
        title.addView(tv("વિક્રમ સંવત ${Gu.num(last.vsYear)} • $sub", 12f, inkSoft).apply { gravity = Gravity.CENTER })
        nav.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        nav.addView(navBtn("›") { shiftMonth(1) }, LinearLayout.LayoutParams(dp(44), dp(44)))
        content.addView(nav, lp(top = 18))

        if (viewYear != today.year || viewMonth != today.monthValue) {
            content.addView(tv("આજે પાછા જાઓ", 13f, saffron, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(4))
                setOnClickListener { viewYear = today.year; viewMonth = today.monthValue; selected = today; render() }
            }, lp())
        }

        // weekday header
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (i in 0..6) head.addView(tv(Gu.WEEK_SHORT[i], 12f, if (i == 0) maroon else inkSoft, true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, WRAP, 1f))
        content.addView(head, lp(top = 10))

        // grid
        val firstDow = LocalDate.of(viewYear, viewMonth, 1).dayOfWeek.value % 7
        var dayNum = 1
        for (row in 0 until 6) {
            if (dayNum > daysInMonth) break
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (col in 0..6) {
                val idx = row * 7 + col
                if (idx < firstDow || dayNum > daysInMonth) {
                    r.addView(View(this), LinearLayout.LayoutParams(0, dp(54), 1f))
                } else {
                    r.addView(cell(days, dayNum, col), LinearLayout.LayoutParams(0, dp(54), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
                    dayNum++
                }
            }
            content.addView(r, lp(top = 0))
        }
        if (selected.year != viewYear || selected.monthValue != viewMonth) selected = LocalDate.of(viewYear, viewMonth, 1)

        // selected day detail
        content.addView(detailCard(days, selected.dayOfMonth), lp(top = 14))

        // festivals this month
        val items = ArrayList<Pair<Int, String>>()
        for (i in 1..daysInMonth) for (f in Panchang.festivals(days, i)) items.add(Pair(i, f))
        if (items.isNotEmpty()) {
            content.addView(tv("આ મહિનાના તહેવારો અને વ્રત", 16f, ink, true), lp(top = 20))
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(surface, 18)
                setPadding(dp(16), dp(8), dp(16), dp(8))
            }
            for ((d, name) in items) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)) }
                val date = LocalDate.of(viewYear, viewMonth, d)
                row.addView(tv("${Gu.num(d)} ${Gu.GREG_SHORT[viewMonth - 1]}", 14f, saffron, true), LinearLayout.LayoutParams(dp(74), WRAP))
                val txt = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                txt.addView(tv(name, 15f, ink, true))
                txt.addView(tv(Gu.WEEK[date.dayOfWeek.value % 7] + " • " + Gu.dateLine(days[d]), 12f, inkSoft))
                row.addView(txt, LinearLayout.LayoutParams(0, WRAP, 1f))
                row.setOnClickListener { selected = date; render() }
                box.addView(row)
            }
            content.addView(box, lp(top = 8))
        }

        content.addView(tv("તિથિ અમદાવાદ ના સૂર્યોદય મુજબ ગણાય છે. કેટલાક તહેવારોમાં પંચાંગ પ્રમાણે ૧ દિવસ નો ફરક હોઈ શકે, તેથી અગત્ય ના કામ માટે સ્થાનિક પંચાંગ સાથે ખાતરી કરો.",
            11f, inkSoft), lp(top = 22))
    }

    private fun navBtn(label: String, onClick: () -> Unit) = tv(label, 26f, saffron, true).apply {
        gravity = Gravity.CENTER
        background = ripple(surface, 22, alpha(saffron, 60))
        setOnClickListener { onClick() }
    }

    private fun shiftMonth(delta: Int) {
        val d = LocalDate.of(viewYear, viewMonth, 1).plusMonths(delta.toLong())
        viewYear = d.year; viewMonth = d.monthValue
        selected = if (viewYear == today.year && viewMonth == today.monthValue) today else d
        render()
    }

    private fun todayCard(d: Panchang.Day, fest: List<String>): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#F57C00"), Color.parseColor("#B3261E"))).apply { cornerRadius = dp(26).toFloat() }
            setPadding(dp(22), dp(18), dp(22), dp(20))
            elevation = dp(4).toFloat()
        }
        val w = Color.WHITE
        card.addView(tv("ગુજરાતી કેલેન્ડર • આજ", 12f, alpha(w, 210), true))
        card.addView(tv("${Gu.WEEK[today.dayOfWeek.value % 7]}, ${Gu.num(today.dayOfMonth)} ${Gu.GREG[today.monthValue - 1]} ${Gu.num(today.year)}", 14f, alpha(w, 230)), lp(top = 4))
        card.addView(tv(Gu.dateLine(d), 30f, w, true), lp(top = 8))
        card.addView(tv("${Gu.tithiName(d)} • વિક્રમ સંવત ${Gu.num(d.vsYear)}", 15f, alpha(w, 235)), lp(top = 2))
        val end = Panchang.jdToIst(Panchang.tithiEndJd(d.sunriseJd, d.tithi))
        card.addView(tv("તિથિ સમાપ્તિ: " + endText(end, today), 13f, alpha(w, 210)), lp(top = 6))
        if (fest.isNotEmpty()) {
            card.addView(tv("✨ " + fest.joinToString("  •  "), 14f, w, true).apply {
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = rounded(alpha(w, 50), 12)
            }, lp(top = 12))
        }
        return card
    }

    private fun endText(end: java.time.LocalDateTime, day: LocalDate): String =
        (if (end.toLocalDate() != day) "${Gu.num(end.dayOfMonth)} ${Gu.GREG_SHORT[end.monthValue - 1]}, " else "") + Gu.time(end)

    private fun cell(days: List<Panchang.Day>, n: Int, col: Int): View {
        val d = days[n]
        val date = LocalDate.of(viewYear, viewMonth, n)
        val isToday = date == today
        val isSel = date == selected
        val fest = Panchang.festivals(days, n)
        val special = d.tithiInPaksha == 15
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val fill = when {
                isToday -> saffron
                isSel -> alpha(saffron, if (dark) 70 else 45)
                special -> alpha(saffron, if (dark) 35 else 22)
                else -> surface
            }
            background = ripple(fill, 14, alpha(saffron, 80))
            isClickable = true
            setOnClickListener { selected = date; render() }
        }
        val numColor = when { isToday -> Color.WHITE; col == 0 -> maroon; else -> ink }
        c.addView(tv(Gu.num(n), 16f, numColor, true).apply { gravity = Gravity.CENTER })
        c.addView(tv(Gu.cell(d), 10f, if (isToday) alpha(Color.WHITE, 230) else if (fest.isNotEmpty()) maroon else inkSoft, fest.isNotEmpty()).apply { gravity = Gravity.CENTER })
        if (fest.isNotEmpty()) {
            c.addView(View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (isToday) Color.WHITE else maroon) } },
                LinearLayout.LayoutParams(dp(5), dp(5)).apply { topMargin = dp(2) })
        }
        return c
    }

    private fun detailCard(days: List<Panchang.Day>, n: Int): View {
        val d = days[n]
        val date = LocalDate.of(viewYear, viewMonth, n)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 20)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            elevation = dp(1).toFloat()
        }
        box.addView(tv("${Gu.WEEK[date.dayOfWeek.value % 7]}, ${Gu.num(n)} ${Gu.GREG[viewMonth - 1]} ${Gu.num(viewYear)}", 13f, inkSoft, true))
        box.addView(tv(Gu.dateLine(d), 22f, ink, true), lp(top = 4))
        box.addView(tv("${Gu.tithiName(d)} • વિક્રમ સંવત ${Gu.num(d.vsYear)}", 14f, saffron, true), lp(top = 2))
        val end = Panchang.jdToIst(Panchang.tithiEndJd(d.sunriseJd, d.tithi))
        box.addView(tv("તિથિ સમાપ્તિ: " + endText(end, date), 13f, inkSoft), lp(top = 6))
        val sr = Panchang.jdToIst(d.sunriseJd)
        box.addView(tv("સૂર્યોદય (અમદાવાદ): " + Gu.time(sr), 13f, inkSoft), lp(top = 2))
        val fest = Panchang.festivals(days, n)
        if (fest.isNotEmpty()) box.addView(tv(fest.joinToString("\n") { "• $it" }, 15f, maroon, true), lp(top = 10))
        return box
    }
}
