package com.ubs.codingchallenge.mailtime.service

import com.ubs.codingchallenge.mailtime.model.DifficultyLevel
import com.ubs.codingchallenge.mailtime.model.Email
import com.ubs.codingchallenge.mailtime.model.Input
import com.ubs.codingchallenge.mailtime.model.MailtimeChecker
import com.ubs.codingchallenge.mailtime.model.Output
import com.ubs.codingchallenge.mailtime.model.User
import com.ubs.codingchallenge.mailtime.model.generateInput
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

class MailtimeSolver {


    fun solve(): Int {
        val input: Input = generateInput(DifficultyLevel.DEFAULT)
        val output =
            Output(input.users.associate { it.name to responseTimesForUser(it, input.emails).average().roundToLong() });
        return MailtimeChecker.calculateScore(input, output);
    }

    private fun responseTimesForUser(user: User, emails: List<Email>): List<Long> {
        return emails.filter { email -> email.senderUser.name == user.name && email.subject.startsWith("RE:") }
            .map { email -> timeTakenToRespond(findQuestion(email, emails).timeSent, email.timeSent, email.senderUser) }

    }
}

fun findQuestion(response: Email, emails: List<Email>): Email {
    val questionSubject = response.subject.substring(4);
    return emails.find { email -> email.subject == questionSubject }!!
}

fun timeTakenToRespond(questionSentTime: ZonedDateTime, answerSentTime: ZonedDateTime, user: User): Long {
    val zoneIdResponse = user.officeHours.timeZone
    val timeReceived = questionSentTime.withZoneSameInstant(zoneIdResponse);
    val timeAnswered = answerSentTime.withZoneSameInstant(zoneIdResponse);

    if (timeReceived.dayOfWeek == timeAnswered.dayOfWeek) {
        //answered the same day
        val startOfOfficeHours = timeAnswered.withHour(user.officeHours.start).withMinute(0).withSecond(0);
        return if (timeReceived.isBefore(startOfOfficeHours)) {
            ChronoUnit.SECONDS.between(startOfOfficeHours, timeAnswered);
        } else {
            ChronoUnit.SECONDS.between(timeReceived, timeAnswered);
        }
    }
    // Answered on different days
    val startOfOfficeHoursOnDayAnswered =
        timeAnswered.withHour(user.officeHours.start).withMinute(0).withSecond(0);
    val startOfOfficeHoursOnDayReceived =
        timeReceived.withHour(user.officeHours.start).withMinute(0).withSecond(0);
    val endOfOfficeHoursOnDayReceived =
        timeReceived.withHour(user.officeHours.end).withMinute(0).withSecond(0);
    val timeOnDayReceived = if (timeReceived.isBefore(endOfOfficeHoursOnDayReceived)) {
        if (timeReceived.isBefore(startOfOfficeHoursOnDayReceived)) {
            ChronoUnit.SECONDS.between(startOfOfficeHoursOnDayReceived, endOfOfficeHoursOnDayReceived);
        } else {
            ChronoUnit.SECONDS.between(timeReceived, endOfOfficeHoursOnDayReceived);
        }
    } else {
        0
    }
    val timeOnDayResponded = ChronoUnit.SECONDS.between(startOfOfficeHoursOnDayAnswered, timeAnswered);

    val secondsInWorkingDay = (user.officeHours.end - user.officeHours.start) * 3600;
    val fullWorkingDaysBetween = calculateWorkingDaysBetween(timeReceived, timeAnswered);
    return fullWorkingDaysBetween * secondsInWorkingDay + timeOnDayResponded + timeOnDayReceived;
}

fun calculateWorkingDaysBetween(timeReceived: ZonedDateTime, timeAnswered: ZonedDateTime): Int {
    return daysBetween(timeReceived.dayOfWeek, timeAnswered.dayOfWeek);
}

fun daysBetween(dayReceived: DayOfWeek, dayAnswered: DayOfWeek): Int {
    var i = dayReceived;
    var numberOfDays = 0;
    while (i.plus(1) != dayAnswered) {
        i = i.plus(1);
        if (i !== DayOfWeek.SATURDAY && i !== DayOfWeek.SUNDAY) {
            numberOfDays++;
        }
    }
    return numberOfDays;
}