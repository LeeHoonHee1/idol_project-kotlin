package com.example.idolproject.Drawer.Group

import com.example.idolproject.Calendar.MultiDotSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class GroupDotDecorator(
    private val date: CalendarDay,
    private val colors: List<Int>
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay?): Boolean {
        return day == date
    }

    override fun decorate(view: DayViewFacade?) {
        if (colors.isNotEmpty()) {
            view?.addSpan(MultiDotSpan(6f, colors))
        }
    }
}