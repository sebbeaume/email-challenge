package com.ubs.codingchallenge.mailtime.controller

import com.ubs.codingchallenge.mailtime.model.OfficeHours
import com.ubs.codingchallenge.mailtime.model.Output
import com.ubs.codingchallenge.mailtime.model.User
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters.next
import kotlin.math.roundToLong

@RestController
class SolverController {

    @PostMapping(value = ["/mailtime"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluate(@RequestBody input: SolverInput): ResponseEntity<Output> =
        input.let(MailtimeSolver(partTwo)).let { ResponseEntity.ok(it) }
}

data class SolverInput(val emails: List<SolverEmail>, val users: List<User>)

data class SolverEmail(val subject: String, val sender: String, val receiver: String, val timeSent: OffsetDateTime)

private class MailtimeSolver(
    private val calculator: (User, OffsetDateTime, OffsetDateTime) -> Sequence<Segment>
) : (SolverInput) -> Output {
    override fun invoke(input: SolverInput): Output = input.run {
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

private fun zoned(user: User, offsetDateTime: OffsetDateTime): ZonedDateTime =
    offsetDateTime.atZoneSameInstant(user.officeHours.timeZone)

private val partOne: (User, OffsetDateTime, OffsetDateTime) -> Sequence<Segment> = { user, previous, current ->
    sequenceOf(Segment(officeHours = user.officeHours, from = zoned(user, previous), to = zoned(user, current)))
}

private val partTwo: (User, OffsetDateTime, OffsetDateTime) -> Sequence<Segment> = { user, previous, current ->
    val (zonedPrevious, zonedCurrent) = zoned(user, previous) to zoned(user, current)
    generateSequence(Segment(officeHours = user.officeHours, from = null, to = zonedPrevious)) {
        it.until(cutOff = zonedCurrent)
    }.takeWhile { it.from == null || it.from < zonedCurrent }
}

private data class Segment(val officeHours: OfficeHours, val from: ZonedDateTime?, val to: ZonedDateTime) {
    val toDuration: Duration? = from?.let { Duration.between(it, to) }

    fun until(cutOff: ZonedDateTime): Segment =
        copy(from = to.takeIf { it in officeHours }, to = to.with(officeHours.asTemporalAdjuster).coerceAtMost(cutOff))

    private operator fun OfficeHours.contains(zonedDateTime: ZonedDateTime) = zonedDateTime.hour in start until end

    private val OfficeHours.asTemporalAdjuster: TemporalAdjuster
        get() = TemporalAdjuster { temporal ->
            ZonedDateTime.from(temporal).let {
                when (it.dayOfWeek) {
                    SATURDAY, SUNDAY -> it.with(next(MONDAY)).withHour(start)
                    else -> when {
                        it.hour < start -> it.withHour(start)
                        it.hour >= end -> it.with(next(it.dayOfWeek.nextWeekday)).withHour(start)
                        else -> it.withHour(end)
                    }
                }
            }.withMinute(0).withSecond(0).withNano(0)
        }

    private val DayOfWeek.nextWeekday: DayOfWeek get() = if (this >= FRIDAY) MONDAY else this.plus(1)

    override fun toString(): String = "${from?.let(format) ?: "".padEnd(24, ' ')} .. ${format(to)}"

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ")

        private val format: (ZonedDateTime) -> String = { it.toOffsetDateTime().format(formatter) }
    }
}
