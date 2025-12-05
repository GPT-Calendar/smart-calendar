package com.example.voicereminder

import com.example.voicereminder.domain.CommandParser
import com.example.voicereminder.domain.ParseErrorType
import com.example.voicereminder.domain.ParseResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests for CommandParser
 * Tests time format parsing, message extraction, and invalid input handling
 */
class CommandParserTest {

    private lateinit var parser: CommandParser

    @Before
    fun setup() {
        parser = CommandParser()
    }

    // Test 12-hour time format parsing
    @Test
    fun `parseCommand with 2 PM format returns correct time`() {
        val result = parser.parseCommand("remind me at 2 PM to call mom")
        assertNotNull(result)
        assertEquals(14, result!!.scheduledTime.hour)
        assertEquals(0, result.scheduledTime.minute)
        assertEquals("call mom", result.message)
    }

    @Test
    fun `parseCommand with 2 30 PM format returns correct time`() {
        val result = parser.parseCommand("remind me at 2:30 PM to buy groceries")
        assertNotNull(result)
        assertEquals(14, result!!.scheduledTime.hour)
        assertEquals(30, result.scheduledTime.minute)
        assertEquals("buy groceries", result.message)
    }

    @Test
    fun `parseCommand with 12 AM format returns midnight`() {
        val result = parser.parseCommand("remind me at 12 AM to check email")
        assertNotNull(result)
        assertEquals(0, result!!.scheduledTime.hour)
        assertEquals("check email", result.message)
    }

    @Test
    fun `parseCommand with 12 PM format returns noon`() {
        val result = parser.parseCommand("remind me at 12 PM to have lunch")
        assertNotNull(result)
        assertEquals(12, result!!.scheduledTime.hour)
        assertEquals("have lunch", result.message)
    }

    // Test 24-hour time format parsing
    @Test
    fun `parseCommand with 14 00 format returns correct time`() {
        val result = parser.parseCommand("remind me at 14:00 to attend meeting")
        assertNotNull(result)
        assertEquals(14, result!!.scheduledTime.hour)
        assertEquals(0, result.scheduledTime.minute)
        assertEquals("attend meeting", result.message)
    }

    @Test
    fun `parseCommand with 09 30 format returns correct time`() {
        val result = parser.parseCommand("remind me at 09:30 to start work")
        assertNotNull(result)
        assertEquals(9, result!!.scheduledTime.hour)
        assertEquals(30, result.scheduledTime.minute)
        assertEquals("start work", result.message)
    }

    // Test relative time format parsing
    @Test
    fun `parseCommand with in 30 minutes format returns correct time`() {
        val beforeParse = LocalDateTime.now()
        val result = parser.parseCommand("remind me in 30 minutes to take medicine")
        val afterParse = LocalDateTime.now()
        
        assertNotNull(result)
        assertEquals("take medicine", result!!.message)
        
        // Verify the time is approximately 30 minutes from now (with 1 minute tolerance)
        val expectedTime = beforeParse.plusMinutes(30)
        assertTrue(result.scheduledTime.isAfter(expectedTime.minusMinutes(1)))
        assertTrue(result.scheduledTime.isBefore(afterParse.plusMinutes(31)))
    }

    @Test
    fun `parseCommand with in 2 hours format returns correct time`() {
        val beforeParse = LocalDateTime.now()
        val result = parser.parseCommand("remind me in 2 hours to finish report")
        val afterParse = LocalDateTime.now()
        
        assertNotNull(result)
        assertEquals("finish report", result!!.message)
        
        // Verify the time is approximately 2 hours from now
        val expectedTime = beforeParse.plusHours(2)
        assertTrue(result.scheduledTime.isAfter(expectedTime.minusMinutes(1)))
        assertTrue(result.scheduledTime.isBefore(afterParse.plusHours(2).plusMinutes(1)))
    }

    // Test message extraction from various command formats
    @Test
    fun `parseCommand extracts message with remind me prefix`() {
        val result = parser.parseCommand("remind me at 3 PM to walk the dog")
        assertNotNull(result)
        assertEquals("walk the dog", result!!.message)
    }

    @Test
    fun `parseCommand extracts message with reminder prefix`() {
        val result = parser.parseCommand("reminder at 5 PM to close windows")
        assertNotNull(result)
        assertEquals("close windows", result!!.message)
    }

    @Test
    fun `parseCommand extracts message without prefix`() {
        val result = parser.parseCommand("at 6 PM to prepare dinner")
        assertNotNull(result)
        assertEquals("prepare dinner", result!!.message)
    }

    // Test invalid input handling
    @Test
    fun `parseCommand with no time returns null`() {
        val result = parser.parseCommand("remind me to do something")
        assertNull(result)
    }

    @Test
    fun `parseCommand with no message returns null`() {
        val result = parser.parseCommand("remind me at 3 PM")
        assertNull(result)
    }

    @Test
    fun `parseCommand with invalid time format returns null`() {
        val result = parser.parseCommand("remind me at tomorrow to call")
        assertNull(result)
    }

    @Test
    fun `parseCommand with invalid hour returns null`() {
        val result = parser.parseCommand("remind me at 25:00 to sleep")
        assertNull(result)
    }

    @Test
    fun `parseCommand with invalid minute returns null`() {
        val result = parser.parseCommand("remind me at 14:75 to work")
        assertNull(result)
    }

    // Test edge cases
    @Test
    fun `parseCommand schedules for next day if time has passed`() {
        val now = LocalDateTime.now()
        val pastTime = now.minusHours(2)
        val timeStr = String.format("%02d:%02d", pastTime.hour, pastTime.minute)
        
        val result = parser.parseCommand("remind me at $timeStr to test")
        assertNotNull(result)
        
        // Should be scheduled for tomorrow
        assertTrue(result!!.scheduledTime.isAfter(now))
        assertEquals(pastTime.hour, result.scheduledTime.hour)
        assertEquals(pastTime.minute, result.scheduledTime.minute)
    }

