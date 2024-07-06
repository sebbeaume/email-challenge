package org.example

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Random

fun main() {
    generateInput().forEach(::println)
}

fun generateInput(): List<Email> =
    generateSequence { User(getRandomString(5), officeHours.random()) }
        .take(10)
        .toList()
        .let { users ->
            generateSequence { users.shuffled().take(Random().nextInt(2, 5)) }
                .map(::generateEmailLoop)
                .take(Random().nextInt(10, 15))
                .flatten()
                .shuffled()
                .toList()
        }

fun generateEmailLoop(users: List<User>): List<Email> =
    Email(
        subject = getRandomString(10),
        sender = users[0],
        receiver = users[1],
        timeSent = ZonedDateTime.of(
            2024, 1, 8, (users[0].officeHours.start..<users[0].officeHours.end).random(),
            (0..59).random(), (0..59).random(), 0, users[0].officeHours.timeZone
        )
    ).let { generateSequence(it, generateResponse(users)).take((5..10).random()).toList() }

fun generateResponse(users: List<User>): (Email) -> Email =
    { email ->
        Email(
            subject = "RE: ${email.subject}",
            sender = email.receiver,
            receiver = users.filter { user -> user.name != email.receiver.name }.random(),
            timeSent = generateResponseTime(email.timeSent, email.receiver)
        )
    }

fun generateResponseTime(timeReceived: ZonedDateTime, user: User): ZonedDateTime =
    timeReceived.withZoneSameInstant(user.officeHours.timeZone)
        .withHour((user.officeHours.start..<user.officeHours.end).random())
        .withMinute((0..59).random())
        .withSecond((0..59).random())
        .let { if (it.isBefore(timeReceived)) it.plusDays(1) else it }
        .let {
            // Add a 25% chance that the next working day is also skipped
            if ((0..3).random() == 0) it.plusDays(1) else it
        }.let {
            when (it.dayOfWeek) {
                DayOfWeek.SATURDAY -> it.plusDays(2)
                DayOfWeek.SUNDAY -> it.plusDays(1)
                else -> it
            }
        }.also {
            user.responseTimes += calculateResponseTime(timeReceived, it, user)
            user.responseTimesWithoutOfficeHours += ChronoUnit.SECONDS.between(it, timeReceived)
        }

fun calculateResponseTime(timeReceived: ZonedDateTime, timeResponded: ZonedDateTime, user: User): Long {
    if (timeReceived.withZoneSameInstant(user.officeHours.timeZone).dayOfYear == timeResponded.dayOfYear) {
        //Sent on the same day: need to check if it was received before office hours
        if (timeReceived.withZoneSameInstant(user.officeHours.timeZone).hour < user.officeHours.start) {
            // Received before office hours : return difference between response time and start of office hours
            val startOfOfficeHours = timeResponded.withHour(user.officeHours.start).withMinute(0).withSecond(0)
            return ChronoUnit.SECONDS.between(startOfOfficeHours, timeResponded)
        }
        return ChronoUnit.SECONDS.between(timeReceived, timeResponded)
    }
    // Received on a previous day - need to check if there was a full working day in between
    val emailNotRespondedToDuringAFullWorkingDay =
        ((timeReceived.dayOfWeek == DayOfWeek.FRIDAY && timeResponded.dayOfWeek == DayOfWeek.TUESDAY) ||
                (timeReceived.dayOfWeek == DayOfWeek.THURSDAY && timeResponded.dayOfWeek == DayOfWeek.MONDAY) ||
                (timeResponded.dayOfWeek.value - timeReceived.dayOfWeek.value > 1))

    val timeTakenDuringFullDay =
        if (emailNotRespondedToDuringAFullWorkingDay) (user.officeHours.end - user.officeHours.start) * 3600 else 0
    // Received on previous day - need to check if received before end of office hours
    if (timeReceived.withZoneSameInstant(user.officeHours.timeZone).hour < user.officeHours.end) {
        // Received during working hours : time for response is time Left in the working day + time taken during next working day
        val endOfOfficeDay = timeReceived.withHour(user.officeHours.end).withMinute(0).withSecond(0)
        val startOfOfficeHours = timeResponded.withHour(user.officeHours.start).withMinute(0).withSecond(0)
        return timeTakenDuringFullDay + ChronoUnit.SECONDS.between(
            timeReceived,
            endOfOfficeDay
        ) + ChronoUnit.SECONDS.between(startOfOfficeHours, timeResponded)
    }
    val startOfOfficeHours = timeResponded.withHour(user.officeHours.start).withMinute(0).withSecond(0)
    return timeTakenDuringFullDay + ChronoUnit.SECONDS.between(startOfOfficeHours, timeResponded)

}

data class User(
    val name: String,
    val officeHours: OfficeHours,
    val responseTimesWithoutOfficeHours: MutableList<Long> = mutableListOf(),
    val responseTimes: MutableList<Long> = mutableListOf()
)

data class OfficeHours(val timeZone: ZoneId, val start: Int, val end: Int)

data class Email(val subject: String, val sender: User, val receiver: User, val timeSent: ZonedDateTime)

fun getRandomString(length: Int): String = (1..length).fold("") { acc, _ -> "$acc${allowedChars.random()}" }

private val officeHours = listOf(
    OfficeHours(ZoneId.of("Europe/Paris"), 9, 18),
    OfficeHours(ZoneId.of("Australia/Sydney"), 10, 18),
    OfficeHours(ZoneId.of("Asia/Singapore"), 8, 17),
    OfficeHours(ZoneId.of("Hongkong"), 8, 17),
    OfficeHours(ZoneId.of("America/New_York"), 10, 18),
    OfficeHours(ZoneId.of("America/Los_Angeles"), 7, 16)
)

private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
