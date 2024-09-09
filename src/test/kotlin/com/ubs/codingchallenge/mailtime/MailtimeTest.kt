package com.ubs.codingchallenge.mailtime

import com.ubs.codingchallenge.mailtime.model.DifficultyLevel
import com.ubs.codingchallenge.mailtime.model.MailtimeChecker
import com.ubs.codingchallenge.mailtime.model.OfficeHours
import com.ubs.codingchallenge.mailtime.model.Output
import com.ubs.codingchallenge.mailtime.model.User
import com.ubs.codingchallenge.mailtime.model.calculateResponseRimeWithoutOfficeHours
import com.ubs.codingchallenge.mailtime.model.calculateResponseTime
import com.ubs.codingchallenge.mailtime.model.generateInput
import com.ubs.codingchallenge.mailtime.service.MailtimeSolver
import org.junit.jupiter.api.Disabled
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MailtimeTest {

    @Test
    fun should_calculate_response_time_if_received_during_office_hours_and_responded_on_the_same_day() {
        // Should calculate correctly if dates are on the same day
        val officeHours = OfficeHours(ZoneId.of("Asia/Singapore"), 8, 17);
        val user = User("user", officeHours);

        val timeReceived = ZonedDateTime.of(2024, 1, 8, 12, 0, 0, 0, officeHours.timeZone);
        val timeResponded = ZonedDateTime.of(2024, 1, 8, 12, 1, 0, 0, officeHours.timeZone);

        assertEquals(60, calculateResponseTime(timeReceived, timeResponded, user));
    }

    @Test
    fun should_handle_timezones_correctly() {
        // Should calculate correctly if dates are on the same day
        val singaporeZone = ZoneId.of("Asia/Singapore");
        val tokyoZone = ZoneId.of("Asia/Tokyo"); //1h ahead of Singapore
        val officeHours = OfficeHours(singaporeZone, 8, 17);
        val user = User("user", officeHours);

        // Received at 12:00 tokyo time, responded at 12:00 singapore time, hence one hour later
        val timeReceived = ZonedDateTime.of(2024, 1, 8, 12, 0, 0, 0, tokyoZone);
        val timeResponded = ZonedDateTime.of(2024, 1, 8, 12, 0, 0, 0, singaporeZone);

        assertEquals(3600, calculateResponseTime(timeReceived, timeResponded, user));
    }

    @Test
    fun should_calculate_response_time_if_received_before_office_hours_and_responded_on_the_same_day() {
        // Should calculate correctly if dates are on the same day
        val officeHours = OfficeHours(ZoneId.of("Asia/Singapore"), 8, 17);
        val user = User("user", officeHours);

        val timeReceived = ZonedDateTime.of(2024, 1, 8, 7, 0, 0, 0, officeHours.timeZone);
        // Responded 1 minute after start of office hours
        val timeResponded = ZonedDateTime.of(2024, 1, 8, 8, 1, 0, 0, officeHours.timeZone);

        assertEquals(60, calculateResponseTime(timeReceived, timeResponded, user));
    }

    @Test
    fun should_calculate_response_time_if_received_after_office_hours_and_responded_on_the_next_day() {
        // Should calculate correctly if dates are on the same day
        val officeHours = OfficeHours(ZoneId.of("Asia/Singapore"), 8, 17);
        val user = User("user", officeHours);

        val timeReceived = ZonedDateTime.of(2024, 1, 8, 19, 0, 0, 0, officeHours.timeZone);
        // Responded 1 minute after start of office hours on the next day
        val timeResponded = ZonedDateTime.of(2024, 1, 9, 8, 1, 0, 0, officeHours.timeZone);

        assertEquals(60, calculateResponseTime(timeReceived, timeResponded, user));
    }

    @Test
    fun should_calculate_response_time_if_received_during_office_hours_and_responded_on_the_next_day() {
        // Should calculate correctly if dates are on the same day
        val officeHours = OfficeHours(ZoneId.of("Asia/Singapore"), 8, 17);
        val user = User("user", officeHours);

        // Received with 1 minute left in the day
        val timeReceived = ZonedDateTime.of(2024, 1, 8, 16, 59, 0, 0, officeHours.timeZone);
        // Responded 1 minute after start of office hours on the next day
        val timeResponded = ZonedDateTime.of(2024, 1, 9, 8, 1, 0, 0, officeHours.timeZone);

        assertEquals(120, calculateResponseTime(timeReceived, timeResponded, user));
    }

    @Test
    fun should_calculate_response_time_if_received_during_office_hours_on_a_friday_and_responded_on_the_next_monday() {
        // Should calculate correctly if dates are on the same day
        val officeHours = OfficeHours(ZoneId.of("Asia/Singapore"), 8, 17);
        val user = User("user", officeHours);

        // Received with 1 minute left in the day on 5-01-2024 (friday)
        val timeReceived = ZonedDateTime.of(2024, 1, 5, 16, 59, 0, 0, officeHours.timeZone);
        // Responded 1 minute after start of office hours on the next monday
        val timeResponded = ZonedDateTime.of(2024, 1, 8, 8, 1, 0, 0, officeHours.timeZone);

        assertEquals(120, calculateResponseTime(timeReceived, timeResponded, user));
    }

    @Test
    fun should_calculate_response_time_if_received_during_office_hours_on_a_friday_and_responded_on_the_next_tuesday() {
        // Should calculate correctly if dates are on the same day
        // Office hours are just 1hour long for ease of calculation of the test
        val officeHours = OfficeHours(ZoneId.of("Asia/Singapore"), 8, 9);
        val user = User("user", officeHours);

        // Received with 1 minute left in the day on 5-01-2024 (friday)
        val timeReceived = ZonedDateTime.of(2024, 1, 5, 8, 59, 0, 0, officeHours.timeZone);
        // Responded 1 minute after start of office hours on the next tuesday
        val timeResponded = ZonedDateTime.of(2024, 1, 9, 8, 1, 0, 0, officeHours.timeZone);

        assertEquals(
            3720 /*3600 for the full day worked with no answer, + 2 minutes*/,
            calculateResponseTime(timeReceived, timeResponded, user)
        );
    }

    @Test
    fun should_calculate_response_time_if_received_during_office_hours_on_a_thursday_and_responded_on_the_next_monday() {
        // Should calculate correctly if dates are on the same day
        // Office hours are just 1hour long for ease of calculation of the test
        val officeHours = OfficeHours(ZoneId.of("Asia/Singapore"), 8, 9);
        val user = User("user", officeHours);

        // Received with 1 minute left in the day on 4-01-2024 (thursday)
        val timeReceived = ZonedDateTime.of(2024, 1, 4, 8, 59, 0, 0, officeHours.timeZone);
        // Responded 1 minute after start of office hours on the next monday
        val timeResponded = ZonedDateTime.of(2024, 1, 8, 8, 1, 0, 0, officeHours.timeZone);

        assertEquals(
            3720 /*3600 for the full day worked with no answer, + 2 minutes*/,
            calculateResponseTime(timeReceived, timeResponded, user)
        );
    }

    @Test
    fun should_calculate_response_time_if_received_during_office_hours_on_a_monday_and_responded_on_the_next_wednesday() {
        // Should calculate correctly if dates are on the same day
        // Office hours are just 1hour long for ease of calculation of the test
        val officeHours = OfficeHours(ZoneId.of("Asia/Singapore"), 8, 9);
        val user = User("user", officeHours);

        // Received with 1 minute left in the day on 4-01-2024 (thursday)
        val timeReceived = ZonedDateTime.of(2024, 1, 8, 8, 59, 0, 0, officeHours.timeZone);
        // Responded 1 minute after start of office hours on the next monday
        val timeResponded = ZonedDateTime.of(2024, 1, 10, 8, 1, 0, 0, officeHours.timeZone);

        assertEquals(
            3720 /*3600 for the full day worked with no answer, + 2 minutes*/,
            calculateResponseTime(timeReceived, timeResponded, user)
        );
    }

    @Test
    fun response_time_should_always_be_positive() {
        val user = User("user", OfficeHours(ZoneId.of("America/Los_Angeles"), 7, 16));
        val timeReceived = ZonedDateTime.of(2024, 1, 24, 17, 8, 35, 0, ZoneId.of("Europe/Paris"))
        val timeResponded = ZonedDateTime.of(2024, 1, 25, 7, 41, 1, 0, ZoneId.of("America/Los_Angeles"))

        val responseTime = calculateResponseTime(timeReceived, timeResponded, user)
        assertTrue(responseTime > 0)

    }

    @Test
    fun should_calculate_response_time_without_office_hours_correctly() {
        val officeHours = OfficeHours(ZoneId.of("Asia/Singapore"), 8, 9);

        // Received at 10 AM 8-01-2024
        val timeReceived = ZonedDateTime.of(2024, 1, 8, 10, 0, 0, 0, officeHours.timeZone);
        // Responded at 10:01 AM 10-01-2024
        val timeResponded = ZonedDateTime.of(2024, 1, 10, 10, 1, 0, 0, officeHours.timeZone);

        assertEquals(
            172860, // = 3600 x24 x2 days + 1 minute
            calculateResponseRimeWithoutOfficeHours(timeReceived, timeResponded)
        );
    }

    @Disabled
    @Test
    fun should_calculate_score_without_office_hours_correctly() {
        val input = generateInput(DifficultyLevel.DEFAULT)
        val response: Map<String, Long> = input.emails.map { email -> email.senderUser }
            .toSet()
            .associate { it.name to it.responseTimesWithoutOfficeHours.average().roundToLong() }

        val score = MailtimeChecker.calculateScore(input, Output(response))
        assertEquals(25, score)
    }

    @Disabled
    @Test
    fun should_calculate_score_with_office_hours_correctly() {
        val input = generateInput(DifficultyLevel.DEFAULT)
        val response: Map<String, Long> = input.emails.map { email -> email.senderUser }
            .toSet()
            .associate { it.name to it.responseTimes.average().roundToLong() }

        val score = MailtimeChecker.calculateScore(input, Output(response))
        assertEquals(score, 100)

    }


    @Test
    fun response_times_should_be_smaller_than_response_times_without_office_hours_and_positive() {
        for (i in 0..10000) {
            val input = generateInput(DifficultyLevel.DEFAULT)
            input.users.forEach { user ->
                assertEquals(user.responseTimes.size, user.responseTimesWithoutOfficeHours.size);
                user.responseTimes.forEachIndexed { index, responseTime ->
                    assertTrue {
                        responseTime >= 0 && user.responseTimesWithoutOfficeHours[index] >= 0
                    }

                    assertTrue {
                        responseTime <= user.responseTimesWithoutOfficeHours[index]
                    }
                }
            }
        }
    }

    @Disabled
    @Test
    fun should_be_able_to_solve_challenge() {
        val score = MailtimeSolver().solve();
        assertEquals(100, score)
    }
}