    @Test
    fun `parseCommandWithError returns NO_MESSAGE error when message missing`() {
        val result = parser.parseCommandWithError("remind me at 3 PM")
        assertTrue(result is ParseResult.Error)
        val error = result as ParseResult.Error
        assertEquals(ParseErrorType.NO_MESSAGE, error.errorType)
    }

    @Test
    fun `parseCommandWithError returns INVALID_TIME_FORMAT error when time invalid`() {
        val result = parser.parseCommandWithError("remind me to call someone")
        assertTrue(result is ParseResult.Error)
        val error = result as ParseResult.Error
        assertEquals(ParseErrorType.INVALID_TIME_FORMAT, error.errorType)
    }

    @Test
    fun `parseCommand handles case insensitive input`() {
        val result = parser.parseCommand("REMIND ME AT 3 PM TO CALL")
        assertNotNull(result)
        assertEquals("CALL", result!!.message)
    }

    @Test
    fun `parseCommand handles extra whitespace`() {
        val result = parser.parseCommand("  remind me   at  3 PM   to   test  ")
        assertNotNull(result)
        assertEquals("test", result!!.message)
    }

    // Test day of week parsing
    @Test
    fun `parseCommand with on friday at 10 PM schedules for correct day`() {
        val now = LocalDateTime.now()
        val result = parser.parseCommand("remind me to call ahmad on friday at 10 PM")
        
        assertNotNull(result)
        assertEquals("call ahmad on friday", result!!.message)
        assertEquals(22, result.scheduledTime.hour)
        assertEquals(0, result.scheduledTime.minute)
        assertEquals(5, result.scheduledTime.dayOfWeek.value) // Friday = 5
        
        // Should be in the future
        assertTrue(result.scheduledTime.isAfter(now))
    }

    @Test
    fun `parseCommand with on monday schedules for next monday if today is later in week`() {
        val now = LocalDateTime.now()
        val result = parser.parseCommand("remind me on monday at 9 AM to start week")
        
        assertNotNull(result)
        assertEquals("start week", result!!.message)
        assertEquals(9, result.scheduledTime.hour)
        assertEquals(1, result.scheduledTime.dayOfWeek.value) // Monday = 1
        
        // Should be in the future
        assertTrue(result.scheduledTime.isAfter(now))
    }

    @Test
    fun `parseCommand with on saturday at 3 PM schedules correctly`() {
        val result = parser.parseCommand("remind me on saturday at 3 PM to go shopping")
        
        assertNotNull(result)
        assertEquals("go shopping", result!!.message)
        assertEquals(15, result.scheduledTime.hour)
        assertEquals(6, result.scheduledTime.dayOfWeek.value) // Saturday = 6
    }

    @Test
    fun `parseCommand with on sunday at midnight schedules correctly`() {
        val result = parser.parseCommand("remind me on sunday at 12 AM to reset")
        
        assertNotNull(result)
        assertEquals("reset", result!!.message)
        assertEquals(0, result.scheduledTime.hour)
        assertEquals(7, result.scheduledTime.dayOfWeek.value) // Sunday = 7
    }

    @Test
    fun `parseCommand with day name without on prefix works`() {
        val result = parser.parseCommand("remind me friday at 5 PM to finish work")
        
        assertNotNull(result)
        assertEquals("finish work", result!!.message)
        assertEquals(17, result.scheduledTime.hour)
        assertEquals(5, result.scheduledTime.dayOfWeek.value) // Friday = 5
    }

    // Test alarm command parsing
    @Test
    fun `parseCommand with set alarm at 5 AM returns ALARM type`() {
        val result = parser.parseCommand("set alarm at 5 AM")
        
        assertNotNull(result)
        assertEquals(com.example.voicereminder.domain.CommandType.ALARM, result!!.type)
        assertEquals(5, result.scheduledTime.hour)
        assertEquals(0, result.scheduledTime.minute)
        assertEquals("Alarm", result.message)
    }

    @Test
    fun `parseCommand with alarm at 7 30 AM returns ALARM type`() {
        val result = parser.parseCommand("alarm at 7:30 AM")
        
        assertNotNull(result)
        assertEquals(com.example.voicereminder.domain.CommandType.ALARM, result!!.type)
        assertEquals(7, result.scheduledTime.hour)
        assertEquals(30, result.scheduledTime.minute)
    }

    @Test
    fun `parseCommand with set alarm for tomorrow at 6 AM works`() {
        val result = parser.parseCommand("set alarm for tomorrow at 6 AM")
        
        assertNotNull(result)
        assertEquals(com.example.voicereminder.domain.CommandType.ALARM, result!!.type)
        assertEquals(6, result.scheduledTime.hour)
    }

    @Test
    fun `parseCommand with alarm on monday at 8 AM works`() {
        val result = parser.parseCommand("alarm on monday at 8 AM")
        
        assertNotNull(result)
        assertEquals(com.example.voicereminder.domain.CommandType.ALARM, result!!.type)
        assertEquals(8, result.scheduledTime.hour)
        assertEquals(1, result.scheduledTime.dayOfWeek.value) // Monday = 1
    }

    @Test
    fun `parseCommand with reminder command returns REMINDER type`() {
        val result = parser.parseCommand("remind me at 3 PM to call mom")
        
        assertNotNull(result)
        assertEquals(com.example.voicereminder.domain.CommandType.REMINDER, result!!.type)
        assertEquals("call mom", result.message)
    }
}
