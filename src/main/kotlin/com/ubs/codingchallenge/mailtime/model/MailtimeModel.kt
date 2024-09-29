package com.ubs.codingchallenge.mailtime.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.ubs.codingchallenge.mailtime.config.objectMapper
import com.ubs.codingchallenge.mailtime.service.timeTakenToRespond
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
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
    EXTRA_SMALL(
        userCount = { 10 },
        usersPerThread = { 5 },
        threadCount = { (5..10).random() },
        responsesPerThread = { (5..10).random() }
    ),
    SMALL(
        userCount = { 10 },
        usersPerThread = { 10 },
        threadCount = { (10..20).random() },
        responsesPerThread = { (10..20).random() }
    ),
    DEFAULT(
        userCount = { 25 },
        usersPerThread = { 10 },
        threadCount = { (20..30).random() },
        responsesPerThread = { (10..20).random() }
    ),
    LARGE(
        userCount = { 50 },
        usersPerThread = { 20 },
        threadCount = { (25..50).random() },
        responsesPerThread = { (25..50).random() }
    ),
    EXTRA_LARGE(
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
        (request as Input to response as Output).let { (input, output) ->
            val responseTimesWithoutOfficeHours = MailtimeSolver(partOne)(input).response
            val responseTimes = MailtimeSolver(partTwo)(input).response
            val (scores, messages) = input.users.map(User::name).map {
                val expectedPartTwo = responseTimes.getValue(it)
                val expectedPartOne = responseTimesWithoutOfficeHours.getValue(it)
                when (output.response[it]) {
                    expectedPartTwo -> 4L to null
                    expectedPartOne -> 1L to null
                    else -> 0L to it
                }
            }.unzip()
            ChallengeResult(
                score = 5 * scores.averageOrZero.toInt(),
                message = messages.mapNotNull { it }.joinToString(",")
            )
        }

    fun calculateScore(input: Input, output: Output): Int =
        input.users.map { user ->
            when (output.response[user.name]) {
                user.responseTimes.averageOrZero -> 4L
                user.responseTimesWithoutOfficeHours.averageOrZero -> 1L
                else -> 0L
            }
        }.averageOrZero.toInt()
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
            senderUser = users[0],
            receiverUser = users[1],
            timeSent = LocalDate.of(2024, 5, 1)
                .atStartOfDay(users[0].officeHours.timeZone)
                .let(users[0].officeHours::randomTimeSince)
        ),
        nextFunction = generateResponseWith(randomReceiverFrom(users))
    )

private fun randomReceiverFrom(users: List<User>): (Email) -> User =
    { email -> users.filter { user -> user.name != email.receiverUser.name }.random() }

private fun generateResponseWith(getReceiver: (Email) -> User): (Email) -> Email =
    { email ->
        Email(
            subject = "RE: ${email.subject}",
            senderUser = email.receiverUser,
            receiverUser = getReceiver(email),
            timeSent = generateResponseTime(email.timeSent, email.receiverUser)
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
            .withHour((start until end).random())
            .withMinute((0..59).random())
            .withSecond((0..59).random())

    override fun toString(): String = "$start..$end@$timeZone"
}

data class Email(
    val subject: String,
    @get:JsonIgnore val senderUser: User,
    @get:JsonIgnore val receiverUser: User,
    val timeSent: ZonedDateTime
) {
    val sender: String get() = senderUser.name
    val receiver: String get() = receiverUser.name
}

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

private val List<Long>.averageOrZero: Long get() = takeIf { it.isNotEmpty() }?.average()?.roundToLong() ?: 0L

class MailtimeSolver(
    private val calculator: (User, ZonedDateTime, ZonedDateTime) -> Sequence<Segment>
) : (Input) -> Output {
    override fun invoke(input: Input): Output = input.run {
        users.associate { it.name to UserTime(duration = Duration.ZERO, count = 0) }.toMutableMap() to
                users.associateBy(User::name)
    }.let { (results, userByName) ->
        input.emails.groupBy { email -> email.subject.replace("RE: ", "") }.values.forEach { emails ->
            emails.sortedBy { it.timeSent }.reduce { previous, current ->
                val user = userByName.getValue(current.sender)
                log { "$current\n\tCALCULATING FOR [${user.name}, ${user.officeHours}]: ${previous.timeSent} .. ${current.timeSent}" }
                val logger = { it: UserTime ->
                    log { "\tRESULT FOR [${current.sender}]: ${results[current.sender] ?: 0} + ${it.duration.seconds}s" }
                }
                results.merge(
                    current.sender,
                    calculator(user, previous.timeSent, current.timeSent)
                        .mapNotNull { segment -> segment.toDuration?.also { log { "\t\t$segment" } } }
                        .reduceOrNull { a, b -> a + b }
                        .let { UserTime(it ?: Duration.ZERO) }
                        .also(logger),
                    UserTime::plus
                )
                current
            }
        }
        results.mapValues { (_, value) -> value() }
    }.let(::Output)

    private fun log(message: () -> String): Unit = Unit // println(message())

    private data class UserTime(val duration: Duration, val count: Int = 1) : () -> Long {
        operator fun plus(other: UserTime) =
            UserTime(duration = this.duration + other.duration, count = this.count + other.count)

        override fun invoke(): Long = duration.seconds.toDouble().div(count.coerceAtLeast(1)).roundToLong()
    }
}

val partOne: (User, ZonedDateTime, ZonedDateTime) -> Sequence<Segment> = { user, previous, current ->
    sequenceOf(Segment(officeHours = user.officeHours, from = previous, to = current))
}

val partTwo: (User, ZonedDateTime, ZonedDateTime) -> Sequence<Segment> = { user, previous, current ->
    generateSequence(Segment(officeHours = user.officeHours, from = null, to = previous)) {
        it.until(cutOff = current)
    }.takeWhile { it.from == null || it.from < current }
}

data class Segment(val officeHours: OfficeHours, val from: ZonedDateTime?, val to: ZonedDateTime) {
    val toDuration: Duration? = from?.let { Duration.between(it, to) }

    fun until(cutOff: ZonedDateTime): Segment =
        copy(from = to.takeIf { it in officeHours }, to = to.with(officeHours.asTemporalAdjuster).coerceAtMost(cutOff))

    private operator fun OfficeHours.contains(zonedDateTime: ZonedDateTime) = zonedDateTime.hour in start until end

    private val OfficeHours.asTemporalAdjuster: TemporalAdjuster
        get() = TemporalAdjuster { temporal ->
            ZonedDateTime.from(temporal).withZoneSameInstant(timeZone).let {
                when (it.dayOfWeek) {
                    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> it.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                        .withHour(start)

                    else -> when {
                        it.hour < start -> it.withHour(start)
                        it.hour >= end -> it.with(TemporalAdjusters.next(it.dayOfWeek.nextWeekday)).withHour(start)
                        else -> it.withHour(end)
                    }
                }
            }.withMinute(0).withSecond(0).withNano(0)
        }

    private val DayOfWeek.nextWeekday: DayOfWeek get() = if (this >= DayOfWeek.FRIDAY) DayOfWeek.MONDAY else this.plus(1)

    override fun toString(): String = "${from?.let(format) ?: "".padEnd(24, ' ')} .. ${format(to)}"

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ")

        private val format: (ZonedDateTime) -> String = { it.toOffsetDateTime().format(formatter) }
    }
}
