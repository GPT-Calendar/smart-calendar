package com.example.voicereminder.presentation.calendar.decorators

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.LineBackgroundSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import org.threeten.bp.LocalDate

/**
 * Decorator for calendar dates with multiple events.
 * Shows different visual indicators based on the number of events:
 * - 1-2 events: Small single dot (8dp radius)
 * - 3-5 events: Larger single dot (12dp radius)
 * - 5+ events: Background highlight with three small dots
 * 
 * Requirements: 6.1, 6.2, 6.3
 */
class MultiEventDecorator(
    private val dateCounts: Map<LocalDate, Int>,
    private val color: Int,
    private val highlightColor: Int
) : DayViewDecorator {

    /**
     * Determines if a given calendar day should be decorated.
     * Returns true if the date has events scheduled.
     */
    override fun shouldDecorate(day: CalendarDay): Boolean {
        val localDate = LocalDate.of(day.year, day.month, day.day)
        return dateCounts.containsKey(localDate) && dateCounts[localDate]!! > 0
    }

    /**
     * Applies decoration based on the number of events.
     * Different visual styles for different event counts.
     */
    override fun decorate(view: DayViewFacade) {
        // Note: We can't access the specific CalendarDay in decorate()
        // So we create separate decorators for each event count range
        // This decorator is meant to be used with filtered date sets
    }
}

/**
 * Decorator for dates with 1-2 events.
 * Shows a small dot indicator.
 */
class FewEventsDecorator(
    private val dates: Set<LocalDate>,
    private val color: Int
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        val localDate = LocalDate.of(day.year, day.month, day.day)
        return dates.contains(localDate)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(8f, color))
    }
}

/**
 * Decorator for dates with 3-5 events.
 * Shows a larger dot indicator.
 */
class ModerateEventsDecorator(
    private val dates: Set<LocalDate>,
    private val color: Int
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        val localDate = LocalDate.of(day.year, day.month, day.day)
        return dates.contains(localDate)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(12f, color))
    }
}

/**
 * Decorator for dates with 5+ events.
 * Shows a background highlight with multiple small dots.
 */
class ManyEventsDecorator(
    private val dates: Set<LocalDate>,
    private val backgroundColor: Int,
    private val dotColor: Int
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        val localDate = LocalDate.of(day.year, day.month, day.day)
        return dates.contains(localDate)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(BackgroundHighlightSpan(backgroundColor))
        view.addSpan(MultipleDotSpan(5f, dotColor))
    }
}

/**
 * Span that adds a circular background highlight to the calendar date.
 */
class BackgroundHighlightSpan(
    private val backgroundColor: Int
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
        val oldStyle = paint.style
        
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        
        // Draw circular background behind the date number
        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val radius = Math.min(right - left, bottom - top) / 2.2f
        
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        paint.color = oldColor
        paint.style = oldStyle
    }
}

/**
 * Span that draws three small dots below the date to indicate many events.
 */
class MultipleDotSpan(
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
        val oldStyle = paint.style
        
        paint.color = color
        paint.style = Paint.Style.FILL
        
        // Draw three small dots below the date
        val centerX = (left + right) / 2f
        val dotY = bottom + radius * 3
        val spacing = radius * 3f
        
        // Left dot
        canvas.drawCircle(centerX - spacing, dotY, radius, paint)
        // Center dot
        canvas.drawCircle(centerX, dotY, radius, paint)
        // Right dot
        canvas.drawCircle(centerX + spacing, dotY, radius, paint)
        
        paint.color = oldColor
        paint.style = oldStyle
    }
}
