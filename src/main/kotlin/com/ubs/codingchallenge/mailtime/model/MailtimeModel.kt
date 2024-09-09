package com.ubs.codingchallenge.mailtime.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.ubs.codingchallenge.mailtime.config.objectMapper
import com.ubs.codingchallenge.mailtime.service.timeTakenToRespond
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

object MailtimeChallenge : LevelBasedChallenge {
    override fun createFor(teamUrl: String): ChallengeRequest = createFor(teamUrl, DifficultyLevel.values().first())

    override fun createFor(teamUrl: String, level: ChallengeLevel): ChallengeRequest =
        generateInput(level as DifficultyLevel)
}

enum class DifficultyLevel(
    val userCount: () -> Int,
    val usersPerThread: () -> Int,
    val threadCount: () -> Int,
    val responsesPerThread: () -> Int
) : ChallengeLevel {
    EXAMPLE(
        userCount = { 2 },
        usersPerThread = { 2 },
        threadCount = { 1 },
        responsesPerThread = { 2 }
    ),
    DEFAULT(
        userCount = { 25 },
        usersPerThread = { 10 },
        threadCount = { (20..30).random() },
        responsesPerThread = { (10..20).random() }
    ),
    LARGE(
        userCount = { 50 },
        usersPerThread = { 25 },
        threadCount = { (100..150).random() },
        responsesPerThread = { (50..100).random() }
    );

    override val difficulty: Int = ordinal
}

object MailtimeChecker : Checker {
    override fun convert(rawResponse: String): ChallengeResponse =
        objectMapper.readValue(rawResponse, Output::class.java)

    override fun check(request: ChallengeRequest, response: ChallengeResponse): ChallengeResult =
        ChallengeResult(
            score = calculateScore(request as Input, response as Output),
            message = "MAILTIME!"
        )

    fun calculateScore(input: Input, output: Output): Int =
        input.users.sumOf { user ->
            when (output.response[user.name]) {
                user.responseTimes.average().roundToLong() -> 4L
                user.responseTimesWithoutOfficeHours.average().roundToLong() -> 1L
                else -> 0L
            }
        }.toInt()
}

fun generateInput(difficultyLevel: DifficultyLevel): Input {
    val userList: List<User> = generateSequence { User(getRandomString(5), officeHours.random()) }
        .take(difficultyLevel.userCount())
        .toList()

    val emails: List<Email> =
        userList.let { users -> generateSequence { users.shuffled().take(difficultyLevel.usersPerThread()) } }
            .map(::generateEmailThread)
            .take(difficultyLevel.threadCount())
            .flatMap { it.take(difficultyLevel.responsesPerThread()) }
            .shuffled()
            .toList()

    return Input(emails, userList)
}


private fun generateEmailThread(users: List<User>): Sequence<Email> =
    generateSequence(
        seed = Email(
            subject = getRandomString(10),
            sender = users[0],
            receiver = users[1],
            timeSent = LocalDate.of(2024, 1, 8)
                .atStartOfDay(users[0].officeHours.timeZone)
                .let(users[0].officeHours::randomTimeSince)
        ),
        nextFunction = generateResponseWith(randomReceiverFrom(users))
    )

private fun randomReceiverFrom(users: List<User>): (Email) -> User =
    { email -> users.filter { user -> user.name != email.receiver.name }.random() }

private fun generateResponseWith(getReceiver: (Email) -> User): (Email) -> Email =
    { email ->
        Email(
            subject = "RE: ${email.subject}",
            sender = email.receiver,
            receiver = getReceiver(email),
            timeSent = generateResponseTime(email.timeSent, email.receiver)
        )
    }

private fun generateResponseTime(timeReceived: ZonedDateTime, user: User): ZonedDateTime =
    user.officeHours.randomTimeSince(timeReceived)
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
            user.responseTimes += timeTakenToRespond(timeReceived, it, user);
            user.responseTimesWithoutOfficeHours += calculateResponseRimeWithoutOfficeHours(timeReceived, it)
        }

fun calculateResponseRimeWithoutOfficeHours(
    timeReceived: ZonedDateTime,
    timeResponded: ZonedDateTime,
): Long {
    return ChronoUnit.SECONDS.between(timeReceived, timeResponded)
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
        val endOfOfficeDay =
            timeReceived.withZoneSameInstant(user.officeHours.timeZone).withHour(user.officeHours.end).withMinute(0)
                .withSecond(0)
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
    @get:JsonIgnore val responseTimesWithoutOfficeHours: MutableList<Long> = mutableListOf(),
    @get:JsonIgnore val responseTimes: MutableList<Long> = mutableListOf()
) {
    override fun toString(): String =
        "($name [$officeHours] responseTimesWithoutOfficeHours=$responseTimesWithoutOfficeHours, responseTimes=$responseTimes)"
}

data class OfficeHours(val timeZone: ZoneId, val start: Int, val end: Int) {
    fun randomTimeSince(from: ZonedDateTime): ZonedDateTime =
        from.withZoneSameInstant(timeZone)
            .withHour((start..<end).random())
            .withMinute((0..59).random())
            .withSecond((0..59).random())

    override fun toString(): String = "$start..$end@$timeZone"
}

data class Email(val subject: String, val sender: User, val receiver: User, val timeSent: ZonedDateTime)

data class Input(val emails: List<Email>, val users: List<User>) : ChallengeRequest

data class Output(val response: Map<String, Long>) : ChallengeResponse

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
