package com.example.voicereminder.presentation.calendar.decorators

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import org.threeten.bp.LocalDate

/**
 * Decorator for calendar dates that have events.
 * Adds a dot indicator below dates with scheduled reminders.
 */
class EventDecorator(
    private val dates: Set<LocalDate>,
    private val color: Int
) : DayViewDecorator {

    /**
     * Determines if a given calendar day should be decorated.
     * Returns true if the date has events scheduled.
     */
    override fun shouldDecorate(day: CalendarDay): Boolean {
        val localDate = LocalDate.of(day.year, day.month, day.day)
        return dates.contains(localDate)
    }

    /**
     * Applies decoration to the calendar day by adding a dot span.
     */
    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(8f, color))
    }
}

/**
 * Custom span that draws a dot below the calendar date text.
 */
class DotSpan(
    private val radius: Float,
    private val color: Int
) : LineBackgroundSpan {

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {
        val oldColor = paint.color
        paint.color = color

        // Draw dot centered below the text
        val centerX = (left + right) / 2f
        val dotY = bottom + radius * 2
        canvas.drawCircle(centerX, dotY, radius, paint)

        paint.color = oldColor
    }
}
