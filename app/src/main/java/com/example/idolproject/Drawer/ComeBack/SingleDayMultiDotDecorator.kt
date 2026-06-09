package com.example.idolproject.Drawer.ComeBack

import android.graphics.Color
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class SingleDayMultiDotDecorator(
    private val date: CalendarDay,
    private val count: Int,
    private val baseColor: Int = Color.RED
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == date
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(
            MultiDotSpan(
                radius = 5f,
                baseColor = baseColor,
                count = count
            )
        )
    }
